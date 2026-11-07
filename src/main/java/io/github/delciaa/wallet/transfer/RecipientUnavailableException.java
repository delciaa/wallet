package io.github.delciaa.wallet.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** The recipient's wallet is frozen. Worded so it doesn't say why, which is private to them. */
public class RecipientUnavailableException extends ErrorResponseException {

	public RecipientUnavailableException() {
		super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Recipient can't receive money right now"), null);
	}

}
