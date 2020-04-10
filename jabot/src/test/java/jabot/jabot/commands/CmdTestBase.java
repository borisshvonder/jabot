package jabot.jabot.commands;

import static org.mockito.Mockito.doNothing;

import java.util.Collections;

import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jabot.comcon.Cmd;
import jabot.comcon.CmdExecutor;
import jabot.comcon.ServiceCore;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;

public abstract class CmdTestBase {
	@Mock 
	Lobby lobby;
	
	@Mock
	ReceivedMessage message;
	
	CmdExecutor executor;
	
	protected ServiceCore core;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		
		core = new ServiceCore();
		executor = new CmdExecutor(core);
		core.setCommandExecutor(executor);
	}

	protected String executeAndCaptureOutput(Cmd fixture) throws Exception {
		ArgumentCaptor<String> posted = ArgumentCaptor.forClass(String.class);
		doNothing().when(lobby).post(posted.capture());
		execute(fixture);
		return posted.getValue();
	}

	protected void execute(Cmd fixture) throws Exception {
		fixture.execute(core, lobby, message, Collections.emptyList());
	}
}
