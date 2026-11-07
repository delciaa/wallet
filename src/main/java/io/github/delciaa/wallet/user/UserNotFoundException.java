package io.github.delciaa.wallet.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** A valid token whose user no longer exists. */
public class UserNotFoundException extends ErrorResponseException {

	public UserNotFoundException() {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "User not found"), null);
	}

}
