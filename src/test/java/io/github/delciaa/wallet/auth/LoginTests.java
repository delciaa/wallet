package io.github.delciaa.wallet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.jayway.jsonpath.JsonPath;
import io.github.delciaa.wallet.TestcontainersConfiguration;
import io.github.delciaa.wallet.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LoginTests {

	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void loginWithCorrectPasswordReturnsBearerToken() throws Exception {
		String email = registeredUser();

		login(email, PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900));
	}

	@Test
	void loginIgnoresEmailCase() throws Exception {
		String email = registeredUser();

		login(email.toUpperCase(), PASSWORD).andExpect(status().isOk());
	}

	@Test
	void wrongPasswordAndUnknownEmailGiveTheSameError() throws Exception {
		String email = registeredUser();

		String wrongPassword = login(email, "wrong-password").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid email or password"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String unknownEmail = login(uniqueEmail(), PASSWORD).andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(unknownEmail).isEqualTo(wrongPassword);
	}

	@Test
	void blankFieldsAreRejected() throws Exception {
		login("", "").andExpect(status().isBadRequest());
	}

	@Test
	void meReturnsTheTokensOwnUserAndWallet() throws Exception {
		String email = registeredUser();
		String token = tokenFor(email);

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.role").value("USER"))
			.andExpect(jsonPath("$.walletId").isNotEmpty())
			.andExpect(jsonPath("$.balance").value(0));
	}

	@Test
	void meWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void meWithGarbageTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredTokenIsRejected() throws Exception {
		UUID userId = users.findByEmail(registeredUser()).orElseThrow().getId();
		Instant past = Instant.now().minusSeconds(3600);
		String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
				claims(userId).issuedAt(past.minusSeconds(900)).expiresAt(past).build())).getTokenValue();

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenSignedWithAnotherKeyIsRejected() throws Exception {
		UUID userId = users.findByEmail(registeredUser()).orElseThrow().getId();
		byte[] attackerKey = "an-attacker-key-that-is-long-enough-for-hs256".getBytes(StandardCharsets.UTF_8);
		JwtEncoder attacker = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(attackerKey, "HmacSHA256")).build();
		Instant now = Instant.now();
		String forged = attacker.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
				claims(userId).issuedAt(now).expiresAt(now.plusSeconds(900)).build())).getTokenValue();

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void userTokenCannotReachAdminPaths() throws Exception {
		String token = tokenFor(registeredUser());

		mockMvc.perform(get("/api/admin/anything").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());
	}

	@Test
	void loginStillWorksWhenAStaleTokenIsSentAlong() throws Exception {
		String email = registeredUser();

		mockMvc
			.perform(post("/api/auth/login").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredTokenFor(email))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(email, PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void registerStillWorksWhenAGarbageTokenIsSentAlong() throws Exception {
		mockMvc
			.perform(post("/api/auth/register").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(uniqueEmail(), PASSWORD)))
			.andExpect(status().isCreated());
	}

	@Test
	void staleTokenIsStillRejectedOutsideTheAuthPaths() throws Exception {
		String email = registeredUser();

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredTokenFor(email)))
			.andExpect(status().isUnauthorized());
	}

	private String expiredTokenFor(String email) {
		UUID userId = users.findByEmail(email).orElseThrow().getId();
		Instant past = Instant.now().minusSeconds(3600);
		return jwtEncoder
			.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
					claims(userId).issuedAt(past.minusSeconds(900)).expiresAt(past).build()))
			.getTokenValue();
	}

	private JwtClaimsSet.Builder claims(UUID userId) {
		return JwtClaimsSet.builder().issuer("wallet").subject(userId.toString()).claim("role", "USER");
	}

	private String registeredUser() throws Exception {
		String email = uniqueEmail();
		mockMvc
			.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(json(email, PASSWORD)))
			.andExpect(status().isCreated());
		return email;
	}

	private String tokenFor(String email) throws Exception {
		String body = login(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(email, password)));
	}

	private static String json(String email, String password) {
		return """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password);
	}

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

}
