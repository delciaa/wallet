package io.github.delciaa.wallet.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.jayway.jsonpath.JsonPath;
import io.github.delciaa.wallet.TestcontainersConfiguration;
import io.github.delciaa.wallet.auth.AuthService;
import io.github.delciaa.wallet.auth.LoginRequest;
import io.github.delciaa.wallet.auth.RegisterRequest;
import io.github.delciaa.wallet.common.ConcurrentUpdateException;
import io.github.delciaa.wallet.support.Concurrently;
import io.github.delciaa.wallet.wallet.InsufficientFundsException;
import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// Deliberately not @Transactional: the concurrency tests need real, separately committed transactions
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TransferTests {

	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private TopUpService topUpService;

	@Autowired
	private TransferService transferService;

	@Autowired
	private WalletRepository wallets;

	@Autowired
	private TransferRepository transfers;

	@Autowired
	private LedgerEntryRepository ledger;

	private TestUser ana;

	private TestUser maria;

	private TestUser joao;

	@BeforeEach
	void createUsers() {
		ana = newUser("ana");
		maria = newUser("maria");
		joao = newUser("joao");
	}

	@Test
	void sendMovesMoneyFromSenderToRecipient() throws Exception {
		fund(ana, "100.00");

		send(ana, "key-1", maria.email(), "25.00").andExpect(status().isCreated())
			.andExpect(header().string(IdempotencyHeaders.REPLAYED, "false"))
			.andExpect(jsonPath("$.type").value("P2P"))
			.andExpect(jsonPath("$.amount").value(25.00));

		assertBalances("75.00", "25.00");
		assertLedgerMatchesBalances(ana, maria);
	}

	@Test
	void recipientEmailIgnoresCase() throws Exception {
		fund(ana, "100.00");

		send(ana, "key-1", maria.email().toUpperCase(), "25.00").andExpect(status().isCreated());

		assertBalances("75.00", "25.00");
	}

	@Test
	void doubleTapSendsMoneyOnce() throws Exception {
		fund(ana, "100.00");
		String first = send(ana, "same-key", maria.email(), "25.00").andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		String second = send(ana, "same-key", maria.email(), "25.00").andExpect(status().isOk())
			.andExpect(header().string(IdempotencyHeaders.REPLAYED, "true"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(JsonPath.<String>read(second, "$.transferId")).isEqualTo(JsonPath.read(first, "$.transferId"));
		assertBalances("75.00", "25.00");
	}

	@Test
	void sameKeyForADifferentRecipientIsRejected() throws Exception {
		fund(ana, "100.00");
		send(ana, "key-1", maria.email(), "25.00").andExpect(status().isCreated());

		send(ana, "key-1", joao.email(), "25.00").andExpect(status().isUnprocessableContent());

		assertThat(balanceOf(joao)).isEqualByComparingTo("0");
	}

	@Test
	void sendingMoreThanTheBalanceIsRejected() throws Exception {
		fund(ana, "30.00");

		send(ana, "key-1", maria.email(), "30.01").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Insufficient funds"));

		assertBalances("30.00", "0");
	}

	@Test
	void sendingTheWholeBalanceIsAllowed() throws Exception {
		fund(ana, "30.00");

		send(ana, "key-1", maria.email(), "30.00").andExpect(status().isCreated());

		assertBalances("0", "30.00");
	}

	@Test
	void unknownRecipientIsRejected() throws Exception {
		fund(ana, "100.00");

		send(ana, "key-1", "nobody-" + UUID.randomUUID() + "@example.com", "25.00").andExpect(status().isNotFound());

		assertThat(balanceOf(ana)).isEqualByComparingTo("100.00");
	}

	@Test
	void sendingToYourselfIsRejected() throws Exception {
		fund(ana, "100.00");

		send(ana, "key-1", ana.email(), "25.00").andExpect(status().isUnprocessableContent());

		assertThat(balanceOf(ana)).isEqualByComparingTo("100.00");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-50", "0.001", "10000.01", "null" })
	void invalidAmountsAreRejected(String amount) throws Exception {
		fund(ana, "100.00");

		send(ana, "key-1", maria.email(), amount).andExpect(status().isBadRequest());

		assertBalances("100.00", "0");
	}

	@Test
	void sendWithoutTokenIsUnauthorized() throws Exception {
		mockMvc
			.perform(post("/api/transfers").header(IdempotencyHeaders.KEY, "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(maria.email(), "25.00")))
			.andExpect(status().isUnauthorized());
	}

	/** The race from the project brief: €30, and two €25 sends at the same moment. */
	@Test
	void twoSimultaneousSendsCannotOverspend() throws Exception {
		fund(ana, "30.00");

		List<Outcome> outcomes = Concurrently
			.run(List.of(sendTask(ana, "to-maria", maria, "25.00"), sendTask(ana, "to-joao", joao, "25.00")));

		assertThat(outcomes).containsExactlyInAnyOrder(Outcome.SENT, Outcome.INSUFFICIENT_FUNDS);
		assertThat(balanceOf(ana)).isEqualByComparingTo("5.00");
		assertThat(balanceOf(maria).add(balanceOf(joao))).isEqualByComparingTo("25.00");
		assertLedgerMatchesBalances(ana, maria, joao);
	}

	@Test
	void manySimultaneousSendsNeverOverspendOrLoseMoney() throws Exception {
		fund(ana, "100.00");
		List<Callable<Outcome>> tasks = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			tasks.add(sendTask(ana, "burst-" + i, maria, "25.00"));
		}

		long sent = Concurrently.run(tasks).stream().filter(Outcome.SENT::equals).count();

		// €100 covers at most four €25 sends, however the race goes
		assertThat(sent).isBetween(1L, 4L);
		BigDecimal moved = new BigDecimal("25.00").multiply(BigDecimal.valueOf(sent));
		assertThat(balanceOf(ana)).isEqualByComparingTo(new BigDecimal("100.00").subtract(moved));
		assertThat(balanceOf(maria)).isEqualByComparingTo(moved);
		assertLedgerMatchesBalances(ana, maria);
	}

	@Test
	void sendsInOppositeDirectionsAtOnceDoNotDeadlockOrLoseMoney() throws Exception {
		fund(ana, "100.00");
		fund(maria, "100.00");
		List<Callable<Outcome>> tasks = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			tasks.add(sendTask(ana, "a-to-m-" + i, maria, "5.00"));
			tasks.add(sendTask(maria, "m-to-a-" + i, ana, "5.00"));
		}

		List<Outcome> outcomes = Concurrently.run(tasks);

		assertThat(outcomes).contains(Outcome.SENT).doesNotContain(Outcome.INSUFFICIENT_FUNDS);
		assertThat(balanceOf(ana).add(balanceOf(maria))).isEqualByComparingTo("200.00");
		assertLedgerMatchesBalances(ana, maria);
	}

	@Test
	void simultaneousDoubleTapsSendExactlyOnce() throws Exception {
		fund(ana, "100.00");
		List<Callable<UUID>> tasks = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			tasks.add(() -> transferService
				.send(ana.id(), "tapped-many-times", new TransferRequest(maria.email(), new BigDecimal("25.00")))
				.transfer()
				.transferId());
		}

		List<UUID> transferIds = Concurrently.run(tasks);

		assertThat(transferIds).containsOnly(transferIds.getFirst());
		assertBalances("75.00", "25.00");
		assertThat(transfers.countByInitiatedBy(ana.id())).isEqualTo(2); // the top-up and one send
	}

	private enum Outcome {

		SENT, INSUFFICIENT_FUNDS, BUSY

	}

	private Callable<Outcome> sendTask(TestUser from, String key, TestUser to, String amount) {
		return () -> {
			try {
				transferService.send(from.id(), key, new TransferRequest(to.email(), new BigDecimal(amount)));
				return Outcome.SENT;
			}
			catch (InsufficientFundsException ex) {
				return Outcome.INSUFFICIENT_FUNDS;
			}
			catch (ConcurrentUpdateException ex) {
				// Allowed: it changed nothing, and the app told the caller to retry
				return Outcome.BUSY;
			}
		};
	}

	private record TestUser(UUID id, String email, String token) {
	}

	private TestUser newUser(String name) {
		String email = name + "-" + UUID.randomUUID() + "@example.com";
		UUID id = authService.register(new RegisterRequest(email, PASSWORD)).userId();
		String token = authService.login(new LoginRequest(email, PASSWORD)).accessToken();
		return new TestUser(id, email, token);
	}

	private void fund(TestUser user, String amount) {
		topUpService.topUp(user.id(), "fund-" + UUID.randomUUID(), new TopUpRequest(new BigDecimal(amount)));
	}

	private ResultActions send(TestUser from, String key, String recipientEmail, String amount) throws Exception {
		return mockMvc.perform(post("/api/transfers").header(HttpHeaders.AUTHORIZATION, "Bearer " + from.token())
			.header(IdempotencyHeaders.KEY, key)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(recipientEmail, amount)));
	}

	private static String body(String recipientEmail, String amount) {
		return """
				{"recipientEmail": "%s", "amount": %s}
				""".formatted(recipientEmail, amount);
	}

	private BigDecimal balanceOf(TestUser user) {
		return wallet(user).getBalance();
	}

	private Wallet wallet(TestUser user) {
		return wallets.findByUserId(user.id()).orElseThrow();
	}

	private void assertBalances(String anaBalance, String mariaBalance) {
		assertThat(balanceOf(ana)).isEqualByComparingTo(anaBalance);
		assertThat(balanceOf(maria)).isEqualByComparingTo(mariaBalance);
	}

	private void assertLedgerMatchesBalances(TestUser... users) {
		for (TestUser user : users) {
			Wallet wallet = wallet(user);
			assertThat(ledger.sumForWallet(wallet.getId())).isEqualByComparingTo(wallet.getBalance());
		}
		assertThat(ledger.sumOfAllEntries()).isEqualByComparingTo("0");
	}

}
