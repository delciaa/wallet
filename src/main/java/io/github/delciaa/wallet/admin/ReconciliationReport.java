package io.github.delciaa.wallet.admin;

import java.math.BigDecimal;
import java.util.List;

/**
 * Whether the numbers add up.
 *
 * @param balanced true when the ledger sums to zero and no wallet disagrees with it
 * @param ledgerTotal sum of every ledger line; zero, because every transfer is balanced
 * @param walletsChecked number of user wallets compared with their ledger lines
 * @param moneyInCirculation total of all user balances, which is all money ever topped up
 * minus anything that left the system
 * @param mismatches user wallets whose stored balance differs from their ledger
 */
public record ReconciliationReport(boolean balanced, BigDecimal ledgerTotal, long walletsChecked,
		BigDecimal moneyInCirculation, List<WalletMismatch> mismatches) {

	static ReconciliationReport of(BigDecimal ledgerTotal, long walletsChecked, BigDecimal moneyInCirculation,
			List<WalletMismatch> mismatches) {
		boolean balanced = ledgerTotal.signum() == 0 && mismatches.isEmpty();
		return new ReconciliationReport(balanced, ledgerTotal, walletsChecked, moneyInCirculation, mismatches);
	}

}
