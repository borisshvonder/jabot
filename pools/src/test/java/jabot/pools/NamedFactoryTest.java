package jabot.pools;

import org.junit.Test;

import org.junit.Assert;

public class NamedFactoryTest {
	@Test
	public void testCreateThread() {
		final NamedFactory fixture = new NamedFactory("name", true);
		Assert.assertEquals("name", fixture.getName());
		Assert.assertTrue(fixture.isDaemon());
		
		final Thread t = fixture.newThread(()->{});
		Assert.assertTrue(t.isDaemon());
		Assert.assertTrue(t.getName().startsWith("name"));
	}
	
	@Test
	public void testNonDaemon() {
		final NamedFactory fixture = new NamedFactory("factory", false);
		Assert.assertEquals("factory", fixture.getName());
		Assert.assertFalse(fixture.isDaemon());

		final Thread t = fixture.newThread(()->{});
		Assert.assertFalse(t.isDaemon());
		Assert.assertTrue(t.getName().startsWith("factory"));
	}
}
