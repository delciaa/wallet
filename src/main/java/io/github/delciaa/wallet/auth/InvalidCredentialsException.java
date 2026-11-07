package io.github.delciaa.wallet.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Thrown for both an unknown email and a wrong password, so the response never reveals
 * which emails are registered.
 */
public class InvalidCredentialsException extends ErrorResponseException {

	public InvalidCredentialsException() {
		super(HttpStatus.UNAUTHORIZED,
				ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password"), null);
	}

}
