package jabot.jabot.commands;

import java.io.IOException;
import java.util.List;

import jabot.comcon.Cmd;
import jabot.comcon.CmdExecutor.PrioritizedCmd;
import jabot.comcon.ServiceCore;
import jabot.common.Texter;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;

public class CommandsCmd implements Cmd {
	private static final Texter TXT = new Texter();
	private String zeroPriorityHelp;

	@Override
	public String getName() {
		return "COMMANDS";
	}

	@Override
	public String getHelp() {
		return "Display help on all commands (you are reading it)";
	}

	@Override
	public void execute(ServiceCore core, Lobby lobby, ReceivedMessage message, List<String> args) throws IOException {
		int lobbyPriority = core.getCommandExecutor().getLobbyPriority(lobby);
		if (lobbyPriority == 0) {
			// Optimize default case
			if (zeroPriorityHelp == null) {
				// Multithreading may cause one recalculation per thread which is not bad in practice
				zeroPriorityHelp = createHelpForPrority(0, core);
			}
			lobby.post(zeroPriorityHelp);
		} else {
			String help = createHelpForPrority(lobbyPriority, core);
			lobby.post(help);
		}
	}

	private String createHelpForPrority(int lobbyPriority, ServiceCore core) {
		Texter.Builder help = TXT.build();
		help.append("Jabot commands:\n");
		for (final PrioritizedCmd prioritized : core.getCommandExecutor().listCommands()) {
			if (prioritized.getPriority()<=lobbyPriority) {
				displayHelp(help, prioritized.getCmd());
			}
		}
		return help.toString();
	}

	private void displayHelp(Texter.Builder help, Cmd cmd) {
		// SPACE AT FRONT IS IMPORTANT!
		// When debug mode is enabled, bot reacts to it's own messages.
		// This could potentially create endless feedback loops.
		help.append(" /").append(cmd.getName()).append("\t").append(cmd.getHelp()).append("\n");
	}

}
