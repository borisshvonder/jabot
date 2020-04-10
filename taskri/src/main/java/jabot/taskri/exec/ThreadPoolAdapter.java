package jabot.taskri.exec;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;

import jabot.common.types.Interval;
import jabot.pools.ThreadPool;

public class ThreadPoolAdapter implements Executor {
	private final ThreadPool delegate;
	
	public ThreadPoolAdapter(ThreadPool delegate) {
		Validate.notNull(delegate, "delegate cannot be null");
		
		this.delegate = delegate;
	}

	@Override
	public int available() {
		return delegate.getMaximumPoolSize() + delegate.getQueueRemainingCapacity() - delegate.getActiveCount();
	}

	@Override
	public void submit(Runnable task) {
		delegate.execute(task);
	}

	@Override
	public boolean shutdown(final Interval waitTime) throws InterruptedException {
		delegate.shutdown();
		return delegate.awaitTermination(waitTime.getValue(), waitTime.getUnit());
	}

	@Override
	public void close() throws Exception {
		delegate.shutdownNow();
		if (!delegate.awaitTermination(1, TimeUnit.MINUTES)) {
			throw new RuntimeException("Cannot interrupt all tasks within 1 minute");
		}
	}

}
