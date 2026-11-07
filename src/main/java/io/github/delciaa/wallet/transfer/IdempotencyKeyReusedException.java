package io.github.delciaa.wallet.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** The same Idempotency-Key was sent again with a different request. */
public class IdempotencyKeyReusedException extends ErrorResponseException {

	public IdempotencyKeyReusedException() {
		super(HttpStatus.UNPROCESSABLE_CONTENT, ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
				"This Idempotency-Key was already used for a different request"), null);
	}

}
