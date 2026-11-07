package io.github.delciaa.wallet.security;

import java.time.Clock;
import java.time.Instant;

import io.github.delciaa.wallet.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues HS256-signed JWTs. The subject is the user's ID, so the server never trusts a
 * user ID sent by the client.
 */
@Service
class JwtTokenService implements TokenService {

	static final String ROLE_CLAIM = "role";

	private final JwtEncoder encoder;

	private final JwtProperties properties;

	private final Clock clock;

	JwtTokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
		this.encoder = encoder;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public AccessToken issueFor(User user) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(properties.ttl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(user.getId().toString())
			.issuedAt(now)
			.expiresAt(expiresAt)
			.claim(ROLE_CLAIM, user.getRole().name())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken(value, now, expiresAt);
	}

}
