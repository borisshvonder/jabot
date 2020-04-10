package jabot.jabot.commands;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.Validate;

import jabot.comcon.Cmd;
import jabot.comcon.ServiceCore;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;
import jabot.taskapi.TaskId;
import jabot.taskapi.Tasker;

public class KillCmd implements Cmd {
	@Override
	public String getName() {
		return "KILL";
	}

	@Override
	public String getHelp() {
		return " moniker : kill task";
	}

	@Override
	public void execute(
			final ServiceCore core, 
			final Lobby lobby, 
			final ReceivedMessage message, 
			final List<String> args
	) throws IOException {
		Validate.isTrue(args.size() == 1, "Expected 1 argument, got: {}", args);
		
		final String moniker = args.get(0);
		final Tasker tasker = core.getTasker();
		final TaskId id = tasker.findTask(moniker);
		if (id == null) {
			lobby.post("Task not found");
		} else {
			tasker.removeTask(moniker);
			lobby.post("Task killed");
		}
	}
}
