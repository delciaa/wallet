package io.github.delciaa.wallet.auth;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterResponse(UUID userId, String email, UUID walletId, BigDecimal balance, String currency) {
}
