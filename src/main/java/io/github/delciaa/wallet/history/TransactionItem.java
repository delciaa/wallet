package io.github.delciaa.wallet.history;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.github.delciaa.wallet.transfer.TransferType;

/**
 * One line of the caller's history. {@code amount} is signed from the caller's side:
 * positive came in, negative went out.
 */
public record TransactionItem(UUID transferId, TransferType type, Direction direction, BigDecimal amount,
		String currency, String counterparty, Instant createdAt) {

	public enum Direction {

		IN, OUT

	}

	static TransactionItem of(HistoryRow row) {
		Direction direction = row.amount().signum() > 0 ? Direction.IN : Direction.OUT;
		return new TransactionItem(row.transferId(), row.type(), direction, row.amount(), row.currency(),
				row.counterpartyEmail(), row.createdAt());
	}

}
