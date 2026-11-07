package io.github.delciaa.wallet.admin;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Everything under /api/admin needs an ADMIN token; SecurityConfig enforces that. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final AdminWalletService walletService;

	private final ReconciliationService reconciliationService;

	public AdminController(AdminWalletService walletService, ReconciliationService reconciliationService) {
		this.walletService = walletService;
		this.reconciliationService = reconciliationService;
	}

	@GetMapping("/wallets")
	public AdminWalletView findWallet(@RequestParam @NotBlank @Email String email) {
		return walletService.findByEmail(email);
	}

	@PostMapping("/wallets/{walletId}/freeze")
	public AdminWalletView freeze(@PathVariable UUID walletId) {
		return walletService.freeze(walletId);
	}

	@PostMapping("/wallets/{walletId}/unfreeze")
	public AdminWalletView unfreeze(@PathVariable UUID walletId) {
		return walletService.unfreeze(walletId);
	}

	@GetMapping("/reconciliation")
	public ReconciliationReport reconcile() {
		return reconciliationService.check();
	}

}
