package io.github.delciaa.wallet.admin;

import java.util.Optional;

import io.github.delciaa.wallet.user.User;
import io.github.delciaa.wallet.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Makes the account named in {@code ADMIN_EMAIL} an admin when the app starts.
 * The
 * account signs up normally first, so there is no default admin password to
 * leak and no
 * public way to become admin.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	enum Outcome {

		NOT_CONFIGURED, USER_NOT_REGISTERED, ALREADY_ADMIN, PROMOTED

	}

	private final AdminProperties properties;

	private final UserRepository users;

	AdminBootstrap(AdminProperties properties, UserRepository users) {
		this.properties = properties;
		this.users = users;
	}

	@Override
	public void run(ApplicationArguments args) {
		promoteConfiguredAdmin();
	}

	@Transactional
	Outcome promoteConfiguredAdmin() {
		if (!properties.isConfigured()) {
			return Outcome.NOT_CONFIGURED;
		}
		Optional<User> user = users.findByEmail(User.normalizeEmail(properties.email()));
		if (user.isEmpty()) {
			log.warn("ADMIN_EMAIL is set, but no account uses it yet. Register it, then restart the app.");
			return Outcome.USER_NOT_REGISTERED;
		}
		if (user.get().isAdmin()) {
			return Outcome.ALREADY_ADMIN;
		}
		user.get().promoteToAdmin();
		users.save(user.get());
		log.info("Promoted the ADMIN_EMAIL account to admin. It must log in again to get an admin token.");
		return Outcome.PROMOTED;
	}

}
