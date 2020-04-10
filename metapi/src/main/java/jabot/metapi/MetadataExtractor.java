package jabot.metapi;

import java.io.IOException;
import java.io.InputStream;

public interface MetadataExtractor {
	
	/**
	 * Extract as much metadata as possible from a stream
	 * @param @notnull in input stream
	 * @param @notnull meta (possibly empty) metadata to enrich. Allows passing in metadata that is already known from 
	 *                 the context (for ex. filename, url, filetype, ...)
	 */
	void extractMetadata(InputStream in, Metadata meta) throws IOException, ExtractionException;
}
