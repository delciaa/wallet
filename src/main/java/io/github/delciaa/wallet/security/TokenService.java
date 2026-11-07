package io.github.delciaa.wallet.security;

import io.github.delciaa.wallet.user.User;

/**
 * Issues access tokens. Callers depend on this interface, not on how tokens are made.
 */
public interface TokenService {

	AccessToken issueFor(User user);

}
