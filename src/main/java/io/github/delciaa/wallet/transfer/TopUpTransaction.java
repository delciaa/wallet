package io.github.delciaa.wallet.transfer;

import java.util.Optional;

import io.github.delciaa.wallet.wallet.SystemWallets;
import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletNotFoundException;
import io.github.delciaa.wallet.wallet.WalletRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One attempt at a top-up, all-or-nothing. If it fails part way, the balance change, the
 * transfer and its ledger lines are all rolled back together.
 */
@Component
class TopUpTransaction {

	private final IdempotencyCheck idempotency;

	private final TransferRepository transfers;

	private final WalletRepository wallets;

	TopUpTransaction(IdempotencyCheck idempotency, TransferRepository transfers, WalletRepository wallets) {
		this.idempotency = idempotency;
		this.transfers = transfers;
		this.wallets = wallets;
	}

	@Transactional
	public TransferResult execute(IdempotentRequest request, TopUpRequest topUp) {
		Optional<TransferResult> replay = idempotency.replayOf(request);
		if (replay.isPresent()) {
			return replay.get();
		}

		Wallet wallet = wallets.findByUserId(request.userId()).orElseThrow(WalletNotFoundException::new);
		wallet.credit(topUp.amount());
		Transfer transfer = transfers.save(Transfer.topUp(request, SystemWallets.EXTERNAL, wallet, topUp.amount()));

		// Write now, so a lost race shows up here as an exception rather than at commit
		transfers.flush();
		return TransferResult.created(transfer);
	}

}
