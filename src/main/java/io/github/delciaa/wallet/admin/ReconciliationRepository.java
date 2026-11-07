package io.github.delciaa.wallet.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.github.delciaa.wallet.wallet.Wallet;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Compares the two records of every balance: the stored {@code balance} column and the
 * ledger lines. The system wallet is left out on purpose, since its stored balance is
 * never updated (see SystemWallets).
 */
interface ReconciliationRepository extends Repository<Wallet, UUID> {

	@Query("""
			select new io.github.delciaa.wallet.admin.WalletMismatch(w.id, w.balance, coalesce(sum(e.amount), 0))
			from Wallet w
			left join LedgerEntry e on e.walletId = w.id
			where w.type = io.github.delciaa.wallet.wallet.WalletType.USER
			group by w.id, w.balance
			having w.balance <> coalesce(sum(e.amount), 0)
			""")
	List<WalletMismatch> walletsThatDisagreeWithTheLedger();

	@Query("""
			select coalesce(sum(w.balance), 0) from Wallet w
			where w.type = io.github.delciaa.wallet.wallet.WalletType.USER
			""")
	BigDecimal totalUserBalances();

}
