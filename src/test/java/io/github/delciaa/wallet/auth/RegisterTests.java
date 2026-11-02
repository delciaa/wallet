package io.github.delciaa.wallet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import io.github.delciaa.wallet.TestcontainersConfiguration;
import io.github.delciaa.wallet.user.User;
import io.github.delciaa.wallet.user.UserRepository;
import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RegisterTests {

	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private WalletRepository wallets;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void registerCreatesUserWithEmptyWallet() throws Exception {
		String email = uniqueEmail();

		register(email, PASSWORD).andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.balance").value(0))
			.andExpect(jsonPath("$.currency").value("EUR"))
			.andExpect(jsonPath("$.walletId").isNotEmpty());

		User user = users.findByEmail(email).orElseThrow();
		Wallet wallet = wallets.findByUserId(user.getId()).orElseThrow();
		assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void passwordIsStoredOnlyAsHash() throws Exception {
		String email = uniqueEmail();

		register(email, PASSWORD).andExpect(status().isCreated())
			.andExpect(content().string(not(containsString("password"))));

		User user = users.findByEmail(email).orElseThrow();
		assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
	}

	@Test
	void duplicateEmailIsRejectedIgnoringCase() throws Exception {
		String email = uniqueEmail();
		register(email, PASSWORD).andExpect(status().isCreated());
		long usersBefore = users.count();
		long walletsBefore = wallets.count();

		register(email.toUpperCase(), PASSWORD).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Email is already registered"));

		assertThat(users.count()).isEqualTo(usersBefore);
		assertThat(wallets.count()).isEqualTo(walletsBefore);
	}

	@Test
	void invalidEmailIsRejected() throws Exception {
		register("not-an-email", PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void shortPasswordIsRejected() throws Exception {
		register(uniqueEmail(), "short").andExpect(status().isBadRequest());
	}

	@Test
	void passwordOverBcryptByteLimitIsRejected() throws Exception {
		// 40 characters, but 80 bytes in UTF-8
		register(uniqueEmail(), "é".repeat(40)).andExpect(status().isBadRequest());
	}

	private ResultActions register(String email, String password) throws Exception {
		String body = """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password);
		return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

}
