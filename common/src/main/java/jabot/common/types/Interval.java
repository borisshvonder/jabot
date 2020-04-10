package jabot.common.types;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import jabot.common.math.JMath;
import javolution.text.TextBuilder;

/** Time interval **/
public class Interval implements Comparable<Interval> {
	public static final int MAX_TOSTRING_LENGTH=(Long.MIN_VALUE+"MILLISECONDS").length();
	private static final Pattern PARSE_PAT = Pattern.compile("(\\d+)(.*?)");
	
	public static final Interval ZERO			= new Interval(0L, TimeUnit.MILLISECONDS);
	public static final Interval MILLISECOND	= new Interval(1L, TimeUnit.MILLISECONDS);
	public static final Interval SECOND			= new Interval(1L, TimeUnit.SECONDS);
	public static final Interval MINUTE			= new Interval(1L, TimeUnit.MINUTES);
	public static final Interval HOUR			= new Interval(1L, TimeUnit.HOURS);
	public static final Interval DAY			= new Interval(1L, TimeUnit.DAYS);
	public static final Interval WEEK			= new Interval(7L, TimeUnit.DAYS);
	public static final Interval MONTH			= new Interval(30L, TimeUnit.DAYS);
	public static final Interval YEAR			= new Interval(365L, TimeUnit.DAYS);

	public static final Interval TEN_SECONDS	= new Interval(10L, TimeUnit.SECONDS);

	private final TimeUnit unit;
	private final long value;
	
	public Interval(long value, TimeUnit unit) {
		this.unit = unit;
		this.value = value;
	}
	
	/**
	 * Parse interval from string. Supports 10DAY, 10DAYS, 10 DAYS, 10 DAY
	 * @param @notnull str
	 * @return parsed interval value
	 * @throws IntervalFormatException when format is not valid
	 */
	public static Interval unmarshall(String str) {
		Validate.notNull(str, "str cannot be null");
		
		final Matcher m = PARSE_PAT.matcher(str);
		if (m.matches()) {
			try {
				long value = Long.parseLong(m.group(1));
				String unit = m.group(2).trim().toUpperCase();
				if (unit.endsWith("S")) {
					unit = unit.substring(0, unit.length()-1);
				}
				
				final TimeUnit asUnit;
				
				switch(unit) {
				case "MILLISECOND":	asUnit = TimeUnit.MILLISECONDS; break;
				case "SECOND":		asUnit = TimeUnit.SECONDS; break;
				case "MINUTE":		asUnit = TimeUnit.MINUTES; break;
				case "HOUR":		asUnit = TimeUnit.HOURS; break;
				case "DAY":			asUnit = TimeUnit.DAYS; break;
				case "WEEK":		asUnit = TimeUnit.DAYS; value=value*7; break;
				case "MONTH":		asUnit = TimeUnit.DAYS; value=value*30; break;
				case "YEAR":		asUnit = TimeUnit.DAYS; value=value*365; break;
				default: throw new IntervalFormatException(str);
				}
				
				if (value == 0) {
					return ZERO;
				} else {
					return new Interval(value, asUnit);
				}
				
			} catch (final NumberFormatException ex) {
				throw new IntervalFormatException(str, ex);
			}
		} else {
			throw new IntervalFormatException(str);
		}
	}
	
	public String marshall() {
		final TextBuilder builder = new TextBuilder(MAX_TOSTRING_LENGTH);
		builder.append(value);
		builder.append(unit.toString());
		return builder.toString();
	}
	

	public TimeUnit getUnit() {
		return unit;
	}

	public long getValue() {
		return value;
	}
	
		
	/**
	 * Sum up two intervals
	 * 
	 * NOTE: this method will overflow at Long.MAX_VALUE
	 * 
	 * @param @notnull i1
	 * @param @notnull i2
	 */
	public static Interval sum(final Interval i1, final Interval i2) {
		Validate.notNull(i1, "i1 cannot be null");
		Validate.notNull(i2, "i2 cannot be null");
		
		final TimeUnit common = lowesCommonUnit(i1, i2);
		final long value = JMath.addWithoutOverflow(i1.as(common), i2.as(common));
		
		return new Interval(value, common);
	}

	/**
	 * Check that two intervals are describing same timeframe disregarding precision
	 * 
	 * NOTE: this method will overflow at Long.MAX_VALUE
	 * 
	 * @param @notnull i1
	 * @param @notnull i2
	 */
	public static boolean equalTimeframes(final Interval i1, final Interval i2) {
		Validate.notNull(i1, "i1 cannot be null");
		Validate.notNull(i2, "i2 cannot be null");
		
		if (i1 == i2) {
			return true;
		} else {
			final TimeUnit common = lowesCommonUnit(i1, i2);
			return i1.as(common) == i2.as(common);
		}
	}
	

	public long as(TimeUnit unit) {
		return unit.convert(this.value, this.unit);
	}
	
	public long asMillis() {
		return as(TimeUnit.MILLISECONDS);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(unit, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof Interval) {
			Interval other = (Interval) obj;
			return value == other.value && unit == other.unit;
		}

		return false;
	}
	
	@Override
	public String toString() {
		return marshall();
	}

	@Override
	public int compareTo(final Interval o) {
		final TimeUnit common = lowesCommonUnit(this, o);
		final long me = as(common);
		final long him = o.as(common);
		
		if (me < him) {
			return -1;
		} else if (me == him) {
			return 0;
		} else {
			return 1;
		}
	}

	private static TimeUnit lowesCommonUnit(Interval i1, Interval i2) {
		TimeUnit common = i1.getUnit();
		if (i2.getUnit().ordinal() < common.ordinal()) {
			common = i2.getUnit();
		}
		return common;
	}

}
