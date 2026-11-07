package io.github.delciaa.wallet.transfer;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record TopUpRequest(
		@NotNull @DecimalMin("0.01") @DecimalMax("10000.00") @Digits(integer = 5, fraction = 2) BigDecimal amount) {

	/** Same amount means same request, so 100 and 100.00 count as one. */
	String fingerprint() {
		return TransferType.TOP_UP + "|" + amount.stripTrailingZeros().toPlainString();
	}

}
