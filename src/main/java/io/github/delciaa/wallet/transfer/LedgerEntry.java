package io.github.delciaa.wallet.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One line of the ledger: negative takes money out of a wallet, positive puts it in. */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transfer_id", nullable = false, updatable = false)
	private Transfer transfer;

	@Column(name = "wallet_id", nullable = false, updatable = false)
	private UUID walletId;

	@Column(nullable = false, updatable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected LedgerEntry() {
	}

	LedgerEntry(Transfer transfer, UUID walletId, BigDecimal amount, Instant createdAt) {
		this.transfer = transfer;
		this.walletId = walletId;
		this.amount = amount;
		this.createdAt = createdAt;
	}

	public UUID getWalletId() {
		return walletId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

}
