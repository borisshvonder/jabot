package jabot.metapi;

import java.util.Arrays;
import java.util.Collection;

public interface NotableBookKeys extends NotableFileKeys {
	/** List of authors */
	static final String AUTHORS = "Authors";
	
	static final String TITLE = "Title";
	
	static final String YEARS = "Years";
	
	static final Collection<String> ALTERNATIVE_TITLEKEYS = Arrays.asList("Title", "meta:title", "dc:title");

	static final Collection<String> ALTERNATIVE_AUTHORKEYS = Arrays.asList(
			"Author", "meta:author", "dc:author"
	);
}
