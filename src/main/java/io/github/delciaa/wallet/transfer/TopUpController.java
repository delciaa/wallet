package io.github.delciaa.wallet.transfer;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets/me/top-ups")
public class TopUpController {

	private final TopUpService topUpService;

	public TopUpController(TopUpService topUpService) {
		this.topUpService = topUpService;
	}

	@PostMapping
	public ResponseEntity<TransferResponse> topUp(@AuthenticationPrincipal Jwt jwt,
			@RequestHeader(IdempotencyHeaders.KEY) @NotBlank @Size(max = 100) String idempotencyKey,
			@Valid @RequestBody TopUpRequest request) {
		return topUpService.topUp(UUID.fromString(jwt.getSubject()), idempotencyKey, request).toResponseEntity();
	}

}
