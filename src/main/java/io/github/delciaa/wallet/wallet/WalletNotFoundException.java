package io.github.delciaa.wallet.wallet;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class WalletNotFoundException extends ErrorResponseException {

	public WalletNotFoundException() {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Wallet not found"), null);
	}

}
