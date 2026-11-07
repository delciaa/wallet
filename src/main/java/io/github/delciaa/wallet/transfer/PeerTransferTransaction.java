package io.github.delciaa.wallet.transfer;

import java.util.Optional;

import io.github.delciaa.wallet.user.User;
import io.github.delciaa.wallet.user.UserRepository;
import io.github.delciaa.wallet.wallet.Wallet;
import io.github.delciaa.wallet.wallet.WalletNotFoundException;
import io.github.delciaa.wallet.wallet.WalletRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One attempt at sending money to another user, all-or-nothing: both balances, the
 * transfer and its two ledger lines are saved together or not at all.
 */
@Component
class PeerTransferTransaction {

	private final IdempotencyCheck idempotency;

	private final TransferRepository transfers;

	private final WalletRepository wallets;

	private final UserRepository users;

	PeerTransferTransaction(IdempotencyCheck idempotency, TransferRepository transfers, WalletRepository wallets,
			UserRepository users) {
		this.idempotency = idempotency;
		this.transfers = transfers;
		this.wallets = wallets;
		this.users = users;
	}

	@Transactional
	public TransferResult execute(IdempotentRequest request, TransferRequest send) {
		Optional<TransferResult> replay = idempotency.replayOf(request);
		if (replay.isPresent()) {
			return replay.get();
		}

		User recipient = users.findByEmail(User.normalizeEmail(send.recipientEmail()))
			.orElseThrow(RecipientNotFoundException::new);
		if (recipient.getId().equals(request.userId())) {
			throw new SelfTransferException();
		}
		Wallet from = wallets.findByUserId(request.userId()).orElseThrow(WalletNotFoundException::new);
		Wallet to = wallets.findByUserId(recipient.getId()).orElseThrow(RecipientNotFoundException::new);
		if (to.isFrozen()) {
			// Checked here so the sender gets a clear message instead of "Wallet is frozen"
			throw new RecipientUnavailableException();
		}

		from.debit(send.amount());
		to.credit(send.amount());
		Transfer transfer = transfers.save(Transfer.peerToPeer(request, from, to, send.amount()));

		// Write now, so a lost race shows up here as an exception rather than at commit
		transfers.flush();
		return TransferResult.created(transfer);
	}

}
