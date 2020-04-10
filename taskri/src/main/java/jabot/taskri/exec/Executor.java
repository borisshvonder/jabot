package jabot.taskri.exec;

import jabot.common.types.Interval;

/**
 * Simple adapter around ThreadPoolExecutor to facilitate testing
 */
public interface Executor extends AutoCloseable {
	/**
	 * Get available executor space (queue + threads). 
	 * Client should NEVER attempt to schedule more tasks than available otherwise it risks for denials/exceptions
	 */
	int available();
	
	/**
	 * Submit a task to be executed as soon as possible
	 * @param @notnull task
	 */
	void submit(Runnable task);
	
	/**
	 * Initiate shutdown. No new tasks will be run, but tasks that are already running will not be aborted.
	 * Returns true if all tasks are shut down after waitTime
	 * 
	 * @param @notnull waitTime wait for this time for tasks to finish. Actual wait time may be less if tasks shut down
	 *                          earlier
	 */
	boolean shutdown(Interval waitTime) throws InterruptedException;
	
	/**
	 * Abort all running tasks and shut down the Executor immediately
	 */
	default void abort() throws Exception {
		close();
	}
}
