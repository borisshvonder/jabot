package jabot.metika;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import jabot.metapi.Metadata;
import jabot.metapi.NotableKeys;
import javolution.text.Text;
import javolution.text.TextBuilder;

class ExtractTextTikaHandler implements ContentHandler {
	private final Metadata meta;
	private final TextBuilder text = new TextBuilder();
	private boolean delimiterRequired = false;
	
	public ExtractTextTikaHandler(final Metadata meta) {
		this.meta = meta;
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
		if (length > 0) {
			if (delimiterRequired) {
				text.append(' ');
			}
			text.append(Text.valueOf(ch, start, length));
		}
	}
	
	@Override
	public void endDocument() throws SAXException {
		meta.add(NotableKeys.EXTRACTED_TEXT, text.toString());
		text.clear();
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		delimit();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		delimit();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		delimit();
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		delimit();
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		delimit();
	}

	@Override
	public void setDocumentLocator(Locator locator) {}

	@Override
	public void startDocument() throws SAXException {}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {}
	
	private void delimit() {
		delimiterRequired = text.length() > 0;
	}
}
