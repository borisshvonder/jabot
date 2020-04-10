package jabot.marshall.stdadapters;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class DateAdapterTest {
	private final DateAdapter fixture = new DateAdapter();
	
	@Test
	public void test() {
		Assert.assertSame(Date.class, fixture.getHandledType());
		Assert.assertEquals("100", fixture.serialize(new Date(100L)));
		Assert.assertEquals(new Date(100L), fixture.deserialize("100"));
	}
}
