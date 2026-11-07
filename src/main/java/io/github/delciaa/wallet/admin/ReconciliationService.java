package io.github.delciaa.wallet.admin;

import io.github.delciaa.wallet.transfer.LedgerEntryRepository;
import io.github.delciaa.wallet.wallet.WalletRepository;
import io.github.delciaa.wallet.wallet.WalletType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {

	private final ReconciliationRepository reconciliation;

	private final LedgerEntryRepository ledger;

	private final WalletRepository wallets;

	ReconciliationService(ReconciliationRepository reconciliation, LedgerEntryRepository ledger,
			WalletRepository wallets) {
		this.reconciliation = reconciliation;
		this.ledger = ledger;
		this.wallets = wallets;
	}

	/**
	 * REPEATABLE_READ makes all four queries see the same moment. Without it, a transfer
	 * committing between them could make a healthy ledger look unbalanced.
	 */
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public ReconciliationReport check() {
		return ReconciliationReport.of(ledger.sumOfAllEntries(), wallets.countByType(WalletType.USER),
				reconciliation.totalUserBalances(), reconciliation.walletsThatDisagreeWithTheLedger());
	}

}
