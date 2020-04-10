package jabot.taskapi.tests;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jabot.common.surge.Surge;
import jabot.common.types.Interval;
import jabot.common.types.Timestamp;
import jabot.taskapi.Progress;
import jabot.taskapi.Schedule;
import jabot.taskapi.TaskState;
import jabot.taskapi.TaskAlreadyExistsException;
import jabot.taskapi.TaskHandlerAlreadyRegisteredException;
import jabot.taskapi.TaskHandlerId;
import jabot.taskapi.TaskId;
import jabot.taskapi.Tasker;
import jabot.taskapi.Tasker.ErrorInfo;

/**
 * Functional tests for {@link Tasker}
 */
public abstract class TaskerTestBase {
	protected abstract Tasker createTasker();
	protected abstract void dispose(Tasker tasker);
	
	private static final Schedule ONE_SHOT = Schedule.once();
	private static final Schedule FIXED_DELAY = Schedule.delay(Interval.YEAR);
	private Tasker fixture;
	private TaskHandlerId handler;
	private DummyTestTaskHandler.Params params;
	private DummyTestTaskHandler.Memento memento;
	
	@Before
	public void setUp() {
		fixture = createTasker();

		handler = fixture.registerHandler(new DummyTestTaskHandler());
		Assert.assertNotNull(handler);
		Assert.assertEquals(DummyTestTaskHandler.class.getName(), handler.getHandlerClass());
		
		params = new DummyTestTaskHandler.Params();
		memento = new DummyTestTaskHandler.Memento();
	}
	
	@After
	public void tearDown() throws Exception {
		fixture.shutdown();
		dispose(fixture);
	}
	
	@Test(expected=TaskHandlerAlreadyRegisteredException.class)
	public void test_cannot_register_same_handler_twice() {
		fixture.registerHandler(new DummyTestTaskHandler());
	}
	
	@Test
	public void test_findHandler_missing_handler_will_not_be_found() {
		Assert.assertNull(fixture.findHandler("handler1"));
	}
	
	@Test
	public void test_findHandler_existing_handler_will_be_found() {
		final TaskHandlerId lookedUp = fixture.findHandler(DummyTestTaskHandler.class.getName());
		Assert.assertNotNull(lookedUp);
		Assert.assertEquals(DummyTestTaskHandler.class.getName(), lookedUp.getHandlerClass());
	}
	
	@Test
	public void test_find_missing_job() {
		Assert.assertNull(fixture.findTask("missing"));
	}
	
	@Test
	public void test_allDummyTestTaskHandlers() {
		final Collection<TaskHandlerId> allDummyTestTaskHandlers = fixture.allHandlers();
		Assert.assertNotNull(allDummyTestTaskHandlers);
		Assert.assertEquals(1, allDummyTestTaskHandlers.size());
		Assert.assertEquals(DummyTestTaskHandler.class.getName(), allDummyTestTaskHandlers.iterator().next().getHandlerClass());
	}
	
	@Test
	public void test_allTasks_no_tasks() {
		final Surge<TaskId> tasks = fixture.allTasks();
		Assert.assertNull(tasks.next());
	}

	@Test
	public void test_getState() throws IOException {
		final TaskId id = fixture.createTask("job1", handler, Schedule.NEVER, null);
		Assert.assertEquals(TaskState.SCHEDULED, fixture.getState(id));
	}
	
	@Test
	public void test_allTasks_one_task_scheduled_to_never_run() throws IOException {
		final TaskId id = fixture.createTask("job1", handler, Schedule.NEVER, null);
		try (final Surge<TaskId> tasks = fixture.allTasks()) {
			Assert.assertEquals(id.getMoniker(), tasks.next().getMoniker());
			Assert.assertNull(tasks.next());
		}
	}
	
	@Test
	public void test_allTasks_one_task_scheduled_to_run_once() throws IOException {
		final TaskId id = fixture.createTask("job1", handler, ONE_SHOT, null);
		try(final Surge<TaskId> tasks = fixture.allTasks()) {
			Assert.assertEquals(id.getMoniker(), tasks.next().getMoniker());
			Assert.assertNull(tasks.next());
		}
	}
	
	@Test
	public void test_allTasks_one_task_scheduled_to_never_run_comes_before_other_tasks() {
		final TaskId id1 = fixture.createTask("id1", handler, ONE_SHOT, null);
		final TaskId id2 = fixture.createTask("id2", handler, Schedule.NEVER, null);
		final Surge<TaskId> tasks = fixture.allTasks();
		Assert.assertEquals(id2.getMoniker(), tasks.next().getMoniker());
		Assert.assertEquals(id1.getMoniker(), tasks.next().getMoniker());
		Assert.assertNull(tasks.next());
	}

