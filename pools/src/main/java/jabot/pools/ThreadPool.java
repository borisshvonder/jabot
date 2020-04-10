package jabot.pools;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Only needed to give access to the queue
 */
public class ThreadPool extends ThreadPoolExecutor {
	private final LinkedBlockingQueue<Runnable> queue;
	
	/**
	 * thread pool with default values
	 */
	public ThreadPool() {
		this(1, 1);
	}
	
	/**
	 * thread pool with most common params
	 */
	public ThreadPool(final int maximumPoolSize, final int queueSize) {
		this(0, maximumPoolSize, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(queueSize), 
				new NamedFactory("DEFAULT", false), new CallerRunsPolicy());
	}

	public ThreadPool(
			final int corePoolSize, 
			final int maximumPoolSize, 
			final long keepAliveTime, 
			final TimeUnit unit,
			final LinkedBlockingQueue<Runnable> workQueue, 
			final ThreadFactory threadFactory, 
			final RejectedExecutionHandler handler
	) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
		this.queue = workQueue;
	}
	
	public int getQueueRemainingCapacity() {
		return queue.remainingCapacity();
	}
}
