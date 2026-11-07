package io.github.delciaa.wallet.transfer;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

	@Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.walletId = :walletId")
	BigDecimal sumForWallet(UUID walletId);

	/** Always zero while the ledger is correct, because every transfer is balanced. */
	@Query("select coalesce(sum(e.amount), 0) from LedgerEntry e")
	BigDecimal sumOfAllEntries();

}
