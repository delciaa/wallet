package io.github.delciaa.wallet.transfer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Who sent a request, the Idempotency-Key they sent with it, and a fingerprint
 * of what
 * they asked for. Reusing a key is only allowed for the exact same request.
 */
public record IdempotentRequest(UUID userId, String key, String fingerprint) {

	public static IdempotentRequest of(UUID userId, String key, String... parts) {
		return new IdempotentRequest(userId, key, sha256(String.join("|", parts)));
	}

	private static String sha256(String value) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is always available in Java", ex);
		}
	}

}
