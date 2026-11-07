package io.github.delciaa.wallet.transfer;

import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Finds an earlier transfer made with the same Idempotency-Key. Used by every flow that
 * moves money, before it moves any.
 */
@Component
class IdempotencyCheck {

	private final TransferRepository transfers;

	IdempotencyCheck(TransferRepository transfers) {
		this.transfers = transfers;
	}

	/**
	 * The earlier result if this request was already handled, or empty if it's new.
	 * @throws IdempotencyKeyReusedException if the key was used for a different request
	 */
	Optional<TransferResult> replayOf(IdempotentRequest request) {
		return transfers.findByInitiatedByAndIdempotencyKey(request.userId(), request.key()).map(earlier -> {
			if (!earlier.isSameRequestAs(request)) {
				throw new IdempotencyKeyReusedException();
			}
			return TransferResult.replayed(earlier);
		});
	}

}
