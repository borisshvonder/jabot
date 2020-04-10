package jabot.common.math;

import org.junit.Test;

import org.junit.Assert;

public class JMathTest {
	@Test
	public void test_addWithoutOverflow_basic() {
		Assert.assertEquals(100L, JMath.addWithoutOverflow(10L, 90L));
		Assert.assertEquals(-100L, JMath.addWithoutOverflow(-10L, -90L));
		Assert.assertEquals(100L, JMath.addWithoutOverflow(-100L, 200L));
		Assert.assertEquals(-100L, JMath.addWithoutOverflow(200L, -300L));
	}
	
	@Test
	public void test_addWithoutOverflow_overflow() {
		Assert.assertEquals(Long.MAX_VALUE, JMath.addWithoutOverflow(Long.MAX_VALUE, 0L));
		Assert.assertEquals(Long.MAX_VALUE, JMath.addWithoutOverflow(0L, Long.MAX_VALUE));
		Assert.assertEquals(Long.MAX_VALUE, JMath.addWithoutOverflow(300L, Long.MAX_VALUE-10L));
		Assert.assertEquals(Long.MAX_VALUE, JMath.addWithoutOverflow(Long.MAX_VALUE-10L, 1000L));
		Assert.assertEquals(Long.MAX_VALUE, JMath.addWithoutOverflow(Long.MAX_VALUE, Long.MAX_VALUE-10L));
		Assert.assertEquals(Long.MAX_VALUE, JMath.addWithoutOverflow(Long.MAX_VALUE-10L, Long.MAX_VALUE));
		Assert.assertEquals(Long.MAX_VALUE, JMath.addWithoutOverflow(Long.MAX_VALUE, Long.MAX_VALUE));
	}

	@Test
	public void test_addWithoutOverflow_underflow() {
		Assert.assertEquals(Long.MIN_VALUE, JMath.addWithoutOverflow(Long.MIN_VALUE, 0L));
		Assert.assertEquals(Long.MIN_VALUE, JMath.addWithoutOverflow(0L, Long.MIN_VALUE));
		Assert.assertEquals(Long.MIN_VALUE, JMath.addWithoutOverflow(-300L, Long.MIN_VALUE+10L));
		Assert.assertEquals(Long.MIN_VALUE, JMath.addWithoutOverflow(Long.MIN_VALUE+10L, -1000L));
		Assert.assertEquals(Long.MIN_VALUE, JMath.addWithoutOverflow(Long.MIN_VALUE, Long.MIN_VALUE+10L));
		Assert.assertEquals(Long.MIN_VALUE, JMath.addWithoutOverflow(Long.MIN_VALUE+10L, Long.MIN_VALUE));
		Assert.assertEquals(Long.MIN_VALUE, JMath.addWithoutOverflow(Long.MIN_VALUE, Long.MIN_VALUE));
	}
}
