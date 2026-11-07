package io.github.delciaa.wallet.wallet;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class SystemWalletException extends ErrorResponseException {

	public SystemWalletException() {
		super(HttpStatus.UNPROCESSABLE_CONTENT, ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
				"System wallets can't be frozen"), null);
	}

}