	@Test
	public void test_allTasks_tasks_ordered_by_nextRun() throws IOException {
		final TaskId id1 = fixture.createTask("id1", handler, schedule(1000L), null);
		final TaskId id2 = fixture.createTask("id2", handler, schedule(500L), null);
		final TaskId id3 = fixture.createTask("id3", handler, schedule(100L), null);
		final TaskId id4 = fixture.createTask("id4", handler, Schedule.NEVER, null);
		try(final Surge<TaskId> tasks = fixture.allTasks()) {
			Assert.assertEquals(id4.getMoniker(), tasks.next().getMoniker());
			Assert.assertEquals(id3.getMoniker(), tasks.next().getMoniker());
			Assert.assertEquals(id2.getMoniker(), tasks.next().getMoniker());
			Assert.assertEquals(id1.getMoniker(), tasks.next().getMoniker());
			Assert.assertNull(tasks.next());
		}
	}

	@Test
	public void test_allTasks_filterByDummyTestTaskHandler() throws IOException {
		final TaskHandlerId handler2 = fixture.registerHandler(new DummyTestTaskHandler(){});
		final TaskId id1 = fixture.createTask("id1", handler, schedule(50L), null);
		final TaskId id2 = fixture.createTask("id2", handler2, schedule(1000L), null);
		
		try(Surge<TaskId> tasks = fixture.allTasks(Arrays.asList())) {
			Assert.assertNull(tasks.next());
		}
		
		try(Surge<TaskId> tasks = fixture.allTasks(Arrays.asList(handler.getHandlerClass()))) {
			Assert.assertEquals(id1.getMoniker(), tasks.next().getMoniker());
			Assert.assertNull(tasks.next());
		}

		try(Surge<TaskId> tasks = fixture.allTasks(Arrays.asList(handler2.getHandlerClass()))) {
			Assert.assertEquals(id2.getMoniker(), tasks.next().getMoniker());
			Assert.assertNull(tasks.next());
		}

		final List<String> handlers = Arrays.asList(handler.getHandlerClass(), handler2.getHandlerClass());
		try(Surge<TaskId> tasks = fixture.allTasks(handlers)) {
			Assert.assertEquals(id1.getMoniker(), tasks.next().getMoniker());
			Assert.assertEquals(id2.getMoniker(), tasks.next().getMoniker());
			Assert.assertNull(tasks.next());
		}
	}

	@Test
	public void test_createJob() {
		final TaskId id = fixture.createTask("job1", handler, Schedule.NEVER, null);
		final TaskId found = fixture.findTask("job1");
		Assert.assertNotNull(found);
		Assert.assertEquals(id.getMoniker(), found.getMoniker());
		Assert.assertEquals(id.getHandlerMoniker(), found.getHandlerMoniker());
	}
	
	@Test
	public void test_cannot_createJob_with_same_moniker() {
		fixture.createTask("job1", handler, Schedule.NEVER, params);
		try {
			fixture.createTask("job1", handler, Schedule.NEVER, params);
			Assert.fail();
		} catch (final TaskAlreadyExistsException ex) {
			// ignore
		}
	}
	
	@Test
	public void test_createJob_with_memento() {
		memento.value = "value1";
		final TaskId id = fixture.createTask("job1", handler, Schedule.NEVER, Schedule.NEVER, params, memento);
		final String mem = fixture.getMemento(id);
		Assert.assertEquals(memento.value, mem);
	}
	
	@Test
	public void test_createJob_will_set_nextRun() {
		final TaskId id = fixture.createTask("job1", handler, ONE_SHOT, params);
		Assert.assertNotNull(fixture.getNextRun(id));
	}
	
	@Test
	public void test_createJob_will_not_set_nextRun_on_NEVER_schedule() {
		final TaskId id = fixture.createTask("job1", handler, Schedule.NEVER, params);
		Assert.assertNull(fixture.getNextRun(id));
	}
	
	@Test
	public void test_setSchedule() {
		final TaskId id = fixture.createTask("job1", handler, Schedule.NEVER, params);
		final Schedule oneShot = new Schedule.Builder().once().build();
		fixture.setSchedule(id, oneShot);
		Assert.assertEquals(Schedule.NEVER, fixture.getSchedule(id));
		Assert.assertEquals(oneShot, fixture.getSchedule(fixture.refresh(id)));
	}
	
	@Test
	public void test_setFailedSchedule() {
		final TaskId id = fixture.createTask("job1", handler, Schedule.NEVER, Schedule.NEVER, params, null);
		final Schedule oneShot = new Schedule.Builder().once().build();
		fixture.setFailedSchedule(id, oneShot);
		Assert.assertEquals(Schedule.NEVER, fixture.getFailedSchedule(id));
		Assert.assertEquals(oneShot, fixture.getFailedSchedule(fixture.refresh(id)));
	}

	@Test
	public void test_removeJob() {
		final TaskId id = fixture.createTask("job1", handler, Schedule.NEVER, params);
		fixture.removeTask(id.getMoniker());
		Assert.assertNull(fixture.findHandler("job1"));
		Assert.assertNull(fixture.refresh(id));
	}
	
