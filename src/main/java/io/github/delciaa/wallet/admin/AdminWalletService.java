package io.github.delciaa.wallet.admin;

import java.util.UUID;

import io.github.delciaa.wallet.common.ConflictRetry;
import io.github.delciaa.wallet.user.User;
import io.github.delciaa.wallet.user.UserNotFoundException;
import io.github.delciaa.wallet.user.UserRepository;
import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletNotFoundException;
import io.github.delciaa.wallet.wallet.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin actions on wallets. Freezing goes through {@link ConflictRetry} because it can
 * race a transfer on the same wallet, just like two transfers can.
 */
@Service
public class AdminWalletService {

	private final WalletStatusTransaction statusChange;

	private final ConflictRetry retry;

	private final UserRepository users;

	private final WalletRepository wallets;

	AdminWalletService(WalletStatusTransaction statusChange, ConflictRetry retry, UserRepository users,
			WalletRepository wallets) {
		this.statusChange = statusChange;
		this.retry = retry;
		this.users = users;
		this.wallets = wallets;
	}

	@Transactional(readOnly = true)
	public AdminWalletView findByEmail(String email) {
		User owner = users.findByEmail(User.normalizeEmail(email)).orElseThrow(UserNotFoundException::new);
		Wallet wallet = wallets.findByUserId(owner.getId()).orElseThrow(WalletNotFoundException::new);
		return AdminWalletView.of(wallet, owner);
	}

	/** Freezing an already frozen wallet is fine, so a retried admin request is harmless. */
	public AdminWalletView freeze(UUID walletId) {
		return retry.run(() -> statusChange.apply(walletId, Wallet::freeze));
	}

	public AdminWalletView unfreeze(UUID walletId) {
		return retry.run(() -> statusChange.apply(walletId, Wallet::unfreeze));
	}

}
