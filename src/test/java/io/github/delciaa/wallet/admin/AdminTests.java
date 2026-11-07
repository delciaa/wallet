package io.github.delciaa.wallet.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.github.delciaa.wallet.TestcontainersConfiguration;
import io.github.delciaa.wallet.auth.AuthService;
import io.github.delciaa.wallet.auth.LoginRequest;
import io.github.delciaa.wallet.auth.RegisterRequest;
import io.github.delciaa.wallet.common.ConcurrentUpdateException;
import io.github.delciaa.wallet.support.Concurrently;
import io.github.delciaa.wallet.transfer.IdempotencyHeaders;
import io.github.delciaa.wallet.transfer.LedgerEntryRepository;
import io.github.delciaa.wallet.transfer.TopUpRequest;
import io.github.delciaa.wallet.transfer.TopUpService;
import io.github.delciaa.wallet.transfer.TransferRequest;
import io.github.delciaa.wallet.transfer.TransferService;
import io.github.delciaa.wallet.user.User;
import io.github.delciaa.wallet.user.UserRepository;
import io.github.delciaa.wallet.wallet.SystemWallets;
import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletFrozenException;
import io.github.delciaa.wallet.wallet.WalletRepository;
import io.github.delciaa.wallet.wallet.WalletStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminTests {

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
	private UserRepository users;

	@Autowired
	private WalletRepository wallets;

	@Autowired
	private LedgerEntryRepository ledger;

	@Autowired
	private JdbcTemplate jdbc;

	private TestUser admin;

	private TestUser ana;

	private TestUser maria;

	@BeforeEach
	void createUsers() {
		admin = newAdmin();
		ana = newUser("ana");
		maria = newUser("maria");
	}

	// --- access ---

	@Test
	void normalUsersCannotUseAdminEndpoints() throws Exception {
		mockMvc.perform(as(ana, get("/api/admin/reconciliation"))).andExpect(status().isForbidden());
		mockMvc.perform(as(ana, post("/api/admin/wallets/{id}/freeze", walletOf(maria).getId())))
			.andExpect(status().isForbidden());
		assertThat(walletOf(maria).getStatus()).isEqualTo(WalletStatus.ACTIVE);
	}

	@Test
	void adminEndpointsNeedAToken() throws Exception {
		mockMvc.perform(get("/api/admin/reconciliation")).andExpect(status().isUnauthorized());
	}

	@Test
	void adminCanFindAWalletByEmail() throws Exception {
		topUp(ana, "40.00");

		mockMvc.perform(as(admin, get("/api/admin/wallets").param("email", ana.email().toUpperCase())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.walletId").value(walletOf(ana).getId().toString()))
			.andExpect(jsonPath("$.email").value(ana.email()))
			.andExpect(jsonPath("$.balance").value(40.00))
			.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void findingAnUnknownEmailIsNotFound() throws Exception {
		mockMvc.perform(as(admin, get("/api/admin/wallets").param("email", "nobody-" + UUID.randomUUID() + "@x.com")))
			.andExpect(status().isNotFound());
	}

	// --- freezing ---

	@Test
	void frozenWalletCannotSendReceiveOrTopUp() throws Exception {
		topUp(ana, "100.00");
		topUp(maria, "100.00");

		freeze(ana).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FROZEN"));

		send(ana, maria, "10.00").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Wallet is frozen"));
		send(maria, ana, "10.00").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Recipient can't receive money right now"));
		topUpOverHttp(ana, "10.00").andExpect(status().isConflict());

		assertThat(walletOf(ana).getBalance()).isEqualByComparingTo("100.00");
		assertThat(walletOf(maria).getBalance()).isEqualByComparingTo("100.00");
	}

	@Test
	void frozenUserCanStillLogInAndSeeTheirMoney() throws Exception {
		topUp(ana, "100.00");
		freeze(ana);

		String token = authService.login(new LoginRequest(ana.email(), PASSWORD)).accessToken();
		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.walletStatus").value("FROZEN"))
			.andExpect(jsonPath("$.balance").value(100.00));
		mockMvc.perform(as(ana, get("/api/wallets/me/transactions")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(1)));
	}

	@Test
	void unfreezingAllowsMoneyToMoveAgain() throws Exception {
		topUp(ana, "100.00");
		freeze(ana);

		mockMvc.perform(as(admin, post("/api/admin/wallets/{id}/unfreeze", walletOf(ana).getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		send(ana, maria, "10.00").andExpect(status().isCreated());
		assertThat(walletOf(maria).getBalance()).isEqualByComparingTo("10.00");
	}

	@Test
	void freezingTwiceIsHarmless() throws Exception {
		freeze(ana).andExpect(status().isOk());
		freeze(ana).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FROZEN"));
	}

	@Test
	void systemWalletCannotBeFrozen() throws Exception {
		mockMvc.perform(as(admin, post("/api/admin/wallets/{id}/freeze", SystemWallets.EXTERNAL)))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void freezingAnUnknownWalletIsNotFound() throws Exception {
		mockMvc.perform(as(admin, post("/api/admin/wallets/{id}/freeze", UUID.randomUUID())))
			.andExpect(status().isNotFound());
	}

	@Test
	void freezeRacingSendsNeverCorruptsMoney() throws Exception {
		topUp(ana, "100.00");
		UUID anaWallet = walletOf(ana).getId();
		List<Callable<String>> tasks = new ArrayList<>();
		tasks.add(() -> mockMvc.perform(as(admin, post("/api/admin/wallets/{id}/freeze", anaWallet)))
			.andReturn()
			.getResponse()
			.getStatus() == 200 ? "FROZE" : "FREEZE_FAILED");
		for (int i = 0; i < 6; i++) {
			String key = "race-" + i;
			tasks.add(() -> {
				try {
					transferService.send(ana.id(), key, new TransferRequest(maria.email(), new BigDecimal("5.00")));
					return "SENT";
				}
				catch (WalletFrozenException ex) {
					return "BLOCKED";
				}
				catch (ConcurrentUpdateException ex) {
					return "BUSY";
				}
			});
		}

		List<String> outcomes = Concurrently.run(tasks);

		assertThat(outcomes).contains("FROZE").doesNotContain("FREEZE_FAILED");
		assertThat(walletOf(ana).getStatus()).isEqualTo(WalletStatus.FROZEN);
		long sent = outcomes.stream().filter("SENT"::equals).count();
		BigDecimal moved = new BigDecimal("5.00").multiply(BigDecimal.valueOf(sent));
		assertThat(walletOf(ana).getBalance()).isEqualByComparingTo(new BigDecimal("100.00").subtract(moved));
		assertThat(walletOf(maria).getBalance()).isEqualByComparingTo(moved);
		assertThat(ledger.sumForWallet(anaWallet)).isEqualByComparingTo(walletOf(ana).getBalance());
	}

	// --- reconciliation ---

	@Test
	void reconciliationIsBalancedAfterRealActivity() throws Exception {
		topUp(ana, "100.00");
		transferService.send(ana.id(), "s-" + UUID.randomUUID(), new TransferRequest(maria.email(), new BigDecimal("25.00")));

		mockMvc.perform(as(admin, get("/api/admin/reconciliation")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balanced").value(true))
			.andExpect(jsonPath("$.ledgerTotal").value(0))
			.andExpect(jsonPath("$.mismatches", hasSize(0)));
	}

	@Test
	void reconciliationCatchesABalanceChangedBehindTheLedgersBack() throws Exception {
		topUp(ana, "100.00");
		UUID anaWallet = walletOf(ana).getId();

		// Simulates a bug or a manual database edit: the balance changes with no ledger line
		jdbc.update("update wallets set balance = balance + 1 where id = ?", anaWallet);
		try {
			mockMvc.perform(as(admin, get("/api/admin/reconciliation")))
				.andExpect(jsonPath("$.balanced").value(false))
				.andExpect(jsonPath("$.mismatches", hasSize(1)))
				.andExpect(jsonPath("$.mismatches[0].walletId").value(anaWallet.toString()))
				.andExpect(jsonPath("$.mismatches[0].storedBalance").value(101.00))
				.andExpect(jsonPath("$.mismatches[0].ledgerBalance").value(100.00));
		}
		finally {
			// Other tests share this database, so put it back
			jdbc.update("update wallets set balance = balance - 1 where id = ?", anaWallet);
		}
	}

	// --- first admin ---

	@Test
	void bootstrapPromotesTheConfiguredAccount() {
		TestUser future = newUser("boss");

		AdminBootstrap.Outcome outcome = bootstrapFor(future.email().toUpperCase()).promoteConfiguredAdmin();

		assertThat(outcome).isEqualTo(AdminBootstrap.Outcome.PROMOTED);
		assertThat(users.findById(future.id()).orElseThrow().isAdmin()).isTrue();
		assertThat(bootstrapFor(future.email()).promoteConfiguredAdmin())
			.isEqualTo(AdminBootstrap.Outcome.ALREADY_ADMIN);
	}

	@Test
	void bootstrapDoesNothingWithoutAnEmail() {
		assertThat(bootstrapFor("").promoteConfiguredAdmin()).isEqualTo(AdminBootstrap.Outcome.NOT_CONFIGURED);
		assertThat(bootstrapFor(null).promoteConfiguredAdmin()).isEqualTo(AdminBootstrap.Outcome.NOT_CONFIGURED);
	}

	@Test
	void bootstrapWaitsForTheAccountToBeRegistered() {
		assertThat(bootstrapFor("not-yet-" + UUID.randomUUID() + "@example.com").promoteConfiguredAdmin())
			.isEqualTo(AdminBootstrap.Outcome.USER_NOT_REGISTERED);
	}

	// --- helpers ---

	private record TestUser(UUID id, String email, String token) {
	}

	private TestUser newUser(String name) {
		String email = name + "-" + UUID.randomUUID() + "@example.com";
		UUID id = authService.register(new RegisterRequest(email, PASSWORD)).userId();
		String token = authService.login(new LoginRequest(email, PASSWORD)).accessToken();
		return new TestUser(id, email, token);
	}

	private TestUser newAdmin() {
		String email = "admin-" + UUID.randomUUID() + "@example.com";
		UUID id = authService.register(new RegisterRequest(email, PASSWORD)).userId();
		User user = users.findById(id).orElseThrow();
		user.promoteToAdmin();
		users.save(user);
		String token = authService.login(new LoginRequest(email, PASSWORD)).accessToken();
		return new TestUser(id, email, token);
	}

	private AdminBootstrap bootstrapFor(String email) {
		return new AdminBootstrap(new AdminProperties(email), users);
	}

	private Wallet walletOf(TestUser user) {
		return wallets.findByUserId(user.id()).orElseThrow();
	}

	private void topUp(TestUser user, String amount) {
		topUpService.topUp(user.id(), "fund-" + UUID.randomUUID(), new TopUpRequest(new BigDecimal(amount)));
	}

	private ResultActions freeze(TestUser user) throws Exception {
		return mockMvc.perform(as(admin, post("/api/admin/wallets/{id}/freeze", walletOf(user).getId())));
	}

	private ResultActions send(TestUser from, TestUser to, String amount) throws Exception {
		return mockMvc.perform(as(from, post("/api/transfers")).header(IdempotencyHeaders.KEY, UUID.randomUUID().toString())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"recipientEmail\": \"%s\", \"amount\": %s}".formatted(to.email(), amount)));
	}

	private ResultActions topUpOverHttp(TestUser user, String amount) throws Exception {
		return mockMvc.perform(as(user, post("/api/wallets/me/top-ups")).header(IdempotencyHeaders.KEY, UUID.randomUUID().toString())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\": " + amount + "}"));
	}

	private static MockHttpServletRequestBuilder as(TestUser user, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token());
	}

}
