package io.github.delciaa.wallet.history;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.delciaa.wallet.transfer.LedgerEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Reads history straight from the ledger, so it can never disagree with the balance.
 * Newest first, ordered by (created_at, id) so rows with the same time still have a fixed
 * order, which the cursor relies on.
 */
interface HistoryRepository extends Repository<LedgerEntry, UUID> {

	String SELECT_ROWS = """
			select new io.github.delciaa.wallet.history.HistoryRow(
			    e.id, t.id, t.type, e.amount, t.currency, e.createdAt, u.email)
			from LedgerEntry e
			join e.transfer t
			join LedgerEntry other on other.transfer = t and other.id <> e.id
			join Wallet w on w.id = other.walletId
			left join User u on u.id = w.userId
			where e.walletId = :walletId
			""";

	String NEWEST_FIRST = " order by e.createdAt desc, e.id desc";

	@Query(SELECT_ROWS + NEWEST_FIRST)
	List<HistoryRow> firstPage(UUID walletId, Pageable page);

	@Query(SELECT_ROWS + """
			and (e.createdAt < :createdAt or (e.createdAt = :createdAt and e.id < :entryId))
			""" + NEWEST_FIRST)
	List<HistoryRow> pageAfter(UUID walletId, Instant createdAt, UUID entryId, Pageable page);

}
