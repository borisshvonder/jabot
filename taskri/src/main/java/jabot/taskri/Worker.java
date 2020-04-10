package jabot.taskri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.common.cache.ExpiringCache;
import jabot.common.types.Interval;
import jabot.common.types.Timestamp;
import jabot.taskapi.Progress;
import jabot.taskapi.Schedule;
import jabot.taskapi.TaskState;
import jabot.taskapi.TaskContext;
import jabot.taskapi.TaskHandler;
import jabot.taskapi.TaskMemento;
import jabot.taskapi.TaskParams;
import jabot.taskapi.Tasker;
import jabot.taskri.store.Task;

public class Worker<P extends TaskParams, M extends TaskMemento> implements Runnable, TaskContext<P, M> {
	private static final Logger LOG = LoggerFactory.getLogger(Worker.class);
	private final ExpiringCache<Boolean> aborted = new ExpiringCache<>(Interval.SECOND);
	private final ExpiringCache<Object> progressReport = new ExpiringCache<>(Interval.SECOND);
	private final TaskerRI tasker;
	private final TaskHandlerIdImpl handlerId;
	private final TaskIdImpl id;
	private P params;
	private M memento;
	private Progress progress;

	public Worker(TaskerRI tasker, TaskHandlerIdImpl handlerId, TaskIdImpl id) {
		this.tasker = tasker;
		this.handlerId = handlerId;
		this.id = id;
	}
	
	public void claim() {
		tasker.mutateUnconditionally(id, tsk -> tsk.setState(TaskState.RUNNING));
	}

	public void unclaim() {
		tasker.mutateUnconditionally(id, tsk -> tsk.setState(TaskState.SCHEDULED));
	}

	@Override
	public void run() {
		final Task task = id.getState().getTask();
		final String marshalledParams = task.getParams();
		this.params = marshalledParams == null ? null : handlerId.unmarshallParams(task.getParams());
		final String marshalledMemento = task.getMemento();
		this.memento = marshalledMemento==null ? null : handlerId.unmarshallMemento(marshalledMemento);
		this.progress = task.getProgress();
		if (this.progress == null) {
			this.progress = new Progress(0, 0, 0, 0);
		}
		
		runHandler();
	}
	
	@Override
	public Tasker getTasker() {
		return tasker;
	}

	@Override
	public boolean isAborted() {
		Boolean ret = aborted.get();
		if (ret == null) {
			ret = tasker.refresh(id) == null;
			aborted.set(ret);
		}
		return ret;
	}

	@Override
	public P getParams() {
		return params;
	}

	@Override
	public M getMemento() {
		return memento;
	}

	@Override
	public void setMemento(final M newMemento) {
		tasker.setMemento(id, newMemento);
		this.memento = newMemento;
	}

	@Override
	public Progress getProgress() {
		return progress;
	}

	@Override
	public void setProgress(final Progress progress) {
		this.progress = progress;
		if (progressReport.isExpired()) {
			tasker.mutateUnconditionally(id, tsk -> tsk.setProgress(progress));
			progressReport.set(null);
		}
	}

	private void runHandler() {
		@SuppressWarnings("unchecked")
		final TaskHandler<P, M> handler = (TaskHandler<P, M>)handlerId.getHandler();
		
		final Timestamp started = Timestamp.now();
		try {
			LOG.trace("{}: running");
			handler.handle(this);
			LOG.trace("{}: success");
			tasker.mutateUnconditionally(id, tsk -> taskSuccess(tsk, started));
		} catch (final Exception ex) {
			LOG.trace("{}: failure", ex);
			tasker.mutateUnconditionally(id, tsk -> failTask(tsk, started, ex.getClass().getName(), ex.getMessage()));
		}
	}
	
	private void taskSuccess(final Task task, final Timestamp started) {
		schedule(task, started, task.getSchedule());
		task.setLastSuccess(task.getLastRunFinish());
	}

	private void failTask(final Task task, final Timestamp started, final String errorClass, final String errorMessage) {
		Schedule failSchedule = task.getFailedSchedule();
		if (failSchedule == null) {
			failSchedule = task.getSchedule();
		}
		task.setLastErrorClass(errorClass);
		task.setLastErrorMessage(errorMessage);
		schedule(task, started, failSchedule);
	}

	private void schedule(final Task task, final Timestamp started, final Schedule schedule) {
		task.setLastRunStart(started);
		task.setLastRunFinish(Timestamp.now());
		if (schedule.getType() == Schedule.Type.Once) {
			task.setNextRun(null);
		} else {
			task.setNextRun(schedule.nextRun(task.getLastRunStart(), task.getLastRunFinish()));
		}
		task.setState(TaskState.SCHEDULED);
		task.setProgress(progress);
	}
}
