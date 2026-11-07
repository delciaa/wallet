package io.github.delciaa.wallet.transfer;

import java.math.BigDecimal;

import io.github.delciaa.wallet.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TransferRequest(@NotBlank @Email @Size(max = 254) String recipientEmail, @ValidAmount BigDecimal amount) {

	/** Same recipient and amount means same request, ignoring email case and trailing zeros. */
	String fingerprint() {
		return TransferType.P2P + "|" + User.normalizeEmail(recipientEmail) + "|"
				+ amount.stripTrailingZeros().toPlainString();
	}

}
