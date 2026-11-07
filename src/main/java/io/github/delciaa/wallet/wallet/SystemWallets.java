package io.github.delciaa.wallet.wallet;

import java.util.UUID;

/**
 * Built-in wallets created by the V3 migration.
 *
 * <p>The EXTERNAL wallet's {@code balance} column is deliberately never updated: every
 * top-up from every user would otherwise update that one row and conflict with each
 * other. Its balance is the sum of its ledger lines instead.
 */
public final class SystemWallets {

	public static final UUID EXTERNAL = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private SystemWallets() {
	}

}