	@Test
	public void test_getParams() {
		params.value = "params1";
		final TaskId id = fixture.createTask("job1", handler, Schedule.NEVER, params);
		params.value = "params2";
		Assert.assertEquals("params1", fixture.getParams(id));
	}
	
	@Test
	public void test_setMemento() {
		memento.value = "memento1";
		final TaskId id = fixture.createTask("job1", handler, Schedule.NEVER, Schedule.NEVER, params, memento);
		memento.value = "memento2";
		fixture.setMemento(id, memento);
		Assert.assertEquals("memento1", fixture.getMemento(id));
		Assert.assertEquals("memento2", fixture.getMemento(fixture.refresh(id)));
	}
	
	@Test
	public void test_runOneTask_successful() throws InterruptedException {
		TaskId id = fixture.createTask("job1", handler, FIXED_DELAY, params);
		Assert.assertNull(fixture.getLastRun(id));
		Assert.assertNull(fixture.getLastSuccessfulRun(id));
		
		fixture.startup();
		waitUntilRunOnce(id);

		Assert.assertNull(fixture.getLastRun(id));
		Assert.assertNull(fixture.getLastSuccessfulRun(id));

		id = fixture.refresh(id);
		Assert.assertNotNull(fixture.getLastRun(id));
		Assert.assertNotNull(fixture.getLastSuccessfulRun(id));
	}
	
	@Test
	public void test_one_shot_task_should_be_set_to_never_run_after_success() throws InterruptedException {
		TaskId id = fixture.createTask("job1", handler, ONE_SHOT, params);
		Assert.assertNull(fixture.getLastRun(id));
		Assert.assertNull(fixture.getLastSuccessfulRun(id));
		
		fixture.startup();
		waitUntilRunOnce(id);

		Assert.assertNotNull(fixture.getNextRun(id));

		id = fixture.refresh(id);
		Assert.assertNull(fixture.getNextRun(id));
	}

	@Test
	public void test_runOneTask_failure() throws InterruptedException {
		params.value = DummyTestTaskHandler.THROW_ERROR;
		TaskId id = fixture.createTask("job1", handler, FIXED_DELAY, params);
		Assert.assertNull(fixture.getLastRun(id));
		Assert.assertNull(fixture.getLastSuccessfulRun(id));
		Assert.assertNull(fixture.getLastError(id));
		
		fixture.startup();
		waitUntilRunOnce(id);
		Assert.assertNull(fixture.getLastRun(id));
		Assert.assertNull(fixture.getLastSuccessfulRun(id));
		Assert.assertNull(fixture.getLastError(id));

		
		id = fixture.refresh(id);
		Assert.assertNotNull(fixture.getLastRun(id));
		Assert.assertNull(fixture.getLastSuccessfulRun(id));
		final ErrorInfo info = fixture.getLastError(id);
		Assert.assertNotNull(info);
		Assert.assertEquals(RuntimeException.class.getName(), info.getErrorClass());
		Assert.assertEquals("message", info.getMessage());
	}
	
	@Test
	public void test_oneshot_task_should_not_dissapear() throws InterruptedException {
		TaskId id = fixture.createTask("job1", handler, ONE_SHOT, params);
		Assert.assertNull(fixture.getLastRun(id));
		Assert.assertNull(fixture.getLastSuccessfulRun(id));
		Assert.assertNull(fixture.getLastError(id));
		
		fixture.startup();
		waitUntilRunOnce(id);
		Assert.assertNotNull(fixture.refresh(id));
	}


	@Test
	public void test_getProgress() throws InterruptedException {
		final TaskId id = fixture.createTask("job1", handler, FIXED_DELAY, params);
		Assert.assertEquals(new Progress(0, 0, 0, 0), fixture.getProgress(id));
		
		fixture.startup();
		waitUntilRunOnce(id);
		Assert.assertEquals(new Progress(0, 0, 0, 0), fixture.getProgress(id));
		Assert.assertEquals(new Progress(1, 0, 0, 0), fixture.getProgress(fixture.refresh(id)));
	}
	
	@Test
	public void testPauseAndResume() throws InterruptedException {
		fixture.startup();
		fixture.pause();
		final TaskId id = fixture.createTask("job1", handler, FIXED_DELAY, params);
		Thread.sleep(100L);
		Assert.assertNull(fixture.getLastRun(fixture.refresh(id)));
		fixture.resume();
		waitUntilRunOnce(id);
	}
	
	private void waitUntilRunOnce(final TaskId id) throws InterruptedException {
		TaskId reloaded = fixture.refresh(id);
		while(reloaded != null && fixture.getLastRun(reloaded)==null) {
			Thread.sleep(100L);
			reloaded = fixture.refresh(id);
		}
	}

	private Schedule schedule(final long timestampUTC) {
		return new Schedule.Builder().start(Timestamp.fromUTCMillis(timestampUTC)).once().build();
	}


}
