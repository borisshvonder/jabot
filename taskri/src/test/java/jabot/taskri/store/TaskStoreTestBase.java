package jabot.taskri.store;

import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import jabot.common.surge.Surge;
import jabot.common.types.Timestamp;
import jabot.marshall.Marshall;
import jabot.taskapi.TaskState;
import jabot.taskri.store.TaskStore.TaskRecord;

/**
 * This is a functional test only, it does not test any possible multithreading issues
 */
public abstract class TaskStoreTestBase {
	protected abstract TaskStore createStore();
	protected abstract void destroyStore(TaskStore store) throws Exception;
	
	protected TaskStore fixture;
	
	@Before
	public void setUp() {
		fixture = createStore();
	}
	
	@After
	public void tearDown() throws Exception {
		if (fixture != null) {
			destroyStore(fixture);
		}
	}
	
	@Test
	public void test_newTask() {
		final Task task = new Task();
		task.setMoniker("moniker1");
		final TaskRecord state = fixture.newTask(task);
		assertSameTasks(task, state.getTask());
		Assert.assertNull(fixture.newTask(task));
	}
	
	@Test
	public void testFindTask() {
		final Task task = new Task();
		task.setMoniker("moniker1");
		fixture.newTask(task);
		final TaskRecord state = fixture.find(task.getMoniker());
		assertSameTasks(task, state.getTask());
	}
	
	@Test
	public void test_cas() {
		final Task task1 = new Task();
		task1.setMoniker("moniker1");
		final TaskRecord state1 = fixture.newTask(task1);
		
		final Task task2 = task1.defrost();
		task2.setMemento("memento1");
		final TaskRecord state2 = fixture.cas(state1, task2);
		Assert.assertNotNull(state2);
		assertSameTasks(task2, state2.getTask());
		
		final Task task3 = task2.defrost();
		Assert.assertNull(fixture.cas(state1, task3));
	}
	
	@Test
	public void test_reload() {
		final Task task1 = new Task();
		task1.setMoniker("moniker1");
		final TaskRecord state1 = fixture.newTask(task1);
		
		final Task task2 = task1.defrost();
		task2.setMemento("memento1");
		fixture.cas(state1, task2);
		
		final TaskRecord state2 = fixture.reload(state1);
		assertSameTasks(task2, state2.getTask());
		
		final Task task3 = task2.defrost();
		task2.setHandlerMoniker("handlerMoniker1");
		final TaskRecord state3 = fixture.cas(state2, task3);
		assertSameTasks(task3, state3.getTask());
	}
	
	@Test
	public void test_remove() {
		final Task task1 = new Task();
		task1.setMoniker("moniker1");
		final TaskRecord state1 = fixture.newTask(task1);
		
		fixture.remove(task1.getMoniker());
		Assert.assertNull(fixture.reload(state1));
		Assert.assertNull(fixture.find(task1.getMoniker()));
	}
	
	@Test
	public void test_listScheduled_empty() throws IOException {
		try(final Surge<TaskRecord> surge = fixture.listScheduled(null, null, null)) {
			Assert.assertNull(surge.next());
		}
	}
	
	@Test
	public void test_listScheduled_onlyRunningTasks() throws IOException {
		final Task task1 = new Task();
		task1.setState(TaskState.RUNNING);
		task1.setMoniker("moniker1");
		task1.setNextRun(Timestamp.fromUTCMillis(100L));
		fixture.newTask(task1);

		try(final Surge<TaskRecord> surge = fixture.listScheduled(null, Arrays.asList(TaskState.SCHEDULED), null)) {
			Assert.assertNull(surge.next());
		}
	}
	
	@Test
	public void test_listScheduled_all() throws IOException {
		final Task task1 = new Task();
		task1.setState(TaskState.SCHEDULED);
		task1.setMoniker("moniker1");
		task1.setNextRun(Timestamp.fromUTCMillis(100L));
		fixture.newTask(task1);
		
		try(final Surge<TaskRecord> surge = fixture.listScheduled(null, null, null)) {
			final TaskRecord listed = surge.next();
			Assert.assertNull(surge.next());
			
			assertSameTasks(task1, listed.getTask());
		}
	}
	
