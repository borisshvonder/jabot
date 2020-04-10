package jabot.taskapi;

import org.junit.Assert;
import org.junit.Test;

import jabot.common.types.Interval;
import jabot.common.types.Timestamp;

public class ScheduleTest {
	
	@Test
	public void once() {
		final Schedule s = Schedule.once();
		
		Assert.assertEquals(Schedule.Type.Once, s.getType());
		Assert.assertNull(s.getInterval());
		Assert.assertFalse(s.getStart().isAfter(Timestamp.now()));
	}

	@Test
	public void buildOnce() {
		final Schedule s = new Schedule.Builder()
			.once()
			.runNow()
			.build();
		
		Assert.assertEquals(Schedule.Type.Once, s.getType());
		Assert.assertNull(s.getInterval());
		Assert.assertFalse(s.getStart().isAfter(Timestamp.now()));
	}
	
	@Test
	public void delay() {
		final Schedule s = Schedule.delay(Interval.DAY);
			
		Assert.assertEquals(Schedule.Type.Delay, s.getType());
		Assert.assertEquals(Interval.DAY, s.getInterval());
		Assert.assertFalse(s.getStart().isAfter(Timestamp.now()));
	}

	@Test
	public void buildDelay() {
		Schedule s = new Schedule.Builder()
				.delay(Interval.DAY)
				.build();
			
		Assert.assertEquals(Schedule.Type.Delay, s.getType());
		Assert.assertEquals(Interval.DAY, s.getInterval());
		Assert.assertFalse(s.getStart().isAfter(Timestamp.now()));
	}

	@Test
	public void rate() {
		final Schedule s = Schedule.rate(Interval.HOUR);
			
		Assert.assertEquals(Schedule.Type.Rate, s.getType());
		Assert.assertFalse(s.getStart().isAfter(Timestamp.now()));
		Assert.assertEquals(Interval.HOUR, s.getInterval());
	}

	@Test
	public void buildRate() {
		final Schedule s = new Schedule.Builder()
				.start(Timestamp.fromUTCMillis(100000000L))
				.rate(Interval.HOUR)
				.build();
			
		Assert.assertEquals(Schedule.Type.Rate, s.getType());
		Assert.assertEquals(Interval.HOUR, s.getInterval());
		Assert.assertEquals(Timestamp.fromUTCMillis(100000000L), s.getStart());
	}
	
	@Test
	public void runAt() {
		final Schedule s = new Schedule.Builder()
				.start(Timestamp.fromUTCMillis(100000000L))
				.rate(Interval.HOUR)
				.build();

		Schedule s2 = s.runAt(Timestamp.fromUTCMillis(100L));
		Assert.assertEquals(Timestamp.fromUTCMillis(100L), s2.getStart());
	}
	
	@Test
	public void testScheduleNever() {
		Assert.assertNull(Schedule.NEVER.nextRun(null, null));
		Assert.assertNull(Schedule.NEVER.nextRun(Timestamp.now(), Timestamp.now()));
	}
	
	@Test
	public void test_ASAP() {
		final Schedule s = Schedule.ASAP;
		
		Assert.assertEquals(Schedule.Type.Once, s.getType());
		Assert.assertNull(s.getInterval());
		Assert.assertEquals(0, s.getStart().asMillis());
	}

	@Test
	public void testScheduleOnce() {
		final Schedule s = new Schedule.Builder()
				.once()
				.start(Timestamp.fromUTCMillis(100))
				.build();
		
		Assert.assertEquals(Timestamp.fromUTCMillis(100), s.nextRun(null, null));
		Assert.assertEquals(Timestamp.fromUTCMillis(100), s.nextRun(Timestamp.now(), Timestamp.now()));
	}
	
	@Test
	public void testScheduleRate() {
		final Schedule s = new Schedule.Builder()
				.rate(Interval.SECOND)
				.start(Timestamp.fromUTCMillis(100))
				.build();
		
		Assert.assertEquals(Timestamp.fromUTCMillis(100), s.nextRun(null, null));
		Assert.assertEquals(Timestamp.fromUTCMillis(2000+1000), s.nextRun(Timestamp.fromUTCMillis(2000), null));
		Assert.assertEquals(Timestamp.fromUTCMillis(2000+1000), s.nextRun(Timestamp.fromUTCMillis(2000), Timestamp.now()));
	}
	
	@Test
	public void testScheduleDelay() {
		final Schedule s = new Schedule.Builder()
				.delay(Interval.SECOND)
				.start(Timestamp.fromUTCMillis(100))
				.build();
		
		Assert.assertEquals(Timestamp.fromUTCMillis(100), s.nextRun(null, null));
		Assert.assertEquals(Timestamp.fromUTCMillis(2000+1000), s.nextRun(null, Timestamp.fromUTCMillis(2000)));
		Assert.assertEquals(Timestamp.fromUTCMillis(2000+1000), s.nextRun(Timestamp.now(), Timestamp.fromUTCMillis(2000)));
	}
	
