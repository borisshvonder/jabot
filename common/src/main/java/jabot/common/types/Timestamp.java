package jabot.common.types;

import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * An immutable UTC timestamp
 */
public class Timestamp implements Comparable<Timestamp> {
	public static final Timestamp MIN_VALUE = fromUTCMillis(0);
	private static final TimeZone LOCAL_ZONE = TimeZone.getDefault();
	
	private final long fastTime;

	private Timestamp(long fastTime) {
		this.fastTime = fastTime;
	}
	
	public static final Timestamp now() {
		return fromLocalMillis(System.currentTimeMillis());
	}

	public static final Timestamp fromUTCMillis(final long millis) {
		return new Timestamp(millis);
	}
	
	public static final Timestamp fromLocalMillis(final long millis) {
		return new Timestamp(millis + LOCAL_ZONE.getOffset(millis));
	}
	
	public static final Timestamp fromLocalDate(final Date date) {
		return fromLocalMillis(date.getTime());
	}
	
	public long asMillis() {
		return fastTime;
	}
	
	public Date asDate() {
		return new Date(fastTime);
	}
	
	public boolean isAfter(final Timestamp other) {
		return fastTime > other.fastTime;
	}
	
	public boolean before(final Timestamp other) {
		return fastTime < other.fastTime;
	}
	
	public Interval since(final Timestamp since) {
		return new Interval(fastTime - since.fastTime, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public String toString() {
		return String.valueOf(fastTime);
	}

	@Override
	public int hashCode() {
		return (int) fastTime;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof Timestamp) {
			final Timestamp other = (Timestamp) obj;
			return other.fastTime == fastTime;
		}
		return false;
	}

	@Override
	public int compareTo(final Timestamp o) {
		if (fastTime < o.fastTime) {
			return -1;
		} else if (fastTime == o.fastTime) {
			return 0;
		} else {
			return 1;
		}
	}

}