	@Test
	public void test_listScheduled_filter_by_timestamp() throws IOException {
		final Task task1 = new Task();
		task1.setState(TaskState.SCHEDULED);
		task1.setMoniker("moniker1");
		task1.setNextRun(Timestamp.fromUTCMillis(100L));
		fixture.newTask(task1);
		
		final Task task2 = new Task();
		task2.setState(TaskState.SCHEDULED);
		task2.setMoniker("moniker2");
		task2.setNextRun(Timestamp.fromUTCMillis(200L));
		fixture.newTask(task2);
		
		final Task task3 = new Task();
		task3.setState(TaskState.SCHEDULED);
		task3.setMoniker("moniker3");
		task3.setNextRun(Timestamp.fromUTCMillis(300L));
		fixture.newTask(task3);

		final Task task4= new Task();
		task4.setState(TaskState.SCHEDULED);
		task4.setMoniker("moniker4");
		task4.setNextRun(Timestamp.fromUTCMillis(400L));
		fixture.newTask(task4);

		try(final Surge<TaskRecord> surge = fixture.listScheduled(null, null, Timestamp.fromUTCMillis(300L))) {
			final TaskRecord listed1 = surge.next();
			final TaskRecord listed2 = surge.next();
			final TaskRecord listed3 = surge.next();
			Assert.assertNull(surge.next());
			
			assertSameTasks(task1, listed1.getTask());
			assertSameTasks(task2, listed2.getTask());
			assertSameTasks(task3, listed3.getTask());
		}
	}
	
	@Test
	public void test_listScheduled_filter_by_handler() throws IOException {
		final Task task1 = new Task();
		task1.setState(TaskState.SCHEDULED);
		task1.setMoniker("moniker1");
		task1.setHandlerMoniker("handler1");
		task1.setNextRun(Timestamp.fromUTCMillis(100L));
		fixture.newTask(task1);
		
		final Task task2 = new Task();
		task2.setState(TaskState.SCHEDULED);
		task2.setMoniker("moniker2");
		task2.setHandlerMoniker("handler2");
		task2.setNextRun(Timestamp.fromUTCMillis(200L));
		fixture.newTask(task2);
		
		final Task task3 = new Task();
		task3.setState(TaskState.SCHEDULED);
		task3.setMoniker("moniker3");
		task3.setHandlerMoniker("handler3");
		task3.setNextRun(Timestamp.fromUTCMillis(300L));
		fixture.newTask(task3);

		try(final Surge<TaskRecord> surge = fixture.listScheduled(Arrays.asList("handler1", "handler2"), null, null)) {
			final TaskRecord listed1 = surge.next();
			final TaskRecord listed2 = surge.next();
			Assert.assertNull(surge.next());
			
			assertSameTasks(task1, listed1.getTask());
			assertSameTasks(task2, listed2.getTask());
		}
	}
	
	@Test
	public void test_listScheduled_filter_by_state() throws IOException {
		final Task task1 = new Task();
		task1.setState(TaskState.SCHEDULED);
		task1.setMoniker("moniker1");
		task1.setNextRun(Timestamp.fromUTCMillis(100L));
		fixture.newTask(task1);
		
		final Task task2 = new Task();
		task2.setState(TaskState.RUNNING);
		task2.setMoniker("moniker2");
		task2.setNextRun(Timestamp.fromUTCMillis(200L));
		fixture.newTask(task2);
		
		try(final Surge<TaskRecord> surge = fixture.listScheduled(null, Arrays.asList(TaskState.RUNNING), null)) {
			assertSameTasks(task2, surge.next().getTask());
			Assert.assertNull(surge.next());
		}
	}
	
	@Test
	public void test_listScheduled_unscheduleTask() throws IOException {
		Task task1 = new Task();
		task1.setState(TaskState.SCHEDULED);
		task1.setMoniker("moniker1");
		task1.setNextRun(Timestamp.fromUTCMillis(100L));
		TaskRecord state = fixture.newTask(task1);
		task1 = state.getTask().defrost();
		task1.setNextRun(null);
		state = fixture.cas(state, task1);
		Assert.assertNotNull(state);

		try(final Surge<TaskRecord> surge = fixture.listScheduled(null, null, null)) {
			Assert.assertNull(surge.next());
		}
	}
	
