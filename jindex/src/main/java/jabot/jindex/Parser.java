package jabot.jindex;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import javolution.text.TextBuilder;

public final class Parser {
	public static final Parser DEFAULT = new Parser("metadata");
	private static final String UNSAFE_CHARS="?!";
	private static final CharArraySet STOPWORDS;
	static {
		try {
			final List<String> words = new LinkedList<>();
			try(final InputStream in = Parser.class.getResourceAsStream("/jindex/stopwords.txt")) {
				final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				String line = reader.readLine();
				while (line != null) {
					line = line.trim();
					if (!line.startsWith("#")) {
						words.add(line);
					}
					line = reader.readLine();
				}
			}
			STOPWORDS = new CharArraySet(words, true);
		} catch (final Exception ex) {
			throw new AssertionError(ex);
		}
	}
	private static final StandardAnalyzer ANALYZER = new StandardAnalyzer(STOPWORDS);
	public final String defaultField;
	
	public Parser(final String defaultField) {
		Validate.notNull(defaultField, "defaultField cannot be null");
		
		this.defaultField = defaultField;
	}
	
	public String getDefaultField() {
		return defaultField;
	}

	public Query parse(final String query) throws ParseException {
		// TODO : use threadlocals or something.
		
		final QueryParser parser =  new QueryParser(defaultField, ANALYZER);
		final String normalized = normalize(query);
		return parser.parse(normalized);
	}
	
	public static String normalize(final String query) {
		final TextBuilder b = new TextBuilder(query.length());
		b.append(query);

		boolean modified = false;
		for(int i=0; i<b.length();) {
			final char c = b.charAt(i);
			if (UNSAFE_CHARS.indexOf(c)>=0) {
				b.setCharAt(i, ' ');
				modified = true;
			} else {
				i++;
			}
		}
		
		return modified ? b.toString() : query;
	}

	public String toString(final Query query) {
		return query.toString(defaultField);
	}
	
}
