package jabot.taskri;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.common.surge.Surge;
import jabot.common.types.Interval;
import jabot.common.types.Timestamp;
import jabot.pools.ThreadPool;
import jabot.taskapi.TaskState;
import jabot.taskri.exec.Executor;
import jabot.taskri.exec.ThreadPoolAdapter;
import jabot.taskri.store.RamTaskStore;
import jabot.taskri.store.Task;
import jabot.taskri.store.TaskStore;
import jabot.taskri.store.TaskStore.TaskRecord;

/**
 * Runs and manages workers
 */
class Boss implements Runnable {
	private enum BossState { INIT, STARTED, PAUSED, DONE }
	private final AtomicReference<BossState> state = new AtomicReference<>(BossState.INIT);
	
	private static final Logger LOG = LoggerFactory.getLogger(Boss.class);
	private static final List<TaskState> SCHEDULED_ONLY = Arrays.asList(TaskState.SCHEDULED);
	private final TaskerRI tasker;
	private TaskStore store;
	private volatile Collection<String> handlers = Collections.emptyList();
	
	/** Throttle execution when idle to conserve resources */
	private Interval throttleIdle = Interval.SECOND;
	
	// TODO: Add per-handler executor mapping
	private Executor executor;

	public Boss(final TaskerRI tasker) {
		Validate.notNull(tasker, "tasker cannot be null");
		
		this.tasker = tasker;
	}
	
	public TaskStore getStore() {
		if (store == null) {
			// use default in-memory implementation
			store = new RamTaskStore();
		}
		return store;
	}

	public void setStore(TaskStore store) {
		this.store = store;
	}
	
	public Executor getExecutor() {
		if (executor == null) {
			executor = new ThreadPoolAdapter(new ThreadPool(2, 1));
		}
		return executor;
	}
	
	public void setThrottleIdle(final Interval throttleIdle) {
		Validate.notNull(throttleIdle, "throttleIdle cannot be null");
		
		this.throttleIdle = throttleIdle;
	}

	/**
	 * This method should be called BEFORE {@link #startup()}
	 */
	public void setExecutor(final Executor executor) {
		this.executor = executor;
	}
	
	public void setHandlers(final Collection<String> handlers) {
		this.handlers = handlers;
	}

	public static Logger getLog() {
		return LOG;
	}

	synchronized void startup() {
		Validate.isTrue(getExecutor().available()>=3, "Executor space should be at least 3");
		
		if (state.get() != BossState.INIT) {
			throw new IllegalStateException("state="+state);
		}
		scheduleBoss(); // Schedule first run
		state.set(BossState.STARTED);
	}
	
	void pause() {
		state.compareAndSet(BossState.STARTED, BossState.PAUSED);
	}

	void resume() {
		state.compareAndSet(BossState.PAUSED, BossState.STARTED);
	}

	void shutdown() throws Exception {
		state.set(BossState.DONE);
		if (executor != null) {
			executor.shutdown(Interval.TEN_SECONDS);
			executor.close();
		}
	}
	
	@Override
	public void run() {
		try {
			LOG.trace("Running Boss");
			final ScheduleResult result = new ScheduleResult();
			if (state.get() == BossState.STARTED) {
				runScheduler(result);
			}
			// While this is not 100% effective when boss is paused, cause it will still run periodically, this is 
			// 'good enough' to conserve resources while not making scheduling over-complicated.
			// The downside, however, will be that 1 thread will always be blocked during idle, thus at least 2 threads
			// are required.
			throttleIdle(result);
		} finally {
			if (state.get() != BossState.DONE) {
				scheduleBoss(); // Schedule next run
			}
		}
		LOG.trace("Boss done : Ok");
	}

	private void throttleIdle(final ScheduleResult result) {
		if (result.tasksScheduled == 0) {
			// No tasks were scheduled at all, throttle by waiting
			try {
				Thread.sleep(throttleIdle.asMillis());
			} catch (final InterruptedException ex) {
				Thread.interrupted();
				LOG.warn("Interrupted while idle waiting");
			}
		}
	}
	
	private void runScheduler(final ScheduleResult result) {
		final int toSchedule = executor.available()-1; // Reserve spot for Boss.
		if (toSchedule > 0) {
			scheduleTasks(toSchedule, result);
		}
	}

	private void scheduleTasks(final int toSchedule, final ScheduleResult result) {
		final Timestamp now = Timestamp.now();
		try {
			try(final Surge<TaskStore.TaskRecord> tasks = getStore().listScheduled(handlers, SCHEDULED_ONLY, now)) {
				int left = toSchedule;
				do {
					if (scheduleTask(tasks)) {
						result.tasksScheduled++;
					} else {
						// No more tasks to schedule
						left = 0;
					}
				} while(--left>0);
			}
		} catch (Exception ex) {
			LOG.warn("Exception in Boss", ex);
		}
	}
	
	private void scheduleBoss() {
		getExecutor().submit(this);
	}

	private boolean scheduleTask(final Surge<TaskRecord> tasks) {
		final TaskRecord state = tasks.next();
		if (state == null) {
			return false;
		}
		final Task task = state.getTask();
		final TaskHandlerIdImpl handlerId = tasker.findHandlerInternal(task.getHandlerMoniker());
		if (handlerId == null) {
			LOG.warn("TaskStore returned a task with handler {} (forHandlers={}) but this handler no longer registered.",
					task.getHandlerMoniker(), handlers);
			return false;
		}
		final TaskIdImpl taskId = new TaskIdImpl(tasker, state);
		
		LOG.trace("Starting task {}", taskId);
		final Worker<?, ?> worker = new Worker<>(tasker, handlerId, taskId);
		try {
			worker.claim();
			LOG.trace("Claimed task {}", taskId);
			getExecutor().submit(worker);
			LOG.trace("Worker submitted for task {}", taskId);
			return true;
		} catch (final Exception ex) {
			worker.unclaim();
			LOG.error("Exception in boss while running task {}", state.getMoniker(), ex);
		} catch (final Error err) {
			worker.unclaim();
			LOG.error("ERROR in boss while running task {}", state.getMoniker(), err);
			throw err;
		}
		return false;
	}

	private static final class ScheduleResult {
		int tasksScheduled;
	}
}
