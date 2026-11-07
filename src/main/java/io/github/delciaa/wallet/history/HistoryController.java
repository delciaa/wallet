package io.github.delciaa.wallet.history;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets/me/transactions")
public class HistoryController {

	private final HistoryService historyService;

	public HistoryController(HistoryService historyService) {
		this.historyService = historyService;
	}

	/** The caller's own history, newest first. Pass {@code nextCursor} back as {@code cursor} for the next page. */
	@GetMapping
	public HistoryPage history(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) @Nullable String cursor) {
		return historyService.historyOf(UUID.fromString(jwt.getSubject()), limit, cursor);
	}

}
