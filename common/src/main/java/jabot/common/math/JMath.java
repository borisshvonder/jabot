package jabot.common.math;

public final class JMath {
	/**
	 * Returns the sum of its arguments without overflow. If sum is too big to
	 * fit in long, return {@value Long#MAX_VALUE}. If sum is too small to fit
	 * in Long, return {@value Long#MIN_VALUE}
	 *
	 * @param x
	 *            the first value
	 * @param y
	 *            the second value
	 * @return the result
	 */
	public static long addWithoutOverflow(long x, long y) {
		long r = x + y;
		// HD 2-12 Overflow iff both arguments have the opposite sign of the
		// result
		if (((x ^ r) & (y ^ r)) < 0) {
			return (x>0 || y>0) ? Long.MAX_VALUE : Long.MIN_VALUE;
		}
		return r;
	}
	
	private JMath() {}
}
