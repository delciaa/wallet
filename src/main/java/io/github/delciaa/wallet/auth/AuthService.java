package io.github.delciaa.wallet.auth;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import io.github.delciaa.wallet.security.AccessToken;
import io.github.delciaa.wallet.security.TokenService;
import io.github.delciaa.wallet.user.User;
import io.github.delciaa.wallet.user.UserRepository;
import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	// BCrypt only accepts 72 bytes, and one character can take up to 4 bytes
	private static final int MAX_PASSWORD_BYTES = 72;

	private final UserRepository users;

	private final WalletRepository wallets;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokenService;

	// Checked against when the email is unknown, so both failures take the same time
	private final String dummyPasswordHash;

	public AuthService(UserRepository users, WalletRepository wallets, PasswordEncoder passwordEncoder,
			TokenService tokenService) {
		this.users = users;
		this.wallets = wallets;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
	}

	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		if (!fitsBcrypt(request.password())) {
			throw new PasswordTooLongException();
		}

		User user = new User(request.email(), passwordEncoder.encode(request.password()));
		try {
			// The UNIQUE constraint decides, so two simultaneous sign-ups can't both win
			users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			throw new EmailAlreadyRegisteredException(ex);
		}

		Wallet wallet = wallets.save(new Wallet(user.getId()));
		return new RegisterResponse(user.getId(), user.getEmail(), wallet.getId(), wallet.getBalance(),
				wallet.getCurrency());
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		Optional<User> user = users.findByEmail(User.normalizeEmail(request.email()));
		String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
		boolean passwordMatches = fitsBcrypt(request.password()) && passwordEncoder.matches(request.password(), hash);
		if (user.isEmpty() || !passwordMatches) {
			throw new InvalidCredentialsException();
		}

		AccessToken token = tokenService.issueFor(user.get());
		return LoginResponse.bearer(token.value(), token.lifetimeSeconds());
	}

	private static boolean fitsBcrypt(String password) {
		return password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;
	}

}
