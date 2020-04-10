package jabot.taskri.marshall;

import org.junit.Assert;
import org.junit.Test;

import jabot.taskapi.Schedule;
import jabot.taskri.store.marshall.ScheduleAdapter;

public class ScheduleAdapterTest {
	private final ScheduleAdapter fixture = new ScheduleAdapter();
	
	@Test
	public void test() {
		Assert.assertSame(Schedule.class, fixture.getHandledType());
		Assert.assertEquals("N", fixture.serialize(Schedule.NEVER));
		Assert.assertEquals(Schedule.NEVER, fixture.deserialize("N"));
	}
}
