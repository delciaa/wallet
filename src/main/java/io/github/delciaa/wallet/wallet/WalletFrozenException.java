package io.github.delciaa.wallet.wallet;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class WalletFrozenException extends ErrorResponseException {

	public WalletFrozenException() {
		super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Wallet is frozen"), null);
	}

}
