package jabot.rsrest2;

import org.junit.Test;

import java.util.Arrays;

import org.junit.Assert;

public class RSUtilsTest {
	@Test
	public void test_chopMessage() {
		Assert.assertEquals(Arrays.asList(""), RSUtils.chopMessage("", 100));
		Assert.assertEquals(Arrays.asList("hello"), RSUtils.chopMessage("hello", 100));
		Assert.assertEquals(Arrays.asList("hel", "lo"), RSUtils.chopMessage("hello", 3));
		Assert.assertEquals(Arrays.asList("Hello", "world"), RSUtils.chopMessage("Hello\nworld", 7));
		Assert.assertEquals(Arrays.asList("Hello", "world", "Goodbye", ", clown", "!"), RSUtils.chopMessage("Hello\nworld\nGoodbye, clown!", 7));
	}
}
