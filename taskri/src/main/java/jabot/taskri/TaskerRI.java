package jabot.taskri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import jabot.common.surge.Surge;
import jabot.common.surge.Surgeon;
import jabot.common.types.Interval;
import jabot.common.types.Timestamp;
import jabot.taskapi.Progress;
import jabot.taskapi.Schedule;
import jabot.taskapi.SimpleErrorInfo;
import jabot.taskapi.TaskState;
import jabot.taskapi.TaskAlreadyExistsException;
import jabot.taskapi.TaskHandler;
import jabot.taskapi.TaskHandlerAlreadyRegisteredException;
import jabot.taskapi.TaskHandlerId;
import jabot.taskapi.TaskId;
import jabot.taskapi.TaskMemento;
import jabot.taskapi.TaskParams;
import jabot.taskapi.Tasker;
import jabot.taskri.exec.Executor;
import jabot.taskri.store.Task;
import jabot.taskri.store.TaskStore;
import jabot.taskri.store.TaskStore.TaskRecord;
import javolution.util.FastMap;

/**
 * Reference implementation of {@link Tasker} that relies on {@link TaskStore} to 
 * schedule/store tasks and employs {@link Executor} to execute them.
 */
public class TaskerRI implements Tasker {
	// IMPLEMENTATION DETAILS
	//
	// impl.001 Distributed design
	//          This implementation is single-node for simplicity reasons. It can be scaled to full distributed 
	//          implemenentation pretty easily though
	private final Map<String, TaskHandlerIdImpl> handlers = new FastMap<String, TaskHandlerIdImpl>().atomic();
	private final Boss boss;

	public TaskerRI() {
		this.boss = new Boss(this);
	}

	public TaskStore getStore() {
		return boss.getStore();
	}

	/**
	 * This method should be called BEFORE {@link #startup()}
	 */
	public void setStore(final TaskStore store) {
		this.boss.setStore(store);
	}
	
	public Executor getExecutor() {
		return boss.getExecutor();
	}

	/**
	 * This method should be called BEFORE {@link #startup()}
	 */
	public void setExecutor(final Executor executor) {
		boss.setExecutor(executor);
	}
	
	public void setThrottleIdle(final Interval throttleIdle) {
		boss.setThrottleIdle(throttleIdle);
	}

	@Override
	public synchronized void startup() {
		boss.startup();
	}

	@Override
	public void pause() {
		boss.pause();
	}

	@Override
	public void resume() {
		boss.resume();
	}

	@Override
	public synchronized void shutdown() throws Exception {
		boss.shutdown();
	}

	@Override
	public TaskHandlerId registerHandler(final TaskHandler<?, ?> handler) {
		Validate.notNull(handler, "handler cannot be null");
		
		final TaskHandlerIdImpl id = new TaskHandlerIdImpl(this, handler);
		handlers.compute(handler.getClass().getName(), (k, v) -> registerHandlerInternal(k, v, id));
		
		final Collection<String> copyToAvoidSynchronization = new ArrayList<>(handlers.keySet());
		boss.setHandlers(Collections.unmodifiableCollection(copyToAvoidSynchronization));
		return id;
	}
	
	private static TaskHandlerIdImpl registerHandlerInternal(final String moniker, 
			final TaskHandlerIdImpl existing,
			final TaskHandlerIdImpl newHandler
	) {
		if (existing != null) {
			throw new TaskHandlerAlreadyRegisteredException(moniker);
		} else {
			return newHandler;
		}
	}

	@Override
	public TaskHandlerId findHandler(final String handlerMoniker) {
		Validate.notNull(handlerMoniker, "handlerMoniker cannot be null");
		
		return findHandlerInternal(handlerMoniker);
	}
	
	TaskHandlerIdImpl findHandlerInternal(final String handlerMoniker) {
		return handlers.get(handlerMoniker);
	}

	@Override
	public TaskId createTask(final String moniker, 
			final TaskHandlerId handler, 
			final Schedule schedule, 
			final Schedule whenFailed,
			final TaskParams params, 
			final TaskMemento memento) 
	{
		Validate.notNull(moniker, "moniker cannot be null");
		Validate.notNull(handler, "handler cannot be null");
		Validate.notNull(schedule, "schedule cannot be null");
		
		final TaskHandlerIdImpl handlerImpl = TaskHandlerIdImpl.recast(this, handler);

		final Task task = new Task();
		task.setState(TaskState.SCHEDULED);
		task.setMoniker(moniker);
		task.setSchedule(schedule);
		task.setFailedSchedule(whenFailed);
		task.setHandlerMoniker(handlerImpl.getHandlerClass());
		task.setParams(params == null ? null : handlerImpl.marshallParams(params));
		task.setMemento(memento == null ? null : handlerImpl.marshallMemento(memento));
		task.setNextRun(schedule.nextRun(null, null));
		task.setProgress(new Progress(0, 0, 0, 0));
		task.freeze();
		
		final TaskRecord state = getStore().newTask(task);
		if (state == null) {
			throw new TaskAlreadyExistsException(moniker);
		}
		return new TaskIdImpl(this, state);
	}

