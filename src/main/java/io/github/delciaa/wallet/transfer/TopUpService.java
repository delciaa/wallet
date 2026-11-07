package io.github.delciaa.wallet.transfer;

import java.util.UUID;

import io.github.delciaa.wallet.common.ConflictRetry;
import org.springframework.stereotype.Service;

/**
 * Adds pretend money to the caller's wallet. Deliberately not @Transactional: each retry
 * needs a new transaction, which {@link TopUpTransaction} starts.
 */
@Service
public class TopUpService {

	private final TopUpTransaction transaction;

	private final ConflictRetry retry;

	TopUpService(TopUpTransaction transaction, ConflictRetry retry) {
		this.transaction = transaction;
		this.retry = retry;
	}

	public TransferResult topUp(UUID userId, String idempotencyKey, TopUpRequest topUp) {
		IdempotentRequest request = IdempotentRequest.of(userId, idempotencyKey, topUp.fingerprint());
		return retry.run(() -> transaction.execute(request, topUp));
	}

}
