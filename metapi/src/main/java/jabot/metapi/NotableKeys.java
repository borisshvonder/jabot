package jabot.metapi;

/** Notable {@link Metadata} keys that don't fall other categories */
public interface NotableKeys {
	static final String CONTENT_TYPE = "Content-Type";
	static final String MEDIA_SUBTYPE = "Media-Subtype";
	static final String CONTENT_ENCODING = "Content-Encoding";
	static final String CONTENT_LENTGH = "Content-Length";
	static final String LANGUAGES = "Languages"; //ISO 639-2 code (rus, eng, ger, ...)
	
	/** String url */
	static final String URL = "URL";
	
	/** very important metadata key - the text extracted from the data, useful for building full-text search index */
	static final String EXTRACTED_TEXT="Extracted-Text";
}
