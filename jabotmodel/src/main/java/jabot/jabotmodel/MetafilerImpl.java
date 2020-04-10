package jabot.jabotmodel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.idxapi.Untokenized;
import jabot.metapi.ExtractionException;
import jabot.metapi.Metadata;
import jabot.metapi.MetadataExtractor;
import jabot.metapi.NotableBookKeys;
import jabot.metapi.NotableFileKeys;
import jabot.metapi.NotableHashes;
import jabot.metapi.NotableKeys;

public class MetafilerImpl implements Metafiler {
	private static final Logger LOG = LoggerFactory.getLogger(MetafilerImpl.class);
	private static final int MAX_ANNOTATION_LENGTH = 1024;

	private static final Collection<String> BOOK_CONTENT_TYPES=Arrays.asList(
		"application/x-fictionbook+xml",
		"application/pdf",
		"image/vnd.djvu"
	);
			
	private final MetadataExtractor extractor;

	public MetafilerImpl(MetadataExtractor extractor) {
		Validate.notNull(extractor, "extractor cannot be null");
		
		this.extractor = extractor;
	}
	
	/* (non-Javadoc)
	 * @see jabot.jabotmodel.Metafiler#sample(java.net.URI)
	 */
	@Override
	public File sample(final URI target, final Metadata meta) throws IOException, ExtractionException {
		Validate.notNull(target, "target cannot be null");
		
		meta.set(NotableFileKeys.URL, target.toString());
		meta.set(NotableFileKeys.FILENAME, filename(target));
		
		try(final InputStream in = target.toURL().openStream()) {
			extractor.extractMetadata(in, meta);
		}
		
		String contentType = meta.getSingle(NotableKeys.CONTENT_TYPE);
		if (contentType == null) {
			contentType = "";
		}
		
		final File result;
		if (BOOK_CONTENT_TYPES.contains(contentType)) {
			result = makeBook(meta);
		} else {
			result = new File();
		}
		
		result.setContentType(contentType);
		result.setFilename(meta.getSingle(NotableFileKeys.FILENAME));
		result.setLocation(target);
		result.setRawText(meta.getSingle(NotableKeys.EXTRACTED_TEXT));
		result.setSha1(new Untokenized(meta.getSingle(NotableHashes.SHA1)));
		result.setLength(Long.parseLong(meta.getSingle(NotableHashes.CONTENT_LENTGH)));
		result.setContentEncoding(meta.getSingle(NotableHashes.CONTENT_ENCODING));

		return result;
	}

	private File makeBook(final Metadata meta) {
		final Book book = new Book();
		book.setTitle(meta.getSingle(NotableBookKeys.TITLE));
		if (book.getTitle() == null) {
			book.setTitle(extractFirstAlternativeKey(meta, NotableBookKeys.ALTERNATIVE_TITLEKEYS));
		}
		book.setAuthors(meta.get(NotableBookKeys.AUTHORS));
		if (book.getAuthors() == null) {
			book.setAuthors(extractAllAlternativeKeys(meta, NotableBookKeys.ALTERNATIVE_AUTHORKEYS));
		}
		//TODO : year, isbn
		
		String annotation = meta.getSingle(NotableKeys.EXTRACTED_TEXT);
		if (annotation != null) {
			if (annotation.length() > MAX_ANNOTATION_LENGTH) {
				annotation = annotation.substring(0, MAX_ANNOTATION_LENGTH);
			}
			book.setAnnotation(annotation);
		}
		
		final List<String> years = meta.get(NotableBookKeys.YEARS);
		if (years != null) {
			final List<Integer> parsedYears = new ArrayList<>(years.size());
			for (final String year : years) {
				try {
					int parsed = Integer.parseInt(year);
					parsedYears.add(parsed);
				} catch (final NumberFormatException ex) {
					LOG.info("Can't parse year {}", year, ex);
				}
			}
			book.setYears(parsedYears);
		}

		BookNormalizer.normalize(book);
		return book;
	}
	
	private List<String> extractAllAlternativeKeys(final Metadata meta, final Collection<String> alternativeKeys) {
		final List<String> ret = new LinkedList<>();
		for (final String key : alternativeKeys) {
			final List<String> vals = meta.get(key);
			if (vals != null) {
				for (String v : vals) {
					if (v != null) {
						v = v.trim();
						if (!v.isEmpty() && !ret.contains(v)) {
							ret.add(v);
						}
					}
				}
			}
		}
		return ret;
	}

	private String extractFirstAlternativeKey(final Metadata meta, final Collection<String> alternativeKeys) {
		for (final String key : alternativeKeys) {
			final List<String> vals = meta.get(key);
			if (vals != null) {
				for (String v : vals) {
					if (v != null) {
						v = v.trim();
						if (!v.isEmpty()) {
							return v;
						}
					}
				}
			}
		}
		return null;
	}

	private String filename(final URI target) {
		final java.io.File javaFile = new java.io.File(target.getPath());
		return javaFile.getName();
	}

}
