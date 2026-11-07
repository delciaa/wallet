package io.github.delciaa.wallet.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import io.github.delciaa.wallet.TestcontainersConfiguration;
import io.github.delciaa.wallet.auth.AuthService;
import io.github.delciaa.wallet.auth.LoginRequest;
import io.github.delciaa.wallet.auth.RegisterRequest;
import io.github.delciaa.wallet.transfer.TopUpRequest;
import io.github.delciaa.wallet.transfer.TopUpService;
import io.github.delciaa.wallet.transfer.TransferRequest;
import io.github.delciaa.wallet.transfer.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class HistoryTests {

	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private TopUpService topUpService;

	@Autowired
	private TransferService transferService;

	private TestUser ana;

	private TestUser maria;

	@BeforeEach
	void createUsers() {
		ana = newUser("ana");
		maria = newUser("maria");
	}

	@Test
	void historyShowsTheUserStoryNewestFirst() throws Exception {
		topUp(ana, "100.00");
		send(ana, maria, "25.00");

		history(ana).andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(2)))
			.andExpect(jsonPath("$.items[0].type").value("P2P"))
			.andExpect(jsonPath("$.items[0].direction").value("OUT"))
			.andExpect(jsonPath("$.items[0].amount").value(-25.00))
			.andExpect(jsonPath("$.items[0].counterparty").value(maria.email()))
			.andExpect(jsonPath("$.items[1].type").value("TOP_UP"))
			.andExpect(jsonPath("$.items[1].direction").value("IN"))
			.andExpect(jsonPath("$.items[1].amount").value(100.00))
			.andExpect(jsonPath("$.items[1].counterparty").value(nullValue()))
			.andExpect(jsonPath("$.nextCursor").value(nullValue()));
	}

	@Test
	void recipientSeesTheSameTransferAsMoneyIn() throws Exception {
		topUp(ana, "100.00");
		send(ana, maria, "25.00");

		history(maria).andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].direction").value("IN"))
			.andExpect(jsonPath("$.items[0].amount").value(25.00))
			.andExpect(jsonPath("$.items[0].counterparty").value(ana.email()));
	}

	@Test
	void usersOnlySeeTheirOwnHistory() throws Exception {
		topUp(ana, "100.00");

		history(maria).andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void newUserHasEmptyHistory() throws Exception {
		history(ana).andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(0)))
			.andExpect(jsonPath("$.nextCursor").value(nullValue()));
	}

	@Test
	void doubleTapAppearsOnceInHistory() {
		String key = "same-" + UUID.randomUUID();
		for (int i = 0; i < 3; i++) {
			topUpService.topUp(ana.id(), key, new TopUpRequest(new BigDecimal("10.00")));
		}

		assertThat(allAmounts(ana, 10)).containsExactly("10.0");
	}

	@Test
	void pagesWalkThroughEverythingOnceInOrder() throws Exception {
		for (int i = 1; i <= 5; i++) {
			topUp(ana, i + ".00");
		}

		String first = body(history(ana, 2, null));
		String second = body(history(ana, 2, JsonPath.read(first, "$.nextCursor")));
		String third = body(history(ana, 2, JsonPath.read(second, "$.nextCursor")));

		assertThat(amounts(first)).containsExactly("5.0", "4.0");
		assertThat(amounts(second)).containsExactly("3.0", "2.0");
		assertThat(amounts(third)).containsExactly("1.0");
		assertThat((Object) JsonPath.read(third, "$.nextCursor")).isNull();
	}

	@Test
	void newTransactionsDoNotShiftLaterPages() throws Exception {
		for (int i = 1; i <= 4; i++) {
			topUp(ana, i + ".00");
		}
		String first = body(history(ana, 2, null));

		// Arrives between page one and page two; with page numbers this would repeat "2.0"
		topUp(ana, "99.00");
		String second = body(history(ana, 2, JsonPath.read(first, "$.nextCursor")));

		assertThat(amounts(first)).containsExactly("4.0", "3.0");
		assertThat(amounts(second)).containsExactly("2.0", "1.0");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "101", "-1", "abc" })
	void invalidLimitIsRejected(String limit) throws Exception {
		mockMvc.perform(authed(ana, get("/api/wallets/me/transactions").param("limit", limit)))
			.andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { "not-a-cursor", "bm8tc2VwYXJhdG9y", "MjAyNnxub3QtYS11dWlk" })
	void invalidCursorIsRejected(String cursor) throws Exception {
		mockMvc.perform(authed(ana, get("/api/wallets/me/transactions").param("cursor", cursor)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Invalid cursor"));
	}

	@Test
	void historyWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/wallets/me/transactions")).andExpect(status().isUnauthorized());
	}

	private record TestUser(UUID id, String email, String token) {
	}

	private TestUser newUser(String name) {
		String email = name + "-" + UUID.randomUUID() + "@example.com";
		UUID id = authService.register(new RegisterRequest(email, PASSWORD)).userId();
		String token = authService.login(new LoginRequest(email, PASSWORD)).accessToken();
		return new TestUser(id, email, token);
	}

	private void topUp(TestUser user, String amount) {
		topUpService.topUp(user.id(), "top-up-" + UUID.randomUUID(), new TopUpRequest(new BigDecimal(amount)));
	}

	private void send(TestUser from, TestUser to, String amount) {
		transferService.send(from.id(), "send-" + UUID.randomUUID(),
				new TransferRequest(to.email(), new BigDecimal(amount)));
	}

	private ResultActions history(TestUser user) throws Exception {
		return mockMvc.perform(authed(user, get("/api/wallets/me/transactions")));
	}

	private ResultActions history(TestUser user, int limit, String cursor) throws Exception {
		MockHttpServletRequestBuilder request = get("/api/wallets/me/transactions").param("limit",
				String.valueOf(limit));
		if (cursor != null) {
			request.param("cursor", cursor);
		}
		return mockMvc.perform(authed(user, request)).andExpect(status().isOk());
	}

	private List<String> allAmounts(TestUser user, int limit) {
		try {
			return amounts(body(history(user, limit, null)));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static MockHttpServletRequestBuilder authed(TestUser user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token());
	}

	private static String body(ResultActions result) throws Exception {
		return result.andReturn().getResponse().getContentAsString();
	}

	private static List<String> amounts(String json) {
		List<Number> raw = JsonPath.read(json, "$.items[*].amount");
		List<String> amounts = new ArrayList<>();
		raw.forEach(n -> amounts.add(String.valueOf(n.doubleValue())));
		return amounts;
	}

}
