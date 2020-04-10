package jabot.jabot.commands;

import java.util.List;

import jabot.comcon.Cmd;
import jabot.comcon.ServiceCore;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;

public class StopCmd implements Cmd {

	@Override
	public String getName() {
		return "JABOT:STOP";
	}

	@Override
	public String getHelp() {
		return "Stop Jabbot";
	}

	@Override
	public void execute(ServiceCore core, Lobby lobby, ReceivedMessage message, List<String> args) throws Exception {
		core.setStopRequested(true);
	}

}
