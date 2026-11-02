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

}
