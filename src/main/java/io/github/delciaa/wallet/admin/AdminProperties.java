package io.github.delciaa.wallet.admin;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.admin.email}: the account to make admin at startup, or blank for none. */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(@Nullable String email) {

	boolean isConfigured() {
		return email != null && !email.isBlank();
	}

}
