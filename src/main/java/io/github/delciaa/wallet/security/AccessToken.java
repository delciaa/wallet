package io.github.delciaa.wallet.security;

import java.time.Duration;
import java.time.Instant;

public record AccessToken(String value, Instant issuedAt, Instant expiresAt) {

	public long lifetimeSeconds() {
		return Duration.between(issuedAt, expiresAt).toSeconds();
	}

}
