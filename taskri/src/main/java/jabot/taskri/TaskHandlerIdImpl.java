package jabot.taskri;

import jabot.taskapi.InvalidIdException;
import jabot.taskapi.TaskHandler;
import jabot.taskapi.TaskHandlerId;
import jabot.taskapi.TaskMemento;
import jabot.taskapi.TaskParams;

class TaskHandlerIdImpl implements TaskHandlerId {
	private final TaskerRI owner;
	private final TaskHandler<?, ?> handler;
	private final String moniker;
	
	public TaskHandlerIdImpl(final TaskerRI owner, final TaskHandler<?, ?> handler) {
		this.owner = owner;
		this.handler = handler;
		this.moniker = handler.getClass().getName();
	}
	
	public static TaskHandlerIdImpl recast(final TaskerRI owner, final TaskHandlerId iface) {
		if (iface instanceof TaskHandlerIdImpl) {
			final TaskHandlerIdImpl ret = (TaskHandlerIdImpl) iface;
			if (ret.owner == owner) {
				return ret;
			}
		}
		throw new InvalidIdException(iface.getHandlerClass());
	}

	@Override
	public String getHandlerClass() {
		return moniker;
	}
	
	@Override
	public String toString() {
		return moniker;
	}
	
	TaskHandler<?, ?> getHandler() {
		return handler;
	}
	
	String marshallParams(final TaskParams params) {
		return bypassGenericsAndRecastToBasic().marshallParams(params);
	}
	
	@SuppressWarnings("unchecked")
	<P extends TaskParams> P unmarshallParams(final String marshalled) {
		return (P)bypassGenericsAndRecastToBasic().unmarshallParams(marshalled);
	}
	
	String marshallMemento(final TaskMemento memento) {
		return bypassGenericsAndRecastToBasic().marshallMemento(memento);
	}
	
	@SuppressWarnings("unchecked")
	<M extends TaskMemento> M unmarshallMemento(final String marshalled) {
		return (M)bypassGenericsAndRecastToBasic().unmarshallMemento(marshalled);
	}
	
	@SuppressWarnings("unchecked")
	private TaskHandler<TaskParams, TaskMemento> bypassGenericsAndRecastToBasic() {
		return (TaskHandler<TaskParams, TaskMemento>) handler;
	}
}
