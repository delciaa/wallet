package io.github.delciaa.wallet.transfer;

import java.math.BigDecimal;

public record TopUpRequest(@ValidAmount BigDecimal amount) {

	/** Same amount means same request, so 100 and 100.00 count as one. */
	String fingerprint() {
		return TransferType.TOP_UP + "|" + amount.stripTrailingZeros().toPlainString();
	}

}
