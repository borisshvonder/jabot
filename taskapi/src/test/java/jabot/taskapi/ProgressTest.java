package jabot.taskapi;

import org.junit.Test;

import org.junit.Assert;

public class ProgressTest {
	@Test
	public void testMashall() {
		final Progress progress = new Progress(1, 2, 3, 4);
		Assert.assertEquals("1/2/3/4", progress.marshall());
	}
	
	@Test
	public void testUnmarshall() {
		final Progress progress = Progress.unmarshall("1/2/3/4");
		Assert.assertEquals(new Progress(1, 2, 3, 4), progress);
	}
}
