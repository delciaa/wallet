package io.github.delciaa.wallet.history;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.github.delciaa.wallet.transfer.TransferType;

/**
 * One of the caller's ledger lines, joined to its transfer and to the other side of it.
 * {@code counterpartyEmail} is null when the other side is a system wallet, as in a top-up.
 */
public record HistoryRow(UUID entryId, UUID transferId, TransferType type, BigDecimal amount, String currency,
		Instant createdAt, String counterpartyEmail) {
}
