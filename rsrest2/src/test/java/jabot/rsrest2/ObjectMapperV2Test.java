package jabot.rsrest2;

import org.junit.Assert;
import org.junit.Test;

public class ObjectMapperV2Test {
	@Test
	public void testInitialized() {
		Assert.assertNotNull(ObjectMapperV2.getMapper());
	}
}
