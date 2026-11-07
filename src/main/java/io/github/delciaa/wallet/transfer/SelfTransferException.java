package io.github.delciaa.wallet.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class SelfTransferException extends ErrorResponseException {

	public SelfTransferException() {
		super(HttpStatus.UNPROCESSABLE_CONTENT, ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
				"You can't send money to yourself"), null);
	}

}
