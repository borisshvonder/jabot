package jabot.jabot.commands;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.lucene.search.Query;

import jabot.comcon.Cmd;
import jabot.comcon.ServiceCore;
import jabot.common.Texter;
import jabot.jabotmodel.File;
import jabot.jindex.JIndexResults;
import jabot.jindex.Parser;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;

public class FileCmd implements Cmd {
	private final int TOP_RESULTS = 10;
	static {
		new File(); // make sure all marshalling is initialized
	}
	private final Texter texter = new Texter();
	
	@Override
	public String getName() {
		return "FILE";
	}

	@Override
	public String getHelp() {
		return " query : find files using simple query";
	}
	
	@Override
	public void execute(
			final ServiceCore core, 
			final Lobby lobby, 
			final ReceivedMessage message, 
			final List<String> args
	) throws Exception {
		Validate.isTrue(!args.isEmpty(), "query required");

		final Texter.Builder b = texter.build();
		for (final String q : args) {
			if (b.length() > 0) {
				b.append(' ');
			}
			b.append(q);
		}
		final Query query = Parser.DEFAULT.parse(b.toString());
		
		b.clear();
		try(final JIndexResults<File> results = core.getJindex().search(File.class, query, 0)) {
			File file = results.next();
			if (file == null) {
				b.append("NO RESULTS");
			} else {
				b.append(query.toString()).append(" RESULTS:<br/>");
				for(int count=0; count<TOP_RESULTS && file != null; count++) {
					formatResult(b, file);
					file = results.next();
				}
				if (results.next() != null) {
					b.append(results.estimateTotalResults() - TOP_RESULTS);
					b.append(" MORE");
				}
			}
		}

		lobby.post(b.toString());
	}

	private void formatResult(final Texter.Builder b, final File file) throws UnsupportedEncodingException {
		b.append("<a href=\"retroshare://file?name=").append(URLEncoder.encode(file.getFilename(), "UTF-8"));
		b.append("&size=").append(file.getLength());
		b.append("&hash=").append(file.getSha1().getText());
		b.append("\">");
		
		b.append(file.getFilename());
		
		b.append("</a><br/>");
	}
}
