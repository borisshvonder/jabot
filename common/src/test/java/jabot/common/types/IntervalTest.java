package jabot.common.types;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class IntervalTest {
	@Test
	public void coverageTest() {
		Assert.assertEquals(1L, Interval.SECOND.getValue());
		Assert.assertEquals(TimeUnit.SECONDS, Interval.SECOND.getUnit());

		Assert.assertEquals(1L, Interval.SECOND.as(TimeUnit.SECONDS));
		Assert.assertEquals(1000L, Interval.SECOND.asMillis());
	}
	
	@Test
	public void sum_MINUTE_and_HOUR() {
		Interval sum1 = Interval.sum(Interval.MINUTE, Interval.HOUR);
		Interval sum2 = Interval.sum(Interval.HOUR, Interval.MINUTE);
		
		Assert.assertEquals(61L, sum1.getValue());
		Assert.assertEquals(TimeUnit.MINUTES, sum1.getUnit());
		Assert.assertEquals(sum1, sum2);
	}
	
	@Test
	public void testToString() {
		Assert.assertEquals("365DAYS", Interval.YEAR.toString());
	}
	
	@Test
	public void testComparable() {
		Assert.assertTrue(Interval.MINUTE.compareTo(Interval.HOUR)<0);
		Assert.assertTrue(Interval.MINUTE.compareTo(Interval.MINUTE)==0);
		Assert.assertTrue(Interval.MINUTE.compareTo(Interval.SECOND)>0);
	}
	
	@Test
	public void testHashCodeAndEquals() {
		assertEqualsAndSameHash(Interval.SECOND, Interval.SECOND);
		Assert.assertFalse(Interval.SECOND.equals(null));
		Assert.assertFalse(Interval.SECOND.equals(new Object()));
		Assert.assertNotEquals(Interval.SECOND, Interval.MINUTE);
		assertNotEquals(Interval.SECOND, Interval.MINUTE);
		assertEqualsAndSameHash(Interval.SECOND, new Interval(1L, TimeUnit.SECONDS));
		assertEqualTimeframes(Interval.HOUR, new Interval(60L, TimeUnit.MINUTES));
		assertEqualsAndSameHash(new Interval(Long.MAX_VALUE, TimeUnit.DAYS), new Interval(Long.MAX_VALUE, TimeUnit.DAYS));
	}
	
	@Test
	public void unmarshall_does_not_match_pattern() {
		try {
			Interval.unmarshall("badformat");
			Assert.fail();
		} catch (final IntervalFormatException ex) {
			Assert.assertEquals("badformat", ex.getText());
		}
	}
	
	@Test
	public void unmarshall_bad_long_value() {
		final String str = "111111111111111111111111111111111111111111DAYS";
		try {
			Interval.unmarshall(str);
			Assert.fail();
		} catch (final IntervalFormatException ex) {
			Assert.assertEquals(str, ex.getText());
			Assert.assertTrue(ex.getCause() instanceof NumberFormatException);
		}
	}
	
	@Test
	public void unmarshall_bad_unit() {
		final String str = "1BOX";
		try {
			Interval.unmarshall(str);
			Assert.fail();
		} catch (final IntervalFormatException ex) {
			Assert.assertEquals(str, ex.getText());
		}
	}
	
	@Test
	public void unmarshall() {
		Assert.assertEquals(Interval.MILLISECOND, Interval.unmarshall("1MILLISECOND"));
		Assert.assertEquals(Interval.MILLISECOND, Interval.unmarshall("1 MILLISECOND"));
		Assert.assertEquals(Interval.MILLISECOND, Interval.unmarshall("1  MILLISECONDS"));
		Assert.assertEquals(Interval.SECOND, Interval.unmarshall("1SECOND"));
		Assert.assertEquals(Interval.MINUTE, Interval.unmarshall("1MINUTE"));
		Assert.assertEquals(Interval.HOUR, Interval.unmarshall("1HOUR"));
		Assert.assertEquals(Interval.DAY, Interval.unmarshall("1DAY"));
		Assert.assertEquals(Interval.WEEK, Interval.unmarshall("1WEEK"));
		Assert.assertEquals(Interval.MONTH, Interval.unmarshall("1MONTH"));
		Assert.assertEquals(Interval.YEAR, Interval.unmarshall("1YEAR"));
		Assert.assertEquals(Interval.ZERO, Interval.unmarshall("0YEAR"));
		Assert.assertEquals(Interval.ZERO, Interval.unmarshall("0SECOND"));
		
		Assert.assertEquals(new Interval(365*2L, TimeUnit.DAYS), Interval.unmarshall("2YEARS"));
	}

	private void assertEqualsAndSameHash(Interval i1, Interval i2) {
		Assert.assertEquals(i1, i2);
		Assert.assertEquals(i1.hashCode(), i2.hashCode());
		assertEqualTimeframes(i1, i2);
	}
	
	private void assertEqualTimeframes(Interval i1, Interval i2) {
		Assert.assertTrue(i1+"!="+i2, Interval.equalTimeframes(i1, i2));
	}
	
	private void assertNotEquals(Interval i1, Interval i2) {
		Assert.assertNotEquals(i1, i2);
		Assert.assertNotEquals(i1.hashCode(), i2.hashCode());
		assertNotEqualTimeframes(i1, i2);
	}
	
	private void assertNotEqualTimeframes(Interval i1, Interval i2) {
		Assert.assertFalse(i1+"=="+i2, Interval.equalTimeframes(i1, i2));
	}
}
