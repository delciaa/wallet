package io.github.delciaa.wallet.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

	public static LoginResponse bearer(String accessToken, long expiresInSeconds) {
		return new LoginResponse(accessToken, "Bearer", expiresInSeconds);
	}

}
