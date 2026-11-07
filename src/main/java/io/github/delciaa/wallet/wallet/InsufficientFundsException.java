package io.github.delciaa.wallet.wallet;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InsufficientFundsException extends ErrorResponseException {

	public InsufficientFundsException() {
		super(HttpStatus.UNPROCESSABLE_CONTENT,
				ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient funds"), null);
	}

}
