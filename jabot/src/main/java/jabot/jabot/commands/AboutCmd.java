package jabot.jabot.commands;

import java.util.List;

import jabot.comcon.Cmd;
import jabot.comcon.ServiceCore;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;

public class AboutCmd implements Cmd {
	private static final String DISCLAIMER="I am non-profit non-commercial research tool dedicated for fulltext "+
	                                       "information retrieval. I'm NOT storing or sharing any files, books or "+
	                                       "any other copyrighted/non-copyrighted material. I am not responsible for "+
	                                       "people who use my links and hope they have good will and judgement.";

	@Override
	public String getName() {
		return "ABOUT";
	}

	@Override
	public String getHelp() {
		return "information about me";
	}

	@Override
	public void execute(
			final ServiceCore core, 
			final Lobby lobby, 
			final ReceivedMessage message, 
			final List<String> args
	) throws Exception {
		lobby.post(DISCLAIMER);
	}

}
