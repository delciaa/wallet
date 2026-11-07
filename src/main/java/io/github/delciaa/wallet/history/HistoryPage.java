package io.github.delciaa.wallet.history;

import java.util.List;

/** A page of history. {@code nextCursor} is null on the last page. */
public record HistoryPage(List<TransactionItem> items, String nextCursor) {
}
