package io.github.delciaa.wallet.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(UUID transferId, TransferType type, BigDecimal amount, String currency,
		Instant createdAt) {

	static TransferResponse of(Transfer transfer) {
		return new TransferResponse(transfer.getId(), transfer.getType(), transfer.getAmount(), transfer.getCurrency(),
				transfer.getCreatedAt());
	}

}
