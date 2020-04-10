package jabot.taskri.marshall;

import org.junit.Assert;
import org.junit.Test;

import jabot.common.types.Timestamp;
import jabot.taskri.store.marshall.TimestampAdapter;

public class TimestampAdapterTest {
	private final TimestampAdapter fixture = new TimestampAdapter();
	
	@Test
	public void test() {
		Assert.assertSame(Timestamp.class, fixture.getHandledType());
		Assert.assertEquals("100", fixture.serialize(Timestamp.fromUTCMillis(100L)));
		Assert.assertEquals(Timestamp.fromUTCMillis(200L), fixture.deserialize("200"));
	}
}
