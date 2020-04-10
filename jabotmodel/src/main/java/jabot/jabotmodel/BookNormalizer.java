package jabot.jabotmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import jabot.common.Texter;

public class BookNormalizer {
	private static final int MAX_AUTHOR_LEN=256;
	private static final int MAX_TITLE_LEN=1024;
	private static final int MAX_AUTHORS=64;
	
	private static final Texter TEXTER = new Texter();
	private static final Pattern WHITESPACES = Pattern.compile("\\s+");
	// 47664595-54a9-4562-bc7b-a8d3cb007bb1
	
	private static final Pattern UUID = Pattern.compile(
			"[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}"
	);
	
	private static final Pattern UGLYDELIMETERS = Pattern.compile("([,.!]\\s[,.!])+");
	private static final Pattern SPACECOMMA = Pattern.compile("\\s([,.!])");
	private static final Pattern COMMASPACE_ATEND = Pattern.compile("\\s*,\\s*$");
	private static final Pattern DELIM_ATFRONT = Pattern.compile("^\\s*[,.!]\\s*");
	private static final Pattern HREFLINK = Pattern.compile("<a\\s?[^>]*>.*?</a>");
	private static final Pattern [] COMMONLINKS = new Pattern[]{
			Pattern.compile("http://\\S+"),
			Pattern.compile("https://\\S+"),
	};
	private static final Pattern TAGS = Pattern.compile("</?[^>]+>");
	private static String [] BAD_AUTHORS = new String [] {
		"неизвестен јвтор", "rusec lib_at_rus.ec", "неизвестен Автор", "Автор Неизвестен",
	};
	
	private static String [] BAD_AUTHOR_TOKENS = new String [] {
		"DIVERSUS", "LIBRUSEC", "RETROSHARE"
	};
	
	/**
	 * Fix common problems with books, remove incorrect authors and links
	 */
	public static void normalize(final Book book) {
		String title = book.getTitle();
		if (title != null) {
			title = normalizeTitle(title);
			book.setTitle(title);
		}
		title = book.getTitleRu();
		if (title != null) {
			title = normalizeTitle(title);
			book.setTitleRu(title);
		}

		book.setAuthors(sanitizeAuthors(book.getAuthors()));
		book.setAuthorsRu(sanitizeAuthors(book.getAuthorsRu()));
	}

	private static String normalizeTitle(final String title) {
		String ret = title;
		ret = cutoff(ret, MAX_TITLE_LEN);
		ret = removeLinks(ret);
		ret = removeTags(ret);
		ret = removeUUIDs(ret);
		ret = normalizeSpaces(ret);
		return ret;
	}

	private static List<String> sanitizeAuthors(final List<String> authors) {
		if (authors == null) {
			return null;
		} else {
			final List<String> ret = new ArrayList<>();
			
			final ListIterator<String> it = authors.listIterator();
			while(it.hasNext() && ret.size() < MAX_AUTHORS) {
				String author = it.next();
				if (author != null) {

					author = cutoff(author, MAX_AUTHOR_LEN);
					author = removeLinks(author);
					author = removeTags(author);
					author = removeUUIDs(author);
					author = removeKnownBadAuthors(author);
					author = normalizeSpaces(author);
					author = normalizeName(author);
					author = normalizeSpaces(author);
					
					if (!author.isEmpty()) {
						ret.add(author);
					}
				}
			}
			
			return ret;
		}
	}

	private static String cutoff(final String s, final int maxLen) {
		return s.length() > maxLen ? s.substring(0, maxLen) : s;
	}

	private static String removeLinks(final String text) {
		String ret = text;
		ret = HREFLINK.matcher(ret).replaceAll("");
		for (final Pattern pat : COMMONLINKS) {
			ret = pat.matcher(ret).replaceAll("");
		}
		return ret;
	}

	private static String removeTags(final String text) {
		String ret = text;
		ret = TAGS.matcher(ret).replaceAll("");
		return ret;
	}

	private static String removeUUIDs(final String text) {
		String ret = text;
		ret = UUID.matcher(ret).replaceAll("");
		return ret;
	}

	private static String removeKnownBadAuthors(final String author) {
		final String authorUppercase = author.toUpperCase();
		for (final String badToken : BAD_AUTHOR_TOKENS) {
			if (authorUppercase.indexOf(badToken)>=0) {
				return "";
			}
		}
		
		String ret = author;
		for (final String badAuthor : BAD_AUTHORS) {
			String fixed = removeSubstring(ret, badAuthor);
			final boolean wasChanged = fixed!=ret;
			if (wasChanged) {
				fixed = normalizeSpaces(fixed);
			}
			ret = fixed;
		}
		return ret;
	}

	private static String removeSubstring(final String text, final String subString) {
		int pos = text.indexOf(subString);
		if (pos>=0) {
			final Texter.Builder b = TEXTER.build();
			b.append(text.substring(0, pos));
			final int len = subString.length();
			
			int prev = pos+len;
			pos = text.indexOf(subString, prev);
			while (pos>=0) {
				b.append(text.substring(prev, pos));
				prev = pos+len;
				pos = text.indexOf(subString, prev);
			}
			b.append(text.substring(prev, text.length()));
			return b.toString();
		} else {
			return text;
		}
	}

	private static String normalizeSpaces(final String text) {
		String ret = text;
		ret = UGLYDELIMETERS.matcher(ret).replaceAll(". ");
		ret = WHITESPACES.matcher(ret).replaceAll(" ");
		ret = SPACECOMMA.matcher(ret).replaceAll("$1");
		ret = COMMASPACE_ATEND.matcher(ret).replaceAll("");
		ret = DELIM_ATFRONT.matcher(ret).replaceAll("");
		ret = ret.trim();
		return ret;
	}

	private static String normalizeName(final String author) {
		// fix LASTNAME, FIRSTNAME
		int comma = 0;
		for (;comma<author.length(); comma++) {
			final char c = author.charAt(comma);
			if (Character.isWhitespace(c)) {
				return author;
			} else if (c == ',') {
				break;
			}
		}

		int nonSpace = comma+1;
		while (nonSpace<author.length()) {
			final char c = author.charAt(nonSpace);
			if (!Character.isWhitespace(c)) {
				break;
			}
			nonSpace++;
		}
		
		if (nonSpace>=author.length()) {
			return author;
		} else {
			
			final Texter.Builder b = TEXTER.build();
			b.append(author.substring(nonSpace));
			b.append(' ');
			b.append(author.substring(0, comma));
			return b.toString();
		}
	}
}
