package jabot.taskri.marshall;

import org.junit.Assert;
import org.junit.Test;

import jabot.taskapi.Progress;
import jabot.taskri.store.marshall.ProgressAdapter;

public class ProgressAdapterTest {
	private final ProgressAdapter fixture = new ProgressAdapter();
	
	@Test
	public void test() {
		Assert.assertSame(Progress.class, fixture.getHandledType());
		Assert.assertEquals("1/2/3/4", fixture.serialize(new Progress(1, 2, 3, 4)));
		Assert.assertEquals(new Progress(1, 2, 3, 4), fixture.deserialize("1/2/3/4"));
	}
}
