package io.github.delciaa.wallet.transfer;

import java.util.UUID;

import io.github.delciaa.wallet.common.ConflictRetry;
import org.springframework.stereotype.Service;

/**
 * Sends money from the caller to another user. Deliberately not @Transactional: each
 * retry needs a new transaction, which {@link PeerTransferTransaction} starts.
 */
@Service
public class TransferService {

	private final PeerTransferTransaction transaction;

	private final ConflictRetry retry;

	TransferService(PeerTransferTransaction transaction, ConflictRetry retry) {
		this.transaction = transaction;
		this.retry = retry;
	}

	public TransferResult send(UUID senderId, String idempotencyKey, TransferRequest send) {
		IdempotentRequest request = IdempotentRequest.of(senderId, idempotencyKey, send.fingerprint());
		return retry.run(() -> transaction.execute(request, send));
	}

}