	@Ignore("This test highly depends on the way underlying iteration is implemented")
	@Test
	public void test_listScheduled_noPhantoms() throws IOException {
		Task task1 = new Task();
		task1.setState(TaskState.SCHEDULED);
		task1.setMoniker("moniker1");
		task1.setNextRun(Timestamp.fromUTCMillis(100L));
		TaskRecord state = fixture.newTask(task1);

		try(final Surge<TaskRecord> surge = fixture.listScheduled(null, null, null)) {
			task1 = state.getTask().defrost();
			task1.setNextRun(null);
			state = fixture.cas(state, task1);
			Assert.assertNotNull(state);

			Assert.assertNull(surge.next());
		}
	}
	
	@Test
	public void test_listScheduled_should_eventually_change_order_by_nextRun() throws IOException {
		Task task1 = new Task();
		task1.setState(TaskState.SCHEDULED);
		task1.setMoniker("moniker1");
		task1.setNextRun(Timestamp.fromUTCMillis(100L));
		TaskRecord state1 = fixture.newTask(task1);

		Task task2 = new Task();
		task2.setState(TaskState.SCHEDULED);
		task2.setMoniker("moniker2");
		task2.setNextRun(Timestamp.fromUTCMillis(200L));
		TaskRecord state2 = fixture.newTask(task2);
		
		task1 = state1.getTask().defrost();
		task1.setNextRun(Timestamp.fromUTCMillis(2000L));
		state1 = fixture.cas(state1, task1);
		Assert.assertNotNull(state1);
		
		task2 = state2.getTask().defrost();
		task2.setNextRun(Timestamp.fromUTCMillis(1000L));
		state2 = fixture.cas(state2, task2);
		Assert.assertNotNull(state2);

		try(final Surge<TaskRecord> surge = fixture.listScheduled(null, null, null)) {
			assertSameTasks(task2, surge.next().getTask());
			assertSameTasks(task1, surge.next().getTask());
			
			Assert.assertNull(surge.next());
		}
	}
	
	@Test
	public void test_listScheduledToNeverRun() throws IOException {
		final Task task1 = new Task();
		task1.setState(TaskState.SCHEDULED);
		task1.setMoniker("moniker1");
		fixture.newTask(task1);
		
		final Task task2 = new Task();
		task2.setState(TaskState.SCHEDULED);
		task2.setMoniker("moniker2");
		fixture.newTask(task2);
		
		final Task task3 = new Task();
		task3.setState(TaskState.SCHEDULED);
		task3.setMoniker("moniker3");
		fixture.newTask(task3);

		try(final Surge<TaskRecord> surge = fixture.listScheduledToNeverRun()) {
			final TaskRecord listed1 = surge.next();
			final TaskRecord listed2 = surge.next();
			final TaskRecord listed3 = surge.next();
			Assert.assertNull(surge.next());
			
			assertSameTasks(task1, listed1.getTask());
			assertSameTasks(task2, listed2.getTask());
			assertSameTasks(task3, listed3.getTask());
		}
	}
	
	@Test
	public void test_listScheduledToNeverRun_resheduleAgain() throws IOException {
		Task task1 = new Task();
		task1.setState(TaskState.SCHEDULED);
		task1.setMoniker("moniker1");
		TaskRecord state = fixture.newTask(task1);
		task1 = state.getTask().defrost();
		task1.setNextRun(Timestamp.now());
		state = fixture.cas(state, task1);
		Assert.assertNotNull(state);
		
		try(final Surge<TaskRecord> surge = fixture.listScheduledToNeverRun()) {
			Assert.assertNull(surge.next());
		}
	}
	
	@Ignore("This test highly depends on the way underlying iteration is implemented")
	@Test
	public void test_listScheduledToNeverRun_resheduleAgain_noPhantoms() throws IOException {
		Task task1 = new Task();
		task1.setState(TaskState.SCHEDULED);
		task1.setMoniker("moniker1");
		TaskRecord state = fixture.newTask(task1);
		
		try(final Surge<TaskRecord> surge = fixture.listScheduledToNeverRun()) {
			task1 = state.getTask().defrost();
			task1.setNextRun(Timestamp.now());
			state = fixture.cas(state, task1);
			Assert.assertNotNull(state);

			Assert.assertNull(surge.next());
		}
	}
	
	private static void assertSameTasks(final Task task1, final Task task2) {
		final String json1 = Marshall.get().toJson(task1);
		final String json2 = Marshall.get().toJson(task2);
		Assert.assertEquals(json1, json2);
	}
}
