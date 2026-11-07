package io.github.delciaa.wallet.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Runs tasks on separate threads, released at the same moment so they really race. */
public final class Concurrently {

	private Concurrently() {
	}

	/** Each task's result, in task order. An unexpected exception in any task fails the call. */
	public static <T> List<T> run(List<Callable<T>> tasks) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService pool = Executors.newFixedThreadPool(tasks.size())) {
			List<Future<T>> futures = new ArrayList<>();
			for (Callable<T> task : tasks) {
				futures.add(pool.submit(() -> {
					start.await();
					return task.call();
				}));
			}
			start.countDown();
			List<T> results = new ArrayList<>();
			for (Future<T> future : futures) {
				results.add(future.get());
			}
			return results;
		}
	}

}
