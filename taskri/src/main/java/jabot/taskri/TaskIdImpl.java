package jabot.taskri;

import jabot.taskapi.InvalidTaskIdException;
import jabot.taskapi.TaskId;
import jabot.taskri.store.TaskStore.TaskRecord;

class TaskIdImpl implements TaskId {
	private final TaskerRI owner;
	private final TaskRecord state;
	private final String moniker;
	
	public TaskIdImpl(final TaskerRI owner, final TaskRecord state) {
		this.owner = owner;
		this.state = state;
		this.moniker = state.getMoniker();
	}
	
	public static TaskIdImpl recast(final TaskerRI owner, final TaskId iface) {
		if (iface instanceof TaskIdImpl) {
			final TaskIdImpl ret = (TaskIdImpl) iface;
			if (ret.owner == owner) {
				return ret;
			}
		}
		throw new InvalidTaskIdException(iface.getMoniker());
	}

	@Override
	public String getMoniker() {
		return moniker;
	}

	@Override
	public String getHandlerMoniker() {
		return state.getTask().getHandlerMoniker();
	}
	
	@Override
	public String toString() {
		return moniker;
	}

	TaskRecord getState() {
		return state;
	}
}
