package io.github.delciaa.wallet.transfer;

/** A transfer, and whether it was just made or replayed from an earlier request with the same key. */
public record TransferResult(TransferResponse transfer, boolean replayed) {

	static TransferResult created(Transfer transfer) {
		return new TransferResult(TransferResponse.of(transfer), false);
	}

	static TransferResult replayed(Transfer transfer) {
		return new TransferResult(TransferResponse.of(transfer), true);
	}

}
