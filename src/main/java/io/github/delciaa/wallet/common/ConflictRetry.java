package io.github.delciaa.wallet.common;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Re-runs an action that lost a race with another request.
 *
 * <p>The action must start its own database transaction. A failed transaction can't be
 * reused, so this class must not itself be inside one; each attempt is a fresh start
 * that sees what the winning request committed.
 *
 * <p>Three kinds of conflict are retried: another request changed the same wallet first
 * (the @Version check); another request with the same Idempotency-Key committed first
 * (the UNIQUE constraint), in which case the next attempt finds it and replays it; or
 * the database broke a deadlock by cancelling this request.
 */
@Component
public class ConflictRetry {

	static final int MAX_ATTEMPTS = 5;

	public <T> T run(Supplier<T> action) {
		for (int attempt = 1;; attempt++) {
			try {
				return action.get();
			}
			catch (OptimisticLockingFailureException | PessimisticLockingFailureException
					| DataIntegrityViolationException ex) {
				if (attempt == MAX_ATTEMPTS) {
					throw new ConcurrentUpdateException(ex);
				}
				pauseBeforeRetry(attempt);
			}
		}
	}

	// Random wait so the requests that lost don't all collide again at the same moment
	private static void pauseBeforeRetry(int attempt) {
		try {
			Thread.sleep(ThreadLocalRandom.current().nextLong(5, 25) * attempt);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ConcurrentUpdateException(ex);
		}
	}

}