	@Test
	public void testMarshall() {
		Assert.assertEquals("N", Schedule.NEVER.marshall());
		Assert.assertEquals("O:100", new Schedule.Builder().once().start(Timestamp.fromUTCMillis(100)).build().marshall());
		Assert.assertEquals("R:100:1HOURS", new Schedule.Builder().rate(Interval.HOUR).start(Timestamp.fromUTCMillis(100)).build().marshall());
		Assert.assertEquals("D:100:1HOURS", new Schedule.Builder().delay(Interval.HOUR).start(Timestamp.fromUTCMillis(100)).build().marshall());
	}
	
	@Test
	public void testUnmarshall() {
		Assert.assertEquals(Schedule.NEVER,  Schedule.unmarshall("N"));
		Assert.assertEquals(new Schedule.Builder().once().start(Timestamp.fromUTCMillis(100L)).build(),  Schedule.unmarshall("O:100"));
		Assert.assertEquals(new Schedule.Builder().rate(Interval.HOUR).start(Timestamp.fromUTCMillis(100)).build(),  Schedule.unmarshall("R:100:1HOURS"));
		Assert.assertEquals(new Schedule.Builder().delay(Interval.HOUR).start(Timestamp.fromUTCMillis(100)).build(),  Schedule.unmarshall("D:100:1HOURS"));
	}
	
	@Test(expected=ScheduleFormatException.class)
	public void testUnmarshall_badTypeStr() {
		Schedule.unmarshall("BADTYPE");
	}
	
	@Test(expected=ScheduleFormatException.class)
	public void testUnmarshall_badTypeChar() {
		Schedule.unmarshall("?:100:1HOURS");
	}
	
	@Test(expected=ScheduleFormatException.class)
	public void testUnmarshall_tooManyParts() {
		Schedule.unmarshall("R:100:1HOURS:hello");
	}
	
	@Test(expected=ScheduleFormatException.class)
	public void testUnmarshall_onceWithInterval() {
		Schedule.unmarshall("O:100:1HOURS");
	}
	
	@Test(expected=ScheduleFormatException.class)
	public void testUnmarshall_neverWithStart() {
		Schedule.unmarshall("N:100");
	}
	
	@Test
	public void testToString() {
		Assert.assertEquals(Schedule.NEVER.toString(), Schedule.NEVER.toString());
		Assert.assertNotEquals(Schedule.NEVER.toString(), new Schedule.Builder().once().build().toString());
	}
	
	@Test(expected=IllegalStateException.class)
	public void cannotRunAt_NEVER_schedule() {
		Schedule.NEVER.runAt(Timestamp.fromUTCMillis(0));
	}
	
	@Test
	public void test_hashCode_equals() {
		assertEquals(Schedule.NEVER, Schedule.NEVER);
		
		final Schedule s1 = new Schedule.Builder().start(Timestamp.MIN_VALUE).once().build();
		assertEquals(s1, s1);
		assertNotEquals(s1, null);
		assertNotEquals(s1, new Object());
	
		assertEquals(s1, new Schedule.Builder().start(Timestamp.MIN_VALUE).once().build());
		
		final Schedule s2 = new Schedule.Builder().start(Timestamp.MIN_VALUE).rate(Interval.DAY).build();
		assertNotEquals(s1, s2);
		
		final Schedule s3 = new Schedule.Builder().start(Timestamp.MIN_VALUE).rate(Interval.HOUR).build();
		assertNotEquals(s1, s3);
		assertNotEquals(s2, s3);
		
		final Schedule s4 = new Schedule.Builder().start(Timestamp.MIN_VALUE).delay(Interval.HOUR).build();
		assertNotEquals(s3, s4);
	}
	
	private void assertEquals(final Schedule s1, final Schedule s2) {
		Assert.assertEquals(s1, s2);
		Assert.assertEquals(s1.hashCode(), s2.hashCode());
		Assert.assertEquals(s1.toString(), s2.toString());
		Assert.assertEquals(s1.marshall(), s2.marshall());
	}
	
	private void assertNotEquals(final Schedule s1, final Object o) {
		Assert.assertNotEquals(s1, o);
		if (o != null) {
			Assert.assertNotEquals(s1.hashCode(), o.hashCode());
			Assert.assertNotEquals(s1.toString(), o.toString());
		}
		if (o instanceof Schedule) {
			Assert.assertNotEquals(s1.marshall(), ((Schedule) o).marshall());
		}
	}
}
