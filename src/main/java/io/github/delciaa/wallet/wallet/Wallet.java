package io.github.delciaa.wallet.wallet;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "wallets")
public class Wallet {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", unique = true, updatable = false)
	private UUID userId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 3)
	private String currency = "EUR";

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal balance = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WalletType type = WalletType.USER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WalletStatus status = WalletStatus.ACTIVE;

	@Version
	private Long version;

	protected Wallet() {
	}

	public Wallet(UUID userId) {
		this.userId = userId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getCurrency() {
		return currency;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public WalletType getType() {
		return type;
	}

	public WalletStatus getStatus() {
		return status;
	}

	/** Adds money. The @Version check at commit makes a concurrent change fail instead of being lost. */
	public void credit(BigDecimal amount) {
		requireActive();
		requirePositive(amount);
		balance = balance.add(amount);
	}

	/**
	 * Takes money out, never below zero. If two debits race, the loser is retried and then
	 * sees the lower balance, so it can't overspend either.
	 */
	public void debit(BigDecimal amount) {
		requireActive();
		requirePositive(amount);
		if (balance.compareTo(amount) < 0) {
			throw new InsufficientFundsException();
		}
		balance = balance.subtract(amount);
	}

	public boolean isFrozen() {
		return status == WalletStatus.FROZEN;
	}

	/**
	 * Blocks all money in and out. Freezing bumps the @Version, so a transfer already in
	 * progress on this wallet loses its race, is retried, and then sees the freeze.
	 */
	public void freeze() {
		requireUserWallet();
		status = WalletStatus.FROZEN;
	}

	public void unfreeze() {
		requireUserWallet();
		status = WalletStatus.ACTIVE;
	}

	private void requireUserWallet() {
		if (type != WalletType.USER) {
			throw new SystemWalletException();
		}
	}

	private static void requirePositive(BigDecimal amount) {
		if (amount.signum() <= 0) {
			throw new IllegalArgumentException("Amount must be positive");
		}
	}

	private void requireActive() {
		if (status == WalletStatus.FROZEN) {
			throw new WalletFrozenException();
		}
	}

}
