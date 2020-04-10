package jabot.jabot.commands;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StopCmdTest extends CmdTestBase {
	
	private StopCmd fixture;
	
	@Before
	public void setUp() {
		super.setUp();
		
		fixture = new StopCmd();
	}
	
	@Test
	public void testName() {
		Assert.assertEquals("JABOT:STOP", fixture.getName());
	}
	
	@Test
	public void testHelp() {
		Assert.assertNotNull(fixture.getHelp());
	}
	
	@Test
	public void testExecuteHelpWithHelpCommandOnly() throws Exception {
		execute(fixture);
		Assert.assertTrue(core.isStopRequested());
	}

}