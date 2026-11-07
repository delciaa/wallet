package io.github.delciaa.wallet.admin;

import java.math.BigDecimal;
import java.util.UUID;

import io.github.delciaa.wallet.user.User;
import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletStatus;

/** What an admin sees about one user's wallet. */
public record AdminWalletView(UUID walletId, UUID userId, String email, BigDecimal balance, String currency,
		WalletStatus status) {

	static AdminWalletView of(Wallet wallet, User owner) {
		return new AdminWalletView(wallet.getId(), owner.getId(), owner.getEmail(), wallet.getBalance(),
				wallet.getCurrency(), wallet.getStatus());
	}

}
