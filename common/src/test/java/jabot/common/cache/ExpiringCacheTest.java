package jabot.common.cache;

import org.junit.Assert;
import org.junit.Test;

import jabot.common.types.Interval;

public class ExpiringCacheTest {
	
	@Test
	public void testAsCacheUsage() {
		final ExpiringCache<String> cache = new ExpiringCache<>(Interval.YEAR);
		String value = cache.get();
		if (value == null) {	
			value = "value";
			cache.set(value);
		}
		Assert.assertEquals(value, cache.get());
	}
	
	@Test
	public void testAsLimiterUsage() {
		final ExpiringCache<Object> cache = new ExpiringCache<>(Interval.YEAR);
		Assert.assertTrue(cache.isExpired());
		cache.set(null);
		Assert.assertFalse(cache.isExpired());
	}

	@Test
	public void testExpiry() throws InterruptedException {
		final ExpiringCache<String> cache = new ExpiringCache<>(Interval.ZERO);
		cache.set("value");
		Thread.sleep(1L);
		Assert.assertTrue(cache.isExpired());
		Assert.assertNull(cache.get());
	}
}
