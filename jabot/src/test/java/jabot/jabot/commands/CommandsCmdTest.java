package jabot.jabot.commands;

import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CommandsCmdTest extends CmdTestBase {
	
	private CommandsCmd fixture;
	
	@Before
	public void setUp() {
		super.setUp();
		
		fixture = new CommandsCmd();
	}
	
	@Test
	public void testName() {
		Assert.assertEquals("COMMANDS", fixture.getName());
	}
	
	@Test
	public void testHelp() {
		Assert.assertNotNull(fixture.getHelp());
	}
	
	@Test
	public void testExecuteHelpWithHelpCommandOnly() throws Exception {
		executor.addCmd(0, fixture);
		
		Assert.assertTrue(executeAndCaptureOutput(fixture).indexOf("/COMMANDS")>=0);
		Assert.assertTrue(executeAndCaptureOutput(fixture).indexOf("/COMMANDS")>=0);
		verify(lobby, times(2)).post(notNull(String.class));
	}

	@Test
	public void testExecuteHelpWithinPriorityLobby() throws Exception {
		executor.setLobbyPriority(lobby, 1);
		executor.addCmd(0, fixture);
		
		Assert.assertTrue(executeAndCaptureOutput(fixture).indexOf("/COMMANDS")>=0);
		Assert.assertTrue(executeAndCaptureOutput(fixture).indexOf("/COMMANDS")>=0);
		verify(lobby, times(2)).post(notNull(String.class));
	}
	
	@Test
	public void testExecuteWillNotShowPriorityCommand() throws Exception {
		executor.addCmd(1, fixture);
		
		Assert.assertTrue(executeAndCaptureOutput(fixture).indexOf("/COMMANDS")<0);
	}

}
