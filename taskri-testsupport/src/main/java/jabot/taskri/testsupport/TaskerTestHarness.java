package jabot.taskri.testsupport;

import java.util.Properties;

import org.apache.commons.lang3.Validate;

import jabot.common.props.PropsConfig;
import jabot.common.types.Interval;
import jabot.taskapi.Schedule;
import jabot.taskapi.TaskHandler;
import jabot.taskapi.TaskHandlerId;
import jabot.taskapi.TaskId;
import jabot.taskapi.TaskMemento;
import jabot.taskapi.TaskParams;
import jabot.taskri.TaskerRI;
import jabot.taskri.store.TaskStore;

/**
 * Test harness which provides an instance of Tasker and some useful test methods
 */
public class TaskerTestHarness {
	private final TaskerRI tasker;
	private final PropsConfig propsConfig = new PropsConfig();
	
	public TaskerTestHarness() {
		this.tasker = new TaskerRI();
		tasker.setThrottleIdle(Interval.MILLISECOND);
	}

	public TaskerRI getTasker() {
		return tasker;
	}
	
	public void startup() {
		tasker.startup();
	}
	
	public void shutdown() throws Exception {
		tasker.shutdown();
	}

	public TaskStore getStore() {
		return tasker.getStore();
	}
	
	public Properties getProps() {
		return propsConfig.asProps();
	}

	public PropsConfig getPropsConfig() {
		return propsConfig;
	}

	public void runOnce(final TaskHandlerId handler, final TaskParams params) throws InterruptedException {
		runOnce(handler, params, null);
	}

	public void runOnce(final TaskHandlerId handler, final TaskParams params, final TaskMemento memento) 
			throws InterruptedException 
	{
		TaskId task = tasker.createTask("runOnce", handler, Schedule.delay(Interval.YEAR), null, params, memento);
		waitForLastRun(task);
	}

	public void waitForLastRun(final TaskId task) throws InterruptedException {
		Validate.notNull(task);
		
		TaskId tsk = task;
		while(tasker.getLastRun(tsk) == null) {
			Thread.sleep(100L);
			tsk = tasker.refresh(tsk);
		}
	}
	
	public void assertParamsMarshallingSupported(final TaskHandler<?, ?> handler, TaskParams params) {

		@SuppressWarnings("unchecked")
		final TaskHandler<TaskParams, ?> recasted = (TaskHandler<TaskParams, ?>) handler;

		final String marshalled = recasted.marshallParams(params);
		final TaskParams unmarshalled = handler.unmarshallParams(marshalled);
		if (unmarshalled == null) {
			throw new NullPointerException();
		}
	}

	public void assertMementosNotSupported(final TaskHandler<?, ?> handler) {
		
		@SuppressWarnings("unchecked")
		final TaskHandler<?, TaskMemento> recasted = (TaskHandler<?, TaskMemento>) handler;
		
		try {
			recasted.marshallMemento(new TaskMemento(){});
			throw new AssertionError("Does not throw UnsupportedOperationException on marshallMemento");
		} catch (final UnsupportedOperationException ex) {
			// Ignore
		}
		
		try {
			recasted.unmarshallMemento("");
			throw new AssertionError("Does not throw UnsupportedOperationException on unmarshallMemento");
		} catch (final UnsupportedOperationException ex) {
			// Ignore
		}
	}

	public void assertMementoMarshallingSupported(final TaskHandler<?, ?> handler, TaskMemento memento) {

		@SuppressWarnings("unchecked")
		final TaskHandler<?, TaskMemento> recasted = (TaskHandler<?, TaskMemento>) handler;

		final String marshalled = recasted.marshallMemento(memento);
		final TaskMemento unmarshalled = handler.unmarshallMemento(marshalled);
		if (unmarshalled == null) {
			throw new NullPointerException();
		}
	}

}
