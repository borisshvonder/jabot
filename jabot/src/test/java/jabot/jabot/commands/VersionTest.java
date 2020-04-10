package jabot.jabot.commands;

import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VersionTest extends CmdTestBase {
	
	private VersionCmd fixture;
	
	@Before
	public void setUp() {
		super.setUp();
		
		fixture = new VersionCmd();
	}
	
	@Test
	public void testName() {
		Assert.assertEquals("VERSION", fixture.getName());
	}
	
	@Test
	public void testHelp() {
		Assert.assertNotNull(fixture.getHelp());
	}
	
	@Test
	public void testExecuteHelpWithHelpCommandOnly() throws Exception {
		Assert.assertNotNull(executeAndCaptureOutput(fixture));
		Assert.assertNotNull(executeAndCaptureOutput(fixture));
		verify(lobby, times(2)).post(notNull(String.class));
	}

}
