package jabot.common.types;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.junit.Assert;

public class IntervalUtilsTest {
	@Test
	public void test_humanReadable() {
		assertHumanReadable("+1Y", Interval.YEAR, 10);
		assertHumanReadable("+30D", Interval.MONTH, 10);
		assertHumanReadable("+1D", Interval.DAY, 10);
		assertHumanReadable("+1h", Interval.HOUR, 10);
		assertHumanReadable("+1m", Interval.MINUTE, 10);
		assertHumanReadable("+1s", Interval.SECOND, 10);
		assertHumanReadable("+1ms", Interval.MILLISECOND, 10);
		assertHumanReadable("", Interval.ZERO, 10);
		
		assertHumanReadable("-1Y", new Interval(-365L, TimeUnit.DAYS), 10);
		
		long time = 2048;
		assertHumanReadable("+2048Y", new Interval(365*time, TimeUnit.DAYS), 10);
		
		time = time*365 + 127;
		assertHumanReadable("+2048Y127D", new Interval(time, TimeUnit.DAYS), 10);

		time = time*24 + 9;  // 9h
		assertHumanReadable("+2048Y127D9h", new Interval(time, TimeUnit.HOURS), 10);
		
		time = time*60 + 8;  // 8m
		assertHumanReadable("+2048Y127D9h8m", new Interval(time, TimeUnit.MINUTES), 10);

		time = time*60 + 7;  // 7s
		assertHumanReadable("+2048Y127D9h8m7s", new Interval(time, TimeUnit.SECONDS), 10);

		time = time*1000 + 6;  // 6ms
		assertHumanReadable("+2048Y127D9h8m7s6ms", new Interval(time, TimeUnit.MILLISECONDS), 10);
		assertHumanReadable("-2048Y127D9h8m7s6ms", new Interval(-time, TimeUnit.MILLISECONDS), 10);
		
		time = ((3L*365L*24L*60L + 5L)*60L + 23)*1000L + 56;
		assertHumanReadable("+3Y5m23s56ms", new Interval(time, TimeUnit.MILLISECONDS), 10);
		assertHumanReadable("+3Y", new Interval(time, TimeUnit.MILLISECONDS), 1);
		assertHumanReadable("+3Y5m", new Interval(time, TimeUnit.MILLISECONDS), 2);
		assertHumanReadable("+3Y5m23s", new Interval(time, TimeUnit.MILLISECONDS), 3);
		assertHumanReadable("+3Y5m23s56ms", new Interval(time, TimeUnit.MILLISECONDS), 4);

	}

	private void assertHumanReadable(final String expected, final Interval interval, int precision) {
		Assert.assertEquals(interval.toString(), expected, IntervalUtils.humanReadable(interval, precision));
	}
}
