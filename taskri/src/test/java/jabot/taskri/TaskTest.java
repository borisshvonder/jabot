package jabot.taskri;

import static jabot.frozentest.FreezeTestSupport.unitTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jabot.common.types.Interval;
import jabot.common.types.Timestamp;
import jabot.marshall.Marshall;
import jabot.taskapi.Progress;
import jabot.taskapi.Schedule;
import jabot.taskapi.TaskState;
import jabot.taskri.store.Task;

public class TaskTest {
	private Task fixture;
	
	@Before
	public void setUp() {
		Task task = new Task();
		task = unitTest(task, t -> t.setState(TaskState.RUNNING));
		task = unitTest(task, t -> t.setMoniker("moniker1"));
		task = unitTest(task, t -> t.setHandlerMoniker("handlerMoniker1"));
		task = unitTest(task, t -> t.setSchedule((new Schedule.Builder().once().build())));
		task = unitTest(task, t -> t.setFailedSchedule(new Schedule.Builder().delay(Interval.HOUR).build()));
		task = unitTest(task, t -> t.setLastRunStart(Timestamp.fromUTCMillis(100)));
		task = unitTest(task, t -> t.setLastRunFinish(Timestamp.fromUTCMillis(100)));
		task = unitTest(task, t -> t.setNextRun(Timestamp.fromUTCMillis(1000)));
		task = unitTest(task, t -> t.setLastSuccess(Timestamp.fromUTCMillis(2000)));
		task = unitTest(task, t -> t.setParams("params1"));
		task = unitTest(task, t -> t.setMemento("memento1"));
		task = unitTest(task, t -> t.setLastErrorClass("class1"));
		task = unitTest(task, t -> t.setLastErrorMessage("message"));
		task = unitTest(task, t -> t.setProgress(new Progress(1, 2, 3, 4)));
		task = unitTest(task, t -> t.setState(TaskState.SCHEDULED));
		fixture = task;
	}
	
	@Test
	public void testMarshall() {
		Assert.assertNotNull(fixture.getState());
		
		final String json = Marshall.get().toJson(fixture);
		final Task restored = Marshall.get().fromJson(json, Task.class);
		
		assertEqualsToFixture(restored);
	}
	
	private void assertEqualsToFixture(final Task restored) {
		Assert.assertEquals(fixture.getState(), restored.getState());
		Assert.assertEquals(fixture.getMoniker(), restored.getMoniker());
		Assert.assertEquals(fixture.getHandlerMoniker(), restored.getHandlerMoniker());
		Assert.assertEquals(fixture.getSchedule(), restored.getSchedule());
		Assert.assertEquals(fixture.getFailedSchedule(), restored.getFailedSchedule());
		Assert.assertEquals(fixture.getLastRunStart(), restored.getLastRunStart());
		Assert.assertEquals(fixture.getLastRunFinish(), restored.getLastRunFinish());
		Assert.assertEquals(fixture.getNextRun(), restored.getNextRun());
		Assert.assertEquals(fixture.getParams(), restored.getParams());
		Assert.assertEquals(fixture.getMemento(), restored.getMemento());
		Assert.assertEquals(fixture.getLastErrorClass(), restored.getLastErrorClass());
		Assert.assertEquals(fixture.getLastErrorMessage(), restored.getLastErrorMessage());
		Assert.assertEquals(fixture.getProgress(), restored.getProgress());
	}
}
