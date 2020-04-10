package jabot.common.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class TimestampTest {
	
	@Test
	public void test_fromUTCMillis() {
		Assert.assertEquals(100L, Timestamp.fromUTCMillis(100L).asMillis());
		//Assert.assertEquals(200L, Timestamp.fromLocalDate(new Date(200L)).asMillis());
	}
	
	@Test
	public void test_fromLocalMillis_and_date() throws ParseException {
		final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		final Date local = fmt.parse("2000-12-01T00:00:00.000+0200");
		final long ts = Timestamp.fromLocalDate(local).asMillis();
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		Assert.assertEquals("2000-11-30T17:00:00.000+0000", fmt.format(new Date(ts)));
		
		Assert.assertEquals(ts, Timestamp.fromLocalMillis(local.getTime()).asMillis());
	}
	
	@Test
	public void testAsDate() {
		Assert.assertEquals(new Date(100L), Timestamp.fromUTCMillis(100L).asDate());
	}
	
	@Test
	public void testNow() {
		Assert.assertTrue(Timestamp.now().asMillis()<=System.currentTimeMillis());
	}
	
	@Test
	public void test_MIN_VALUE() {
		Assert.assertEquals(0, Timestamp.MIN_VALUE.asMillis());
	}
	
	@Test
	public void test_before() {
		Assert.assertTrue(Timestamp.fromUTCMillis(100L).before(Timestamp.fromUTCMillis(200L)));
		Assert.assertFalse(Timestamp.fromUTCMillis(100L).before(Timestamp.fromUTCMillis(100L)));
		Assert.assertFalse(Timestamp.fromUTCMillis(100L).before(Timestamp.fromUTCMillis(50L)));
	}
	
	@Test
	public void test_after() {
		Assert.assertTrue(Timestamp.fromUTCMillis(100L).isAfter(Timestamp.fromUTCMillis(50L)));
		Assert.assertFalse(Timestamp.fromUTCMillis(100L).isAfter(Timestamp.fromUTCMillis(100L)));
		Assert.assertFalse(Timestamp.fromUTCMillis(100L).isAfter(Timestamp.fromUTCMillis(200L)));
	}
	
	@Test
	public void testSince() {
		final Interval positive = Timestamp.fromUTCMillis(2000L).since(Timestamp.fromUTCMillis(1000L));
		Assert.assertEquals(1000L, positive.as(TimeUnit.MILLISECONDS));
		final Interval negative = Timestamp.fromUTCMillis(1000L).since(Timestamp.fromUTCMillis(2000L));
		Assert.assertEquals(-1000L, negative.as(TimeUnit.MILLISECONDS));
	}
	
	@Test
	public void testComparable() {
		Assert.assertTrue(Timestamp.fromUTCMillis(100).compareTo(Timestamp.fromUTCMillis(200))<0);
		Assert.assertTrue(Timestamp.fromUTCMillis(100).compareTo(Timestamp.fromUTCMillis(50))>0);
		Assert.assertTrue(Timestamp.fromUTCMillis(100).compareTo(Timestamp.fromUTCMillis(100))==0);
	}

	@Test
	public void testHashcodeEquals() {
		final Timestamp t1 = Timestamp.fromUTCMillis(100L);
		assertEquals(t1, t1);
		assertEquals(t1, Timestamp.fromUTCMillis(100L));

		assertNotEquals(t1, null);
		assertNotEquals(t1, new Object());
		assertNotEquals(t1, Timestamp.fromUTCMillis(200L));
	}

	private void assertEquals(final Timestamp t1, final Timestamp t2) {
		Assert.assertEquals(t1, t2);
		Assert.assertEquals(t1.hashCode(), t2.hashCode());
		Assert.assertEquals(t1.toString(), t2.toString());
	}

	private void assertNotEquals(Timestamp t, Object obj) {
		Assert.assertNotEquals(t, obj);
		if (obj != null) {
			Assert.assertNotEquals(t.hashCode(), obj.hashCode());
			Assert.assertNotEquals(t.toString(), obj.toString());
		}
	}

}
