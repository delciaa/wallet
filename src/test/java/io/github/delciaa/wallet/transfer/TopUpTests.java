package io.github.delciaa.wallet.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jayway.jsonpath.JsonPath;
import io.github.delciaa.wallet.TestcontainersConfiguration;
import io.github.delciaa.wallet.auth.AuthService;
import io.github.delciaa.wallet.auth.LoginRequest;
import io.github.delciaa.wallet.auth.RegisterRequest;
import io.github.delciaa.wallet.common.ConcurrentUpdateException;
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
class TopUpTests {

	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private TopUpService topUpService;

	@Autowired
	private WalletRepository wallets;

	@Autowired
	private TransferRepository transfers;

	@Autowired
	private LedgerEntryRepository ledger;

	private UUID userId;

	private String token;

	@BeforeEach
	void registerAndLogIn() {
		String email = "user-" + UUID.randomUUID() + "@example.com";
		userId = authService.register(new RegisterRequest(email, PASSWORD)).userId();
		token = authService.login(new LoginRequest(email, PASSWORD)).accessToken();
	}

	@Test
	void topUpAddsMoneyAndWritesBalancedLedgerLines() throws Exception {
		topUp("key-1", "100.00").andExpect(status().isCreated())
			.andExpect(header().string(TopUpController.REPLAYED, "false"))
			.andExpect(jsonPath("$.type").value("TOP_UP"))
			.andExpect(jsonPath("$.amount").value(100.00))
			.andExpect(jsonPath("$.currency").value("EUR"));

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(jsonPath("$.balance").value(100.00));
		assertLedgerMatchesBalance();
	}

	@Test
	void doubleTapWithSameKeyAddsMoneyOnce() throws Exception {
		String first = topUp("same-key", "25.00").andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		String second = topUp("same-key", "25.00").andExpect(status().isOk())
			.andExpect(header().string(TopUpController.REPLAYED, "true"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(JsonPath.<String>read(second, "$.transferId")).isEqualTo(JsonPath.read(first, "$.transferId"));
		assertThat(wallet().getBalance()).isEqualByComparingTo("25.00");
		assertThat(transfers.countByInitiatedBy(userId)).isEqualTo(1);
	}

	@Test
	void sameKeyWithSameAmountWrittenDifferentlyIsStillOneRequest() throws Exception {
		topUp("key-1", "100").andExpect(status().isCreated());
		topUp("key-1", "100.00").andExpect(status().isOk());

		assertThat(wallet().getBalance()).isEqualByComparingTo("100.00");
	}

	@Test
	void sameKeyWithDifferentAmountIsRejected() throws Exception {
		topUp("key-1", "10.00").andExpect(status().isCreated());

		topUp("key-1", "99.00").andExpect(status().isUnprocessableContent());

		assertThat(wallet().getBalance()).isEqualByComparingTo("10.00");
	}

	@Test
	void differentKeysAreDifferentTopUps() throws Exception {
		topUp("key-1", "10.00").andExpect(status().isCreated());
		topUp("key-2", "10.00").andExpect(status().isCreated());

		assertThat(wallet().getBalance()).isEqualByComparingTo("20.00");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-50", "0.001", "10.999", "10000.01", "\"abc\"", "null" })
	void invalidAmountsAreRejected(String amount) throws Exception {
		topUp("key-1", amount).andExpect(status().isBadRequest());

		assertThat(wallet().getBalance()).isEqualByComparingTo("0");
	}

	@Test
	void missingIdempotencyKeyIsRejected() throws Exception {
		mockMvc
			.perform(post("/api/wallets/me/top-ups").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\": 10.00}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void topUpWithoutTokenIsUnauthorized() throws Exception {
		mockMvc
			.perform(post("/api/wallets/me/top-ups").header(TopUpController.IDEMPOTENCY_KEY, "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\": 10.00}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void simultaneousTopUpsNeverLoseMoney() throws Exception {
		int requests = 8;
		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < requests; i++) {
			String key = "parallel-" + i;
			tasks.add(() -> {
				try {
					topUpService.topUp(userId, key, new TopUpRequest(new BigDecimal("10.00")));
					return true;
				}
				catch (ConcurrentUpdateException ex) {
					// Allowed: it changed nothing, and the app told the caller to retry
					return false;
				}
			});
		}

		long succeeded = runAtTheSameTime(tasks).stream().filter(Boolean::booleanValue).count();

		assertThat(succeeded).isPositive();
		assertThat(wallet().getBalance()).isEqualByComparingTo(new BigDecimal("10.00").multiply(BigDecimal.valueOf(succeeded)));
		assertThat(transfers.countByInitiatedBy(userId)).isEqualTo(succeeded);
		assertLedgerMatchesBalance();
	}

	@Test
	void simultaneousDoubleTapsCreateExactlyOneTopUp() throws Exception {
		List<Callable<UUID>> tasks = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			tasks.add(() -> topUpService.topUp(userId, "tapped-many-times", new TopUpRequest(new BigDecimal("25.00")))
				.transfer()
				.transferId());
		}

		List<UUID> transferIds = runAtTheSameTime(tasks);

		assertThat(transferIds).containsOnly(transferIds.getFirst());
		assertThat(wallet().getBalance()).isEqualByComparingTo("25.00");
		assertThat(transfers.countByInitiatedBy(userId)).isEqualTo(1);
		assertLedgerMatchesBalance();
	}

	private ResultActions topUp(String idempotencyKey, String amount) throws Exception {
		return mockMvc.perform(post("/api/wallets/me/top-ups").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.header(TopUpController.IDEMPOTENCY_KEY, idempotencyKey)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\": " + amount + "}"));
	}

	private Wallet wallet() {
		return wallets.findByUserId(userId).orElseThrow();
	}

	private void assertLedgerMatchesBalance() {
		Wallet wallet = wallet();
		assertThat(ledger.sumForWallet(wallet.getId())).isEqualByComparingTo(wallet.getBalance());
		assertThat(ledger.sumOfAllEntries()).isEqualByComparingTo("0");
	}

	/** Starts every task at the same moment, so they really race each other. */
	private static <T> List<T> runAtTheSameTime(List<Callable<T>> tasks) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService pool = Executors.newFixedThreadPool(tasks.size())) {
			List<Future<T>> futures = new ArrayList<>();
			for (Callable<T> task : tasks) {
				futures.add(pool.submit(() -> {
					start.await();
					return task.call();
				}));
			}
			start.countDown();
			List<T> results = new ArrayList<>();
			for (Future<T> future : futures) {
				results.add(future.get());
			}
			return results;
		}
	}

}
