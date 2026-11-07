package io.github.delciaa.wallet.history;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidCursorException extends ErrorResponseException {

	public InvalidCursorException() {
		super(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid cursor"), null);
	}

}
