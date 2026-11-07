package io.github.delciaa.wallet.admin;

import java.util.UUID;
import java.util.function.Consumer;

import io.github.delciaa.wallet.user.User;
import io.github.delciaa.wallet.user.UserNotFoundException;
import io.github.delciaa.wallet.user.UserRepository;
import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletNotFoundException;
import io.github.delciaa.wallet.wallet.WalletRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** One attempt at freezing or unfreezing a wallet, in its own transaction so it can be retried. */
@Component
class WalletStatusTransaction {

	private final WalletRepository wallets;

	private final UserRepository users;

	WalletStatusTransaction(WalletRepository wallets, UserRepository users) {
		this.wallets = wallets;
		this.users = users;
	}

	@Transactional
	public AdminWalletView apply(UUID walletId, Consumer<Wallet> change) {
		Wallet wallet = wallets.findById(walletId).orElseThrow(WalletNotFoundException::new);
		change.accept(wallet);

		// Write now, so a race with a transfer on this wallet shows up here and gets retried
		wallets.flush();
		return AdminWalletView.of(wallet, ownerOf(wallet));
	}

	private User ownerOf(Wallet wallet) {
		return users.findById(wallet.getUserId()).orElseThrow(UserNotFoundException::new);
	}

}
