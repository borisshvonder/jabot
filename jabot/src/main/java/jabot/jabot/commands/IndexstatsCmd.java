package jabot.jabot.commands;

import java.util.List;

import jabot.comcon.Cmd;
import jabot.comcon.ServiceCore;
import jabot.common.Texter;
import jabot.idxapi.SearchResults;
import jabot.idxsolr.SolrIndex;
import jabot.idxsolr.SolrIndexManager;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;

public class IndexstatsCmd implements Cmd {
	private static final Texter TXT = new Texter();

	@Override
	public String getName() {
		return "INDEXSTATS";
	}

	@Override
	public String getHelp() {
		return "Display index statistics";
	}

	@Override
	public void execute(
			final ServiceCore core, 
			final Lobby lobby, 
			final ReceivedMessage message, 
			final List<String> args
	) throws Exception {
		final SolrIndexManager mgr = core.getSolrManager();
		final Texter.Builder b = TXT.build();
		b.append("Index statistics:\n");
		for (final String component: mgr.listComponents()) {
			b.append(component).append(": ").append(countRecords(mgr.getIndex(component))).append("\n");
		}
		lobby.post(b.toString());
	}

	private String countRecords(final SolrIndex index) {
		try {
			try(final SearchResults results = index.search("*:*", 0)) {
				return String.valueOf(results.estimateTotalResults());
			}
		} catch (final Exception ex) {
			return ex.toString();
		}
	}

}
