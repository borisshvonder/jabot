package jabot.taskri.marshall;

import org.junit.Assert;
import org.junit.Test;

import jabot.common.types.Interval;
import jabot.taskri.store.marshall.IntervalAdapter;

public class IntervalAdapterTest {
	private final IntervalAdapter fixture = new IntervalAdapter();
	
	@Test
	public void test() {
		Assert.assertSame(Interval.class, fixture.getHandledType());
		Assert.assertEquals("1DAYS", fixture.serialize(Interval.DAY));
		Assert.assertEquals(Interval.DAY, fixture.deserialize("1DAYS"));
	}
}
