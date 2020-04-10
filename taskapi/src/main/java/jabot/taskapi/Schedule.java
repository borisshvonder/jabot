package jabot.taskapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

import jabot.common.math.JMath;
import jabot.common.types.Interval;
import jabot.common.types.Timestamp;
import javolution.text.TextBuilder;

public class Schedule {
	public static final Schedule NEVER = new Schedule(null, Type.Never, null);
	public static final Schedule ASAP = new Schedule(Timestamp.MIN_VALUE, Type.Once, null);

	private static final Map<Character, Type> TYPES_BY_LETTER = getTypesByLetter();
	private static final int MAX_MARSHALLED_LENGTH=1+String.valueOf(Long.MIN_VALUE).length()+Interval.MAX_TOSTRING_LENGTH;
	private static final String NEVER_MARSHALLED=String.valueOf(Type.Never.name().charAt(0));
	
	private final Timestamp start;
	private final Type type;
	private final Interval interval;
	
	private Schedule(final Timestamp start, final Type type, final Interval interval) {
		this.start = start;
		this.type = type;
		this.interval = interval;
	}
	
	/**
	 * A shortcut for creating schedule to run now and once
	 * @return
	 */
	public static Schedule once() {
		return new Builder().once().build();
	}
	
	/**
	 * A shortcut for creating schedule to run now with fixed delay 
	 * @return
	 */
	public static Schedule delay(final Interval interval) {
		return new Builder()
				.delay(interval)
				.build();
	}
	
	/**
	 * A shortcut for creating schedule to run now with fixed rate
	 * @return
	 */
	public static Schedule rate(final Interval interval) {
		return new Builder()
				.rate(interval)
				.build();
	}

	public Timestamp getStart() {
		return start;
	}

	public Type getType() {
		return type;
	}

	public Interval getInterval() {
		return interval;
	}
	
	/**
	 * Create a new schedule with new start Timestamp.
	 *  
	 * schedule MUST NOT have type == Never
	 * 
	 * @param @notnull start
	 * @return a schedule 
	 * @throws IllegalStateException when type == Never
	 */
	public Schedule runAt(Timestamp start) {
		Validate.notNull(start, "start cannot be null");
		if (type == Type.Never) {
			throw new IllegalStateException("Cannot schedule Never to run now");
		}
		
		return new Schedule(start, type, interval);
	}
	
	/**
	 * Schedule next run Timestamp
	 * @param @nullable prevRunStart when task started previous time
	 * @param @nullable prevRunFinish when task ended previous time
	 * @return null if task should not be scheduled at all, otherwise next run Timestamp
	 */
	public Timestamp nextRun(Timestamp prevRunStart, Timestamp prevRunFinish) {
		// Alternative implementation would subclass the Schedule class or add logic to the Type itself
		// But, for interface simplicity switch should be fine
		switch(type) {
		case Once:	return start;
		case Delay:	return scheduleAfter(prevRunFinish);
		case Rate:	return scheduleAfter(prevRunStart);
		case Never:	return null;
		default:	throw new AssertionError("Unhandled type "+type);
		}
	}

	private Timestamp scheduleAfter(Timestamp timestamp) {
		if (timestamp == null) {
			return start;
		} else {
			final long l1 = timestamp.asMillis();
			final long l2 = interval.asMillis();
			final long sum = JMath.addWithoutOverflow(l1, l2);
			return Timestamp.fromUTCMillis(sum);
		}
	}
	
	
	
	@Override
	public int hashCode() {
		return Objects.hash(start, type, interval);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof Schedule) {
			final Schedule other = (Schedule) obj;
			return type == other.type && Objects.equals(start, other.start) && Objects.equals(interval, other.interval);
		}
		return false;
	}

	@Override
	public String toString() {
		return marshall();
	}

	public String marshall() {
		if (type == Type.Never) {
			return NEVER_MARSHALLED;
		}
		Validate.notNull(start, "Assuming start is not null here since Never is ruled out");

		final TextBuilder b = new TextBuilder(MAX_MARSHALLED_LENGTH);
		b.append(type.name().charAt(0));
		b.append(':');
		b.append(String.valueOf(start.asMillis()));
		if (interval != null) {
			b.append(':');
			b.append(interval.toString());
		}
		return b.toString();
	}
	
	public static Schedule unmarshall(final String str) {
		String[] parts = str.split("\\:");
		String typeStr = parts[0].trim();
		if (typeStr.length() != 1) {
			throw new ScheduleFormatException(str);
		}
		Type type = TYPES_BY_LETTER.get(typeStr.charAt(0));
		if (type == null) {
			throw new ScheduleFormatException(str);
		}
		Timestamp start = parts.length > 1 ? Timestamp.fromUTCMillis(Long.parseLong(parts[1].trim())) : null;
		Interval interval = parts.length > 2 ? Interval.unmarshall(parts[2].trim()) : null;
		
		switch(parts.length) {
		case 1: assertType(str, type, Type.Never); return NEVER;
		case 2: assertType(str, type, Type.Once); break;
		case 3: assertType(str, type, Type.Delay, Type.Rate); break;
		default: throw new ScheduleFormatException(str);
		}
		return new Schedule(start, type, interval);
	}

	private static void assertType(final String str, final Type actual, final Type ... expected) {
		for (final Type e : expected) {
			if (actual == e) {
				return;
			}
		}
		throw new ScheduleFormatException(str);
	}

	public static final class Builder {
		private Timestamp start = Timestamp.now();
		private Type type = Type.Never;
		private Interval interval = Interval.HOUR;
		
		public Builder rate(final Interval interval) {
			Validate.notNull(interval, "interval cannot be null");

			this.type = Type.Rate;
			this.interval = interval;
			return this;
		}

		public Builder delay(final Interval interval) {
			Validate.notNull(interval, "interval cannot be null");

			this.type = Type.Delay;
			this.interval = interval;
			return this;
		}

		public Builder once() {
			this.type = Type.Once;
			this.interval = null;
			return this;
		}

		public Builder runNow() {
			return start(Timestamp.now());
		}

		public Builder start(final Timestamp when) {
			Validate.notNull(when, "when cannot be null");
			
			this.start = when;
			return this;
		}

		public Schedule build() {
			return new Schedule(start, type, interval);
		}
	}

	private static Map<Character, Type> getTypesByLetter() {
		Map<Character, Type> ret = new HashMap<>(Type.values().length);
		for (final Type t : Type.values()) {
			char c = t.name().charAt(0);
			final Type conflicting = ret.put(c, t);
			if (conflicting != null) {
				throw new AssertionError(Type.class.getName()+" has multiple entries starting with letter "+c+
						". Either rename em, or rethink the marshalling solution. Conflicting entries are "+t+" and "+conflicting);
			}
		}
		return Collections.unmodifiableMap(ret);
	}

	public static enum Type {
		/** Run task once only */
		Once,
		
		/** Run task periodically with fixed delay between executions **/
		Delay,
		
		/** Run task periodically with fixed rate of execution **/
		Rate,
		
		/** Never run */
		Never;
		
	}
}
