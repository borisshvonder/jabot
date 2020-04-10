package jabot.metika.fb2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import org.ccil.cowan.tagsoup.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import jabot.metapi.ExtractionException;
import jabot.metapi.Metadata;
import jabot.metapi.MetadataExtractor;
import jabot.metapi.NotableBookKeys;
import jabot.metapi.NotableKeys;
import javolution.text.CharSet;
import javolution.text.Text;
import javolution.text.TextBuilder;

public class Fb2Extractor implements MetadataExtractor {
	private static final Logger LOG = LoggerFactory.getLogger(Fb2Extractor.class);

	@Override
	public void extractMetadata(final InputStream in, final Metadata meta) throws IOException, ExtractionException {
		final Parser parser = new Parser();
		final Fb2Handler handler = new Fb2Handler(meta);
		parser.setContentHandler(handler);

		final InputSource source = determineBestSource(in, meta);
		try {
			parser.parse(source);
		} catch (final SAXException ex) {
			throw new ExtractionException(ex);
		}
	}

	private InputSource determineBestSource(final InputStream in, final Metadata meta) {
		Charset charset = tryDetermineCharset(meta);
		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}
		return new InputSource(new InputStreamReader(in, charset));
	}

	private Charset tryDetermineCharset(final Metadata meta) {
		final String encoding = meta.getSingle(NotableKeys.CONTENT_ENCODING);
		if (encoding == null) {
			return null;
		}
		final int charsetStart = encoding.indexOf(';');
		final String charsetName = encoding.substring(charsetStart+1).trim();
		try {
			return Charset.forName(charsetName);
		} catch (final UnsupportedCharsetException ex) {
			LOG.debug("Unknown charset {}", charsetName, ex);
			return null;
		}
	}

	private static final class Fb2Handler implements ContentHandler {
		private final Metadata meta;
		private final TextBuilder text = new TextBuilder();
		private final TextBuilder authorExtra = new TextBuilder();
		private final TextBuilder firstName = new TextBuilder();
		private final TextBuilder middleName = new TextBuilder();
		private final TextBuilder lastName = new TextBuilder();
		private final TextBuilder title = new TextBuilder();
		private final TextBuilder year = new TextBuilder();
		private static enum State { TEXT, AUTHOR, FIRST_NAME, MIDDLE_NAME, LAST_NAME, TITLE, BINARY, YEAR}
		private State state = State.TEXT;
		private boolean delimeterRequired = false;
		
		public Fb2Handler(final Metadata meta) {
			this.meta = meta;
		}
		
		@Override
		public void endDocument() throws SAXException {
			trim(text);
			addMeta(text, NotableKeys.EXTRACTED_TEXT);
			text.clear();
		}

		@Override
		public void startElement(final String uri, final String localName, final String qName, final Attributes atts) 
				throws SAXException 
		{
			delimeterRequired = true;
			switch(state) {
			
			case TEXT: 
				if ("author".equals(localName)) {
					state = State.AUTHOR;
				} else if ("book-title".equals(localName)) {
					state = State.TITLE;
				} else if ("binary".equals(localName)) {
					state = State.BINARY;
				} else if ("year".equals(localName)) {
					state = State.YEAR;
				} else {
					// Still reading text
				}
				break;

			case AUTHOR: 
				if ("first-name".equals(localName)) {
					state = State.FIRST_NAME;
				} else if ("middle-name".equals(localName)) {
					state = State.MIDDLE_NAME;
				} else if ("last-name".equals(localName)) {
					state = State.LAST_NAME;
				} else {
					// Still reading author
				}
				break;

			case TITLE: 
			case FIRST_NAME:
			case MIDDLE_NAME:
			case LAST_NAME:
			case BINARY:
			case YEAR:
				// Still reading
				break;
			
			default: throw new AssertionError("Unknown state "+state);
			}
		}
		
		@Override
		public void endElement(final String uri, final String localName, final String qName) throws SAXException {
			delimeterRequired = true;
			
			switch(state) {
			
			case TEXT: 
				// Still reading text
				break;

			case AUTHOR: 
				if ("author".equals(localName)) {
					storeAuthor();
					state = State.TEXT;
				} else {
					// Still reading author
				}
				break;

			case TITLE: 
				if ("book-title".equals(localName)) {
					storeTitle();
					state = State.TEXT;
					
				} else {
					// Still reading title
				}
				break;
				
			case FIRST_NAME:
				if ("first-name".equals(localName)) {
					state = State.AUTHOR;
				} else {
					// Still reading first-name 
				}
				break;
				
				
			case MIDDLE_NAME:
				if ("middle-name".equals(localName)) {
					state = State.AUTHOR;
				} else {
					// Still reading middle-name 
				}
				break;
				
			case LAST_NAME:
				if ("last-name".equals(localName)) {
					state = State.AUTHOR;
				} else {
					// Still reading last-name
				}
				break;
				
			case BINARY:
				if ("binary".equals(localName)) {
					state = State.TEXT;
				} else {
					// Still reading binary
				}
				break;
				
			case YEAR:
				if ("year".equals(localName)) {
					storeYear();
					state = State.TEXT;
				} else {
					// Still reading binary
				}
				break;
			
			default: throw new AssertionError("Unknown state "+state);
			}
		}

		@Override
		public void characters(final char[] ch, final int start, final int length) throws SAXException {
			switch(state) {
			
			case TEXT: append(text, ch, start, length); break;
			case AUTHOR: append(authorExtra, ch, start, length); break;
			case TITLE: append(title, ch, start, length); break;
			case FIRST_NAME: append(firstName, ch, start, length); break;
			case MIDDLE_NAME: append(middleName, ch, start, length); break;
			case LAST_NAME: append(lastName, ch, start, length); break;
			case BINARY: /* IGNORE CHARS */ break;
			case YEAR: append(year, ch, start, length); break;
			
			default: throw new AssertionError("Unknown state "+state);
			}
		}


		private void append(final TextBuilder builder, final char[] ch, final int start, final int length) {
			Text text = Text.valueOf(ch, start, length);
			text = text.replace(CharSet.WHITESPACES, " ");
			text = text.trim();
			if (text.length()>0) {
				if (delimeterRequired) {
					final int currentLength = builder.length();
					if (currentLength >0 && !Character.isWhitespace(builder.charAt(currentLength-1))) {
						builder.append(' ');
					}
					delimeterRequired = false;
				}
				builder.append(text);
			}
		}

		private void storeAuthor() {
			trim(firstName);
			trim(middleName);
			trim(lastName);
			trim(authorExtra);
			
			concat(middleName, firstName);
			concat(lastName, firstName);
			if (authorExtra.length()>0) {
				if (firstName.length()>0) {
					firstName.append(',');
				}
				firstName.append(authorExtra);
			}
			
			addMeta(firstName, NotableBookKeys.AUTHORS);
			
			firstName.clear();
			middleName.clear();
			lastName.clear();
			authorExtra.clear();
		}

		private void storeTitle() {
			trim(title);
			addMeta(title, NotableBookKeys.TITLE);
			title.clear();
		}
		
		private void storeYear() {
			trim(year);
			addMeta(year, NotableBookKeys.YEARS);
			year.clear();
		}
		
		private void concat(final TextBuilder source, final TextBuilder dest) {
			if (source.length()>0) {
				if (dest.length()>0) {
					dest.append(' ');
				}
				dest.append(source);
			}
		}

		private void addMeta(final TextBuilder source, final String key) {
			if (source.length()>0) {
				meta.add(key, source.toString());
			}
		}

		private void trim(final TextBuilder source) {
			int trimEnd = source.length()-1;
			while (trimEnd>=0 && Character.isWhitespace(source.charAt(trimEnd))) {
				trimEnd--;
			}
			source.delete(trimEnd+1, source.length());
			
			int trimStart = 0;
			while (trimStart<source.length() && Character.isWhitespace(source.charAt(trimStart))) {
				trimStart++;
			}
			source.delete(0, trimStart);
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			delimeterRequired = true;
		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {
			delimeterRequired = true;
		}

		@Override
		public void setDocumentLocator(Locator locator) {}

		@Override
		public void startDocument() throws SAXException {}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {}

		@Override
		public void skippedEntity(String name) throws SAXException {}
		
	}
}
