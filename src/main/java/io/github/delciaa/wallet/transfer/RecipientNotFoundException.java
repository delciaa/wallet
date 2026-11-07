package io.github.delciaa.wallet.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class RecipientNotFoundException extends ErrorResponseException {

	public RecipientNotFoundException() {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recipient not found"),
				null);
	}

}
