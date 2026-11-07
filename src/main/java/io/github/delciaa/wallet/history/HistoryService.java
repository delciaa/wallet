package io.github.delciaa.wallet.history;

import java.util.List;
import java.util.UUID;

import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletNotFoundException;
import io.github.delciaa.wallet.wallet.WalletRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {

	private final HistoryRepository history;

	private final WalletRepository wallets;

	HistoryService(HistoryRepository history, WalletRepository wallets) {
		this.history = history;
		this.wallets = wallets;
	}

	@Transactional(readOnly = true)
	public HistoryPage historyOf(UUID userId, int limit, @Nullable String cursor) {
		Wallet wallet = wallets.findByUserId(userId).orElseThrow(WalletNotFoundException::new);

		// Ask for one extra row: if it comes back, there is another page
		PageRequest oneMore = PageRequest.ofSize(limit + 1);
		List<HistoryRow> rows = (cursor == null) ? history.firstPage(wallet.getId(), oneMore)
				: pageAfter(wallet, HistoryCursor.decode(cursor), oneMore);

		boolean hasMore = rows.size() > limit;
		List<HistoryRow> page = hasMore ? rows.subList(0, limit) : rows;
		String nextCursor = hasMore ? HistoryCursor.after(page.getLast()).encode() : null;
		return new HistoryPage(page.stream().map(TransactionItem::of).toList(), nextCursor);
	}

	private List<HistoryRow> pageAfter(Wallet wallet, HistoryCursor cursor, PageRequest page) {
		return history.pageAfter(wallet.getId(), cursor.createdAt(), cursor.entryId(), page);
	}

}
