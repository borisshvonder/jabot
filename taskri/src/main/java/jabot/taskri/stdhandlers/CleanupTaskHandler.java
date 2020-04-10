package jabot.taskri.stdhandlers;

import java.io.IOException;

import org.apache.commons.lang3.Validate;

import jabot.common.surge.Surge;
import jabot.common.types.Interval;
import jabot.common.types.Timestamp;
import jabot.marshall.Marshall;
import jabot.taskapi.Progress;
import jabot.taskapi.Schedule;
import jabot.taskapi.TaskContext;
import jabot.taskapi.TaskHandler;
import jabot.taskapi.TaskMemento;
import jabot.taskapi.TaskParams;
import jabot.taskapi.Tasker;
import jabot.taskri.store.Task;
import jabot.taskri.store.TaskStore;
import jabot.taskri.store.TaskStore.TaskRecord;

/**
 * Cleans up 'Once' tasks that has run already
 * @author bpasko
 *
 */
public class CleanupTaskHandler implements TaskHandler<CleanupTaskHandler.Params, TaskMemento> {
	private TaskStore store;

	@Override
	public void handle(final TaskContext<Params, TaskMemento> ctx) throws IOException {
		final Params params = ctx.getParams();
		final Interval olderThan = params.getOlderThan();
		Validate.notNull(olderThan, "olderThan cannot be null");
		
		final Tasker tasker = ctx.getTasker();
		final Timestamp now = Timestamp.now();
		
		Progress progress = ctx.getProgress();
		try(final Surge<TaskRecord> remove = store.listScheduledToNeverRun()) {
			TaskRecord task = remove.next();
			while (task != null) {
				progress = progress.addTotal(1);
				if (shouldBeRemoved(now, task.getTask(), olderThan)) {
					tasker.removeTask(task.getMoniker());
					progress = progress.addCurrent(1);
				}
				task = remove.next();
				ctx.setProgress(progress);
			}
		}
	}
	
	private boolean shouldBeRemoved(final Timestamp now, final Task task, final Interval olderThan) {
		if (task.getSchedule().getType() != Schedule.Type.Once) {
			return false;
		}
		if (task.getLastSuccess() == null) {
			return false;
		}
		final Interval elapsed = now.since(task.getLastSuccess());
		if (elapsed.compareTo(olderThan)<0) {
			return false;
		}
		return true;
	}

	@Override
	public String marshallParams(final Params params) {
		return Marshall.get().toJson(params);
	}

	@Override
	public Params unmarshallParams(final String marshalled) {
		return Marshall.get().fromJson(marshalled, Params.class);
	}

	@Override
	public String marshallMemento(TaskMemento memento) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TaskMemento unmarshallMemento(String marshalled) {
		throw new UnsupportedOperationException();
	}

	public TaskStore getStore() {
		return store;
	}

	public void setStore(TaskStore store) {
		this.store = store;
	}
	
	public static class Params implements TaskParams {
		private Interval olderThan;

		public Interval getOlderThan() {
			return olderThan;
		}

		public void setOlderThan(Interval olderThan) {
			this.olderThan = olderThan;
		}
	}
	
}
