package io.github.delciaa.wallet.user;

import java.math.BigDecimal;
import java.util.UUID;

import io.github.delciaa.wallet.wallet.Wallet;

public record ProfileResponse(UUID userId, String email, Role role, UUID walletId, BigDecimal balance,
		String currency) {

	static ProfileResponse of(User user, Wallet wallet) {
		return new ProfileResponse(user.getId(), user.getEmail(), user.getRole(), wallet.getId(), wallet.getBalance(),
				wallet.getCurrency());
	}

}
