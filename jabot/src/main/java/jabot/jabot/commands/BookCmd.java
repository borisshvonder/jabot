package jabot.jabot.commands;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.comcon.Cmd;
import jabot.comcon.ServiceCore;
import jabot.common.Texter;
import jabot.jabotmodel.Book;
import jabot.jabotmodel.BookNormalizer;
import jabot.jabotmodel.TextNormalizer;
import jabot.jindex.JIndexResults;
import jabot.jindex.Parser;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;
import javolution.text.TextBuilder;

public class BookCmd implements Cmd {
	private static final Map<String, String> FIELDTYPE_CONVERSION = buildMap(
			"fb2", "application/x-fictionbook+xml",
			"pdf", "application/pdf",
			"djvu", "image/vnd.djvu"
		);
	private static final Map<String, String> FIELDTYPE_BACKWARDCONVERSION = reverseMap(FIELDTYPE_CONVERSION);
	
	private static final String[][] NORMALIZATIONS = new String[][] {
		new String[] {"\\+\\+", "\\\\+\\\\+"},
	};
	
	private static final Logger LOG = LoggerFactory.getLogger(BookCmd.class);
	private static final int TOP_RESULTS = 10;
	private static final int MAX_FETCH = 100;
	private static final Random NAMEGEN = new Random();
	static {
		new Book(); // make sure all marshalling is initialized
	}
	private final Texter texter = new Texter();
	
	@Override
	public String getName() {
		return "BOOK";
	}

	@Override
	public String getHelp() {
		return " query : find books using simple query";
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
			b.append(normalizeQueryTerm(q));
		}
		
		final String queryAsString = b.toString();
		