	@Override
	public void removeTask(final String moniker) {
		Validate.notNull(moniker, "moniker cannot be null");
		getStore().remove(moniker);
	}

	@Override
	public TaskId findTask(final String moniker) {
		Validate.notNull(moniker, "moniker cannot be null");
		
		final TaskRecord state = getStore().find(moniker);
		return state == null ? null : new TaskIdImpl(this, state);
	}
	
	@Override
	public TaskId refresh(final TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		final TaskRecord newState = getStore().reload(impl.getState());
		return newState == null ? null : new TaskIdImpl(this, newState);
	}

	@Override
	public TaskState getState(TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		return impl.getState().getTask().getState();
	}

	@Override
	public Schedule getSchedule(final TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		return impl.getState().getTask().getSchedule();
	}

	@Override
	public void setSchedule(final TaskId task, final Schedule newSchedule) {
		Validate.notNull(task, "task cannot be null");
		Validate.notNull(newSchedule, "newSchedule cannot be null");
		
		mutateUnconditionally(task, tsk -> {
			tsk.setSchedule(newSchedule);
			tsk.setNextRun(newSchedule.nextRun(tsk.getLastRunStart(), tsk.getLastRunFinish()));
		});
	}
	
	@Override
	public Schedule getFailedSchedule(final TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		return impl.getState().getTask().getFailedSchedule();
	}

	@Override
	public void setFailedSchedule(final TaskId task, final Schedule newSchedule) {
		Validate.notNull(task, "task cannot be null");
		Validate.notNull(newSchedule, "newSchedule cannot be null");
		
		mutateUnconditionally(task, tsk -> {
			tsk.setFailedSchedule(newSchedule);
		});
	}

	@Override
	public Timestamp getNextRun(final TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		return impl.getState().getTask().getNextRun();
	}

	@Override
	public Timestamp getLastRun(final TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		return impl.getState().getTask().getLastRunFinish();
	}

	@Override
	public Timestamp getLastSuccessfulRun(final TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		return impl.getState().getTask().getLastSuccess();
	}

	@Override
	public ErrorInfo getLastError(final TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		final Task asTask = impl.getState().getTask();
		final String errorClass = asTask.getLastErrorClass();
		return errorClass == null ? null : new SimpleErrorInfo(errorClass, asTask.getLastErrorMessage());
	}

	@Override
	public String getParams(final TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		return impl.getState().getTask().getParams();
	}

	@Override
	public String getMemento(final TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		return impl.getState().getTask().getMemento();
	}

	@Override
	public void setMemento(final TaskId task, final TaskMemento memento) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		final String handlerMoniker = impl.getHandlerMoniker();
		final TaskHandlerIdImpl handler = handlers.get(handlerMoniker);
		Validate.notNull(handler, "There is no {} handler registered for task {}", handlerMoniker, task);
		final String mementoAsString = handler.marshallMemento(memento);
		
		mutateUnconditionally(task, tsk -> {
			tsk.setMemento(mementoAsString);
		});
	}

	@Override
	public Progress getProgress(final TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		return impl.getState().getTask().getProgress();
	}

	@Override
	public Collection<TaskHandlerId> allHandlers() {
		final List<TaskHandlerId> ret = new ArrayList<>(handlers.size());
		for (final TaskHandlerIdImpl impl : handlers.values()) {
			ret.add(impl);
		}
		return Collections.unmodifiableCollection(ret);
	}

	@Override
	public Surge<TaskId> allTasks(final Collection<String> forHandlers) {
		return new Surgeon<TaskRecord>(getStore().listScheduledToNeverRun())
				.append(getStore().listScheduled(forHandlers, null, null))
				.map(state -> (TaskId)new TaskIdImpl(this, state))
				.get();
	}

	interface TaskMutator {
		void mutate(Task task);
	}
	
	void mutateUnconditionally(final TaskId task, final TaskMutator mutator) {
		final TaskIdImpl impl = TaskIdImpl.recast(this, task);
		TaskRecord state = impl.getState();
		while (state != null) {
			final Task tsk = state.getTask().defrost();
			mutator.mutate(tsk);
			final TaskRecord newState = getStore().cas(state, tsk);
			if (newState != null) {
				return;
			}
			state = getStore().reload(state);
		}
	}
	
	void remove(final TaskId task) {
		getStore().remove(task.getMoniker());
	}

}
