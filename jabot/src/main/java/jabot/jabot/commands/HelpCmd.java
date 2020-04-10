package jabot.jabot.commands;

import java.util.List;

import jabot.comcon.Cmd;
import jabot.comcon.ServiceCore;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;

public class HelpCmd implements Cmd {
	private static final String HELP = 
		"Welcome to library. /book command is used to search for books, for example:<br/>\n"+
		"/book whipping girl <br/>\n"+
		"will look for 'whipping' and 'girl' in all metadata fields (not book text)<br/>\n"+
		"/book rawText:(Trans Woman Manifesto) <br/>\n"+
		"will look in book text. You can do more finegrained searches like:<br/>\n"+
		"/book authors:(Jonathan Lynn) title:(yes minister) +fileType:fb2 offset:10<br/>\n"+
		"Metadata fields are: title, titleRu, authors, authorsRu, fileType, filename<br/>\n"+
		"Book text fields are: rawText, rawTextRu<br/>\n"+
		"offset field allows to return results from specified offset (paging)<br/>\n"+
		"All searches follow <a href=\"https://lucene.apache.org/core/2_9_4/queryparsersyntax.html\">"+
		    "Lucene syntax</a><br/>\n"+
		"To get help on other commands, type /commands";

	@Override
	public String getName() {
		return "HELP";
	}

	@Override
	public String getHelp() {
		return "Short intro on searching books";
	}

	@Override
	public void execute(
			final ServiceCore core, 
			final Lobby lobby, 
			final ReceivedMessage message, 
			final List<String> args
	) throws Exception {
		lobby.post(HELP);
	}

}
