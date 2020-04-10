package jabot.jindex;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

public interface SearchTranslator {
	/** This method is for conveinience only */
	default Query translate(String search) throws ParseException {
		return translate(Parser.DEFAULT.parse(search));
	}
	
	Query translate (Query search);
}
