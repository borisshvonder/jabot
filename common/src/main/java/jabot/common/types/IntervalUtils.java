package jabot.common.types;

import jabot.common.Texter;

public class IntervalUtils {
	private static final Texter TEXTER = new Texter();
	private static final Multiplier[] MULTIPLIERS = new Multiplier[] {
		new Multiplier(Interval.YEAR, "Y"),
		new Multiplier(Interval.DAY, "D"),
		new Multiplier(Interval.HOUR, "h"),
		new Multiplier(Interval.MINUTE, "m"),
		new Multiplier(Interval.SECOND, "s"),
		new Multiplier(1, "ms"),
	};
	
	/**
	 * Convert interval to human readable string. For example, 121MINUTES will be converted to "2hr21m" string 
	 * (if precision is 2 or higher) and 26521MINUTES will be converted to "3Y5D2h1m" if precision is 4 or more, or
	 * just "3Y5D" if precision is 2.
	 * 
	 * Maximum precision is milliseconds
	 */
	public static String humanReadable(final Interval interval, final int precision) {
		final Texter.Builder b = TEXTER.build();
		humanReadable(b, interval, precision);
		return b.toString();
	}

	/**
	 * Same as {@link #humanReadable(Interval, int)}, but writes into a Builder
	 * @param b
	 * @param interval
	 * @param precision
	 * @return
	 */
	public static void humanReadable(final Texter.Builder b, final Interval interval, final int precision) {
		long millis = interval.asMillis();
		if (millis == 0) {
			return;
		} else if (millis<0) {
			b.append('-');
			millis = -millis;
		} else {
			b.append('+');
		}
		
		int positionsLeft=precision;
		
		int i;
		for(i=0; i<MULTIPLIERS.length && positionsLeft>0 && millis>0; i++) {
			Multiplier m = MULTIPLIERS[i];
			long val = millis / m.multiplier;
			if (val > 0) {
				b.append(val).append(m.name);
				positionsLeft--;
				millis -= val*m.multiplier;
			}
		}
	}
	
	private static class Multiplier {
		private final long multiplier;
		private final String name;
		
		public Multiplier(final Interval interval, final String name) {
			this(interval.asMillis(), name);
		}
		
		public Multiplier(final long multiplier, final String name) {
			this.multiplier = multiplier;
			this.name = name;
		}
	}
}
