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
@RequestMapping("/api/transfers")
public class TransferController {

	private final TransferService transferService;

	public TransferController(TransferService transferService) {
		this.transferService = transferService;
	}

	/** The sender is always the token's user, so nobody can spend from someone else's wallet. */
	@PostMapping
	public ResponseEntity<TransferResponse> send(@AuthenticationPrincipal Jwt jwt,
			@RequestHeader(IdempotencyHeaders.KEY) @NotBlank @Size(max = 100) String idempotencyKey,
			@Valid @RequestBody TransferRequest request) {
		return transferService.send(UUID.fromString(jwt.getSubject()), idempotencyKey, request).toResponseEntity();
	}

}
