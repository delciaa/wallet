package io.github.delciaa.wallet.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Too many simultaneous changes to the same wallet. Nothing was changed, so retrying is safe. */
public class ConcurrentUpdateException extends ErrorResponseException {

	public ConcurrentUpdateException(Throwable cause) {
		super(HttpStatus.CONFLICT,
				ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Wallet is busy, please try again"), cause);
	}

}
