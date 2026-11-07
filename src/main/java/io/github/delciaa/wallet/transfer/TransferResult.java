package io.github.delciaa.wallet.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** A transfer, and whether it was just made or replayed from an earlier request with the same key. */
public record TransferResult(TransferResponse transfer, boolean replayed) {

	static TransferResult created(Transfer transfer) {
		return new TransferResult(TransferResponse.of(transfer), false);
	}

	static TransferResult replayed(Transfer transfer) {
		return new TransferResult(TransferResponse.of(transfer), true);
	}

	/** 201 for a new transfer; 200 with the original body when the same request is sent again. */
	ResponseEntity<TransferResponse> toResponseEntity() {
		return ResponseEntity.status(replayed ? HttpStatus.OK : HttpStatus.CREATED)
			.header(IdempotencyHeaders.REPLAYED, String.valueOf(replayed))
			.body(transfer);
	}

}
