package io.github.delciaa.wallet.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings from {@code app.jwt.*}. The app refuses to start without a secret of at
 * least 32 bytes, which HS256 needs.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		@NotBlank @Size(min = 32, message = "must be at least 32 characters") String secret,
		@NotNull Duration ttl,
		@NotBlank String issuer) {
}