		try {
			final Query query = Parser.DEFAULT.parse(queryAsString);
			b.clear();
			
			final QueryRewriter rewriter = new QueryRewriter(query);
			final Query rewritten = rewriter.rewrite();
	
			if(query instanceof BooleanQuery && ((BooleanQuery)query).clauses().isEmpty()) {
				b.append("EMPTY QUERY");
			} else {
				try(final JIndexResults<Book> results = 
						core.getJindex().search(Book.class, rewritten, rewriter.getOffset())
				) {
					final Collection<ResultWithVariants> output;
					if (rewriter.variants) {
						output = fetchWithVariants(results);
					} else {
						output = deduplicate(results);
					}
					
					if (output.isEmpty()) {
						b.append("NO RESULTS");
					} else {
						b.append(query.toString()).append(" RESULTS:<br/>");
						for (final ResultWithVariants var : output) {
							formatResult(b, var);
						}
						if (results.next() != null) {
							b.append(results.estimateTotalResults() -TOP_RESULTS -rewriter.getOffset());
							b.append(" MORE");
						}
					}
				}
	
			}
		} catch (final ParseException ex) {
			b.clear();
			b.append("<a href=\"https://lucene.apache.org/core/2_9_4/queryparsersyntax.html\">");
			b.append("Lucene query</a> syntax error: ");
			b.append(ex.getMessage());
		} catch (final Exception ex) {
			LOG.warn("Error executing query {}", queryAsString, ex);
			b.clear();
			b.append(ex.getClass().getName()).append('\n');
			b.append(ex.getMessage());
		}
		lobby.post(b.toString());
	}
	
	/** @visiblefortesting */
	static String normalizeQueryTerm(String term) {
		String q = term;
		if (q == null || q.isEmpty()) {
			return "";
		}
		for (String [] normalization : NORMALIZATIONS) {
			q = q.replaceAll(normalization[0], normalization[1]);
		}
		return q;
	}

	private Collection<ResultWithVariants> fetchWithVariants(JIndexResults<Book> results) {
		final LinkedList<ResultWithVariants> ret = new LinkedList<>();
		Book book = results.next();
		while (book != null && ret.size() < TOP_RESULTS) {
			BookNormalizer.normalize(book);
			ret.add(new ResultWithVariants(book));
			book = results.next();
		}
		return ret;
	}

	private void formatResult(final Texter.Builder b, final ResultWithVariants result) 
				throws UnsupportedEncodingException 
	{
		final Book book = result.getResult();
		String title = book.getTitle();
		if (title == null || title.isEmpty()) {
			title = book.getFilename();
		}
		if (title == null || title.isEmpty()) {
			title = "UNTITLED_"+NAMEGEN.nextInt(Integer.MAX_VALUE);
		}
		
		String filename = title;
		filename = TextNormalizer.maximumNormalization(title);
		filename = TextNormalizer.transliterateFastToEnglish(filename);
		if (filename.isEmpty()) {
			filename = "FILE_"+NAMEGEN.nextInt(Integer.MAX_VALUE);
		}
		
		final String fileExt = getFileExt(book);
		
		b.append("<a href=\"retroshare://file?name=").append(filename);
		if (!filename.endsWith(fileExt)) {
			b.append(fileExt);
		}
		b.append("&size=").append(book.getLength());
		b.append("&hash=").append(book.getSha1().getText());
		b.append("\">");
		
		b.append(title);
		
		b.append("</a>");
		
		final List<String> authors = book.getAuthors();
		if (authors != null && !authors.isEmpty()) {
			for (final String author : book.getAuthors()) {
				b.append(", ").append(author);
			}
		}
		
		if (result.getVariants() > 0) {
			b.append(" (").append(result.getVariants()+1).append(" variants)");
		}
		
		b.append("<br/>\n");
	}
	
	private String getFileExt(final Book book) {
		final String ext = FIELDTYPE_BACKWARDCONVERSION.get(book.getContentType());
		if (ext != null) {
			return "."+ext;
		} else {
			final String filename = book.getFilename();
			if (filename == null) {
				return "";
			} else {
				int lastDot = filename.lastIndexOf('.');
				if (lastDot<0) {
					return "";
				}
				String ret = filename.substring(lastDot+1);
				ret = TextNormalizer.maximumNormalization(ret);
				ret = TextNormalizer.transliterateFastToEnglish(ret);
				ret = "." + ret;
				return ret;
			}
		}
	}

	/**
	 * @visiblefortesting
	 */
	static Collection<ResultWithVariants> deduplicate(final JIndexResults<Book> results) {
		final Map<String, List<ResultWithVariants>> booksByTitle = new LinkedHashMap<>(TOP_RESULTS);
		Book book = results.next();
		int fetched = 1;
		int outputResults = 0;
		while (book != null && fetched<MAX_FETCH && outputResults<TOP_RESULTS) {
			BookNormalizer.normalize(book);
			String title = book.getTitle();
			if (title == null) {
				title = book.getFilename();
			}
			title = normalizeString(title);
			
			List<ResultWithVariants> bucket = booksByTitle.get(title);
			if (bucket == null) {
				bucket = new LinkedList<>();
				bucket.add(new ResultWithVariants(book));
				booksByTitle.put(title, bucket);
				outputResults++;
			} else {
				final ResultWithVariants similar = getSimilarResult(bucket, book);
				if (similar == null) {
					bucket.add(new ResultWithVariants(book));
					outputResults++;
				} else {
					similar.addVariant();
				}
			}
			
			book = results.next();
			fetched++;
		}
		
		final List<ResultWithVariants> tally = new ArrayList<>(outputResults);
		for (final List<ResultWithVariants> variants : booksByTitle.values()) {
			tally.addAll(variants);
		}
		
		return tally;
	}
	
	private static ResultWithVariants getSimilarResult(final Collection<ResultWithVariants> bucket, final Book book) {
		for (final ResultWithVariants var : bucket) {
			if (similarBook(var, book)) {
				return var;
			}
		}
		return null;
	}

	private static boolean similarBook(final ResultWithVariants var, final Book book) {
		final boolean fileSizeMatches = var.getResult().getLength() > 0 && var.getResult().getLength() == book.getLength();
		if (fileSizeMatches) {
			return true;
		}
		
		final List<String> authors = book.getAuthors();
		
		final Set<String> firstAuthor = authors == null || authors.isEmpty() ? Collections.emptySet() 
				: tokenizeString(authors.get(0));
		
		if (!firstAuthor.isEmpty() && var.getFirstAuthorNormalized().equals(firstAuthor)) {
			return true;
		}
		
		return false;
	}

	/**
	 * @visiblefortesting
	 */
	static String normalizeString(final String text) {
		if (text == null) {
			return "";
		}
		final TextBuilder b = new TextBuilder(text.length());
		boolean delimeter = false;
		for (int i=0; i<text.length(); i++) {
			final char c = text.charAt(i);
			if (Character.isAlphabetic(c) || Character.isDigit(c)) {
				delimeter = false;
				b.append(Character.toUpperCase(c));
			} else if (!delimeter && i<text.length()-1) {
				delimeter = true;
				if (i>0) {
					b.append(' ');
				}
			}
		}
		if (b.length()>0 && b.charAt(b.length()-1) == ' ') {
			// trim last space
			b.delete(b.length()-1, b.length());
		}
		
		return b.toString();
	}
	
	/**
	 * @visiblefortesting
	 */
	static Set<String> tokenizeString(final String text) {
		final Set<String> ret = new HashSet<>();
		if (text == null) {
			return ret;
		}
		final TextBuilder b = new TextBuilder(text.length());
		
		boolean delimeter = false;
		for (int i=0; i<text.length(); i++) {
			final char c = text.charAt(i);
			if (Character.isAlphabetic(c) || Character.isDigit(c)) {
				delimeter = false;
				b.append(Character.toUpperCase(c));
			} else if (!delimeter && i<text.length()-1) {
				delimeter = true;
				if (i>0) {
					ret.add(b.toString());
					b.clear();
				}
			}
		}
		
		if (b.length()>0) {
			ret.add(b.toString());
		}
		return ret;
	}

	static class ResultWithVariants {
		private final Book result;
		private final Set<String> firstAuthorNormalized;
		private int variants;
		
		public ResultWithVariants(final Book result) {
			Validate.notNull(result);

			this.result = result;
			final List<String> authors = result.getAuthors();
			
			this.firstAuthorNormalized = authors == null || authors.isEmpty() ? Collections.emptySet() 
					: tokenizeString(authors.get(0));
		}

		public int getVariants() {
			return variants;
		}

		public Book getResult() {
			return result;
		}

		public Set<String> getFirstAuthorNormalized() {
			return firstAuthorNormalized;
		}

		public void addVariant() {
			variants++;
		}
		
	}
	
	private static Map<String, String> buildMap(final String ... keyValues) {
		Validate.isTrue(keyValues.length % 2 == 0, "keyValues length should be even");
		final int len = keyValues.length / 2;
		final Map<String, String> ret = new HashMap<>(len);
		for (int i=0; i<len; i++) {
			final int k = 2*i;
			ret.put(keyValues[k].trim().toLowerCase(), keyValues[k+1]);
		}
		return ret;
	}
	
	private static Map<String, String> reverseMap(final Map<String, String> map) {
		final Map<String, String> ret = new HashMap<>(map.size());
		for (final Map.Entry<String, String> e : map.entrySet()) {
			final String existing = ret.put(e.getValue(), e.getKey());
			Validate.isTrue(existing==null, "Cannot reverse map: duplicated key {}", e.getValue());
		}
		return ret;
	}
	/**
	 * Rewrites query and extracts special parameters
	 *
	 */
	private static final class QueryRewriter {
		private final Query original;
		private boolean variants;
		private int offset;

		public QueryRewriter(final Query original) {
			this.original = original;
		}
		
		public int getOffset() {
			return offset;
		}

		public Query rewrite() {
			if (original instanceof BooleanQuery) {
				return translateBooleanQuery((BooleanQuery)original);
			}
				return original;
			}

		private BooleanQuery translateBooleanQuery(final BooleanQuery q) {
			final BooleanQuery.Builder b = new BooleanQuery.Builder();
			for (final BooleanClause clause : q.clauses()) {
				if (clause.getQuery() instanceof TermQuery) {
					final TermQuery tq = (TermQuery)clause.getQuery();
					final String fieldName = tq.getTerm().field();
					switch(fieldName) {
					case "variants": variants = Boolean.parseBoolean(tq.getTerm().text()); break;
					case "fileType": b.add(convertFileType(clause)); break;
					case "offset": offset = parseOffset(tq.getTerm().text()); break;
					default: b.add(clause); break;
					}
				} else {
					b.add(clause);
				}
			}
			return b.build();
		}

		private int parseOffset(String offset) {
			if (offset == null) {
				return 0;
			} else {
				try {
					return Integer.parseInt(offset.trim());
				} catch (NumberFormatException ex) {
					LOG.debug("Can't convert to number {} ", offset, ex);
					return 0;
				}
			}
		}

		private BooleanClause convertFileType(final BooleanClause clause) {
			final TermQuery tq = (TermQuery)clause.getQuery();
			final Term term = tq.getTerm();
			String termValue = term.text();
			if (termValue != null) {
				termValue = termValue.trim().toLowerCase();
			}
			final String converted = FIELDTYPE_CONVERSION.get(termValue);
			final Term convertedTerm = new Term("contentType", converted == null ? term.bytes() : new BytesRef(converted));
			final PhraseQuery.Builder convertedQuery = new PhraseQuery.Builder();
			convertedQuery.add(convertedTerm);
			return new BooleanClause(convertedQuery.build(), clause.getOccur());
		}

	}
}
