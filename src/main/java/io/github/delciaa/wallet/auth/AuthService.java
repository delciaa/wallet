package io.github.delciaa.wallet.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import io.github.delciaa.wallet.user.User;
import io.github.delciaa.wallet.user.UserRepository;
import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	// BCrypt only accepts 72 bytes, and one character can take up to 4 bytes
	private static final int MAX_PASSWORD_BYTES = 72;

	private final UserRepository users;

	private final WalletRepository wallets;

	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository users, WalletRepository wallets, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.wallets = wallets;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is too long");
		}

		String email = request.email().trim().toLowerCase(Locale.ROOT);
		User user = new User(email, passwordEncoder.encode(request.password()));
		try {
			// The UNIQUE constraint decides, so two simultaneous sign-ups can't both win
			users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
		}

		Wallet wallet = wallets.save(new Wallet(user.getId()));
		return new RegisterResponse(user.getId(), user.getEmail(), wallet.getId(), wallet.getBalance(),
				wallet.getCurrency());
	}

}
