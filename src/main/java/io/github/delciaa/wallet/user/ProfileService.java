package io.github.delciaa.wallet.user;

import java.util.UUID;

import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

	private final UserRepository users;

	private final WalletRepository wallets;

	public ProfileService(UserRepository users, WalletRepository wallets) {
		this.users = users;
		this.wallets = wallets;
	}

	@Transactional(readOnly = true)
	public ProfileResponse profileOf(UUID userId) {
		User user = users.findById(userId).orElseThrow(UserNotFoundException::new);
		Wallet wallet = wallets.findByUserId(userId).orElseThrow(UserNotFoundException::new);
		return ProfileResponse.of(user, wallet);
	}

}
