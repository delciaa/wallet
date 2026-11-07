package io.github.delciaa.wallet.transfer;

public final class IdempotencyHeaders {

	/** Sent by the client: one value per user action, reused when retrying that action. */
	public static final String KEY = "Idempotency-Key";

	/** Sent back by the server: true when the response is a replay of an earlier request. */
	public static final String REPLAYED = "Idempotent-Replayed";

	private IdempotencyHeaders() {
	}

}
