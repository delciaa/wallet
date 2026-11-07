package io.github.delciaa.wallet.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.github.delciaa.wallet.wallet.Wallet;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A movement of money between two wallets, together with its ledger lines. The only way
 * to create one is through a factory method, which always writes a balanced pair of
 * lines, so an unbalanced transfer can't exist.
 */
@Entity
@Table(name = "transfers")
public class Transfer {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TransferType type;

	@Column(name = "initiated_by", nullable = false, updatable = false)
	private UUID initiatedBy;

	@Column(name = "idempotency_key", nullable = false, updatable = false)
	private String idempotencyKey;

	@Column(name = "request_hash", nullable = false, updatable = false)
	private String requestHash;

	@Column(name = "from_wallet_id", nullable = false, updatable = false)
	private UUID fromWalletId;

	@Column(name = "to_wallet_id", nullable = false, updatable = false)
	private UUID toWalletId;

	@Column(nullable = false, updatable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, updatable = false, length = 3)
	private String currency;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "transfer", cascade = CascadeType.PERSIST)
	private List<LedgerEntry> entries = new ArrayList<>();

	protected Transfer() {
	}

	private Transfer(TransferType type, IdempotentRequest request, UUID fromWalletId, Wallet to, BigDecimal amount) {
		this.type = type;
		this.initiatedBy = request.userId();
		this.idempotencyKey = request.key();
		this.requestHash = request.fingerprint();
		this.fromWalletId = fromWalletId;
		this.toWalletId = to.getId();
		this.amount = amount;
		this.currency = to.getCurrency();
		this.createdAt = Instant.now();
		entries.add(new LedgerEntry(this, fromWalletId, amount.negate(), createdAt));
		entries.add(new LedgerEntry(this, toWalletId, amount, createdAt));
	}

	/** Money entering the system from outside, such as a card payment. */
	static Transfer topUp(IdempotentRequest request, UUID externalWalletId, Wallet to, BigDecimal amount) {
		return new Transfer(TransferType.TOP_UP, request, externalWalletId, to, amount);
	}

	/** Money sent from one user's wallet to another's. */
	static Transfer peerToPeer(IdempotentRequest request, Wallet from, Wallet to, BigDecimal amount) {
		if (!from.getCurrency().equals(to.getCurrency())) {
			throw new IllegalArgumentException("Both wallets must use the same currency");
		}
		return new Transfer(TransferType.P2P, request, from.getId(), to, amount);
	}

	public boolean isSameRequestAs(IdempotentRequest request) {
		return requestHash.equals(request.fingerprint());
	}

	public UUID getId() {
		return id;
	}

	public TransferType getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<LedgerEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

}
