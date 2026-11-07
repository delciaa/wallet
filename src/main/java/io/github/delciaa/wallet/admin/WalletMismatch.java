package io.github.delciaa.wallet.admin;

import java.math.BigDecimal;
import java.util.UUID;

/** A wallet whose stored balance disagrees with the sum of its ledger lines. */
public record WalletMismatch(UUID walletId, BigDecimal storedBalance, BigDecimal ledgerBalance) {
}
