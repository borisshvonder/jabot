package jabot.jabotmodel;

import org.junit.Test;

import org.junit.Assert;

public class TextNormalizerTest {

	@Test
	public void test_transliterateFastToEnglish_english() {
		Assert.assertEquals("", TextNormalizer.transliterateFastToEnglish(""));
		Assert.assertEquals("hello", TextNormalizer.transliterateFastToEnglish("hello"));
		Assert.assertEquals("hello, everybody", TextNormalizer.transliterateFastToEnglish("hello, everybody"));
	}
	
	@Test
	public void test_transliterateFastToEnglish_russian() {
		Assert.assertEquals("privet", TextNormalizer.transliterateFastToEnglish("привет"));
		Assert.assertEquals("privet, medved", TextNormalizer.transliterateFastToEnglish("привет, медved"));
	}
	
	
	@Test
	public void test_maximumNormalization() {
		Assert.assertEquals("", TextNormalizer.maximumNormalization(""));
		Assert.assertEquals("hello", TextNormalizer.maximumNormalization("hello"));
		Assert.assertEquals("hello world", TextNormalizer.maximumNormalization("hello world"));
		Assert.assertEquals("hello world", TextNormalizer.maximumNormalization("   hello    world   "));
		Assert.assertEquals("hell0 world.9", TextNormalizer.maximumNormalization(" !!!  hell0  !!  world.9--   +"));
	}
}
