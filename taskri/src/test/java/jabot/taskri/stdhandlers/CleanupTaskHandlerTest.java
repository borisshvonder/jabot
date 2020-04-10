package jabot.taskri.stdhandlers;

import org.apache.commons.lang3.Validate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jabot.common.types.Interval;
import jabot.taskapi.Schedule;
import jabot.taskapi.TaskHandlerId;
import jabot.taskapi.TaskId;
import jabot.taskapi.TaskMemento;
import jabot.taskapi.Tasker;
import jabot.taskapi.tests.DummyTestTaskHandler;
import jabot.taskri.TaskerRI;
import jabot.taskri.stdhandlers.CleanupTaskHandler.Params;

public class CleanupTaskHandlerTest {
	private CleanupTaskHandler fixture;
	private Tasker tasker;
	private TaskHandlerId handler, dummyHandler;
	private Params sampleParams;
	private DummyTestTaskHandler.Params dummyParams;
	
	@Before
	public synchronized void setUp() {
		final TaskerRI impl = new TaskerRI();
		impl.setThrottleIdle(Interval.MILLISECOND);
		tasker = impl;

		fixture = new CleanupTaskHandler();
		fixture.setStore(impl.getStore());
		Assert.assertSame(impl.getStore(), fixture.getStore());
		
		final DummyTestTaskHandler dummy = new DummyTestTaskHandler();
		dummyHandler = impl.registerHandler(dummy);
		
		handler = impl.registerHandler(fixture);
		
		sampleParams = new Params();
		dummyParams = new DummyTestTaskHandler.Params();
		tasker.startup();
	}

	@After
	public synchronized void killAllTasks() throws Exception {
		tasker.shutdown();
	}
	
	@Test
	public void test_dry_run() throws InterruptedException {
		sampleParams.setOlderThan(Interval.YEAR);
		runOnce(sampleParams);
	}
	
	@Test
	public void test_remove_oneShot_task() throws InterruptedException {
		final TaskId taskToRemove = tasker.createTask("testRemoveOneTask", dummyHandler, Schedule.once(), dummyParams);
		waitForLastRun(taskToRemove);
		
		sampleParams.setOlderThan(Interval.ZERO);
		runOnce(sampleParams);
		
		Assert.assertNull(tasker.refresh(taskToRemove));
	}
	
	@Test
	public void test_should_not_remove_non_oneshot_task() throws InterruptedException {
		final TaskId dontRemove = tasker.createTask("testRemoveOneTask", dummyHandler, Schedule.NEVER, dummyParams);
		
		sampleParams.setOlderThan(Interval.ZERO);
		runOnce(sampleParams);
		
		Assert.assertNotNull(tasker.refresh(dontRemove));
	}
	
	@Test
	public void test_should_not_remove_non_expired_task() throws InterruptedException {
		final TaskId dontRemove = tasker.createTask("testRemoveOneTask", dummyHandler, Schedule.once(), dummyParams);
		waitForLastRun(dontRemove);
		
		sampleParams.setOlderThan(Interval.YEAR);
		runOnce(sampleParams);
		
		Assert.assertNotNull(tasker.refresh(dontRemove));
	}
	
	@Test
	public void test_should_not_remove_failed_task() throws InterruptedException {
		dummyParams.value = DummyTestTaskHandler.THROW_ERROR;
		
		TaskId dontRemove = tasker.createTask("testRemoveOneTask", dummyHandler, Schedule.once(), Schedule.once(), 
				dummyParams, null);
		waitForLastRun(dontRemove);
		dontRemove = tasker.refresh(dontRemove);
		Assert.assertNotNull(tasker.getLastError(dontRemove));
		
		sampleParams.setOlderThan(Interval.ZERO);
		runOnce(sampleParams);
		
		Assert.assertNotNull(tasker.refresh(dontRemove));
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void test_does_not_support_memento_marshalling() throws InterruptedException {
		fixture.marshallMemento(new TaskMemento(){});
	}

	@Test(expected=UnsupportedOperationException.class)
	public void test_does_not_support_memento_unmarshalling() throws InterruptedException {
		fixture.unmarshallMemento("");
	}

	private void runOnce(final Params params) throws InterruptedException {
		TaskId task = tasker.createTask("runOnce", handler, Schedule.delay(Interval.YEAR), params);
		task = waitForLastRun(task);
	}

	private TaskId waitForLastRun(final TaskId task) throws InterruptedException {
		Validate.notNull(task);
		
		TaskId tsk = task;
		while(tasker.getLastRun(tsk) == null) {
			Thread.sleep(100L);
			tsk = tasker.refresh(tsk);
		}
		return task;
	}

}
