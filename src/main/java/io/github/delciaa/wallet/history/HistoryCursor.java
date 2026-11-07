package io.github.delciaa.wallet.history;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

/**
 * Where the last page ended: the time and ID of its last row. The next page starts just
 * after it, so new transactions arriving meanwhile never shift or repeat rows the way
 * page numbers would.
 *
 * <p>Clients treat it as an opaque string. Editing it can only change which of your own
 * rows you see, because the wallet always comes from the token.
 */
record HistoryCursor(Instant createdAt, UUID entryId) {

	private static final String SEPARATOR = "|";

	static HistoryCursor after(HistoryRow row) {
		return new HistoryCursor(row.createdAt(), row.entryId());
	}

	String encode() {
		String raw = createdAt + SEPARATOR + entryId;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	static HistoryCursor decode(String encoded) {
		try {
			String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
			int split = raw.indexOf(SEPARATOR);
			if (split < 0) {
				throw new InvalidCursorException();
			}
			return new HistoryCursor(Instant.parse(raw.substring(0, split)), UUID.fromString(raw.substring(split + 1)));
		}
		catch (IllegalArgumentException | DateTimeParseException ex) {
			throw new InvalidCursorException();
		}
	}

}
