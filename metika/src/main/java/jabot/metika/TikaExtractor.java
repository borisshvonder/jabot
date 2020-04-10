package jabot.metika;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.parser.txt.UniversalEncodingDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import jabot.common.bytes.Bts;
import jabot.metapi.ExtractionException;
import jabot.metapi.MetadataExtractor;
import jabot.metapi.NotableFileKeys;
import jabot.metapi.NotableHashes;
import jabot.metapi.NotableKeys;
import jabot.metapi.tools.HashedStream;
import jabot.metika.fb2.Fb2Extractor;

public class TikaExtractor implements MetadataExtractor {
	private static final Logger LOG = LoggerFactory.getLogger(TikaExtractor.class);
	private static final int DEFAULT_MAX_BUFFER = 32 * 1024 * 1024; //32Mb
	private static final Detector DETECTOR;
	private static final Parser PARSER;
	static {
		final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
		DETECTOR = tikaConfig.getDetector();
		PARSER = tikaConfig.getParser();
	}
	private static final Map<String, MetadataExtractor> SPECIALIZED_EXTRACTORS;
	static {
		SPECIALIZED_EXTRACTORS = new HashMap<>(1);
		SPECIALIZED_EXTRACTORS.put("application/x-fictionbook+xml", new Fb2Extractor());
	}
	private int maxBuffer = DEFAULT_MAX_BUFFER;
	
	public int getMaxBuffer() {
		return maxBuffer;
	}

	public void setMaxBuffer(int maxBuffer) {
		this.maxBuffer = maxBuffer;
	}

	@Override
	public void extractMetadata(final InputStream in, final jabot.metapi.Metadata meta) 
			throws IOException, ExtractionException 
	{
		Validate.notNull(in, "in cannot be null");
		Validate.notNull(meta, "meta cannot be null");
		
		final Metadata tikadata = translateFromApiMeta(meta);

		try {
			final HashedStream sha1 = new HashedStream(in, "SHA1");
			final InputStream markSupported = new BufferedInputStream(sha1);

			ensureCharsetDetected(markSupported, tikadata);

			markSupported.mark(maxBuffer);
			final MediaType mediaType = DETECTOR.detect(markSupported, tikadata);
			final String contentType = mediaType.toString();
			tikadata.set(Metadata.CONTENT_TYPE, contentType.toString());
			markSupported.reset();
			
			final MetadataExtractor specialized = SPECIALIZED_EXTRACTORS.get(contentType);
			if (specialized == null) {
				try {
					extractUsingTika(tikadata, meta, markSupported, true);
				} catch (Exception ex) {
					LOG.warn("Cannot extract with OCR/tessearct, retrying without it", ex);
					extractUsingTika(tikadata, meta, markSupported, false);
				}
			} else {
				translateMetaToApi(tikadata, meta);
				specialized.extractMetadata(markSupported, meta);
			}
			translateMetaToApi(tikadata, meta);
			meta.set(NotableHashes.SHA1, Bts.hex(sha1.hash()));
			meta.set(NotableHashes.CONTENT_LENTGH, String.valueOf(sha1.getLength()));
		} catch (final NoSuchAlgorithmException ex) {
			throw new ExtractionException(ex);
		}
	}

	private void ensureCharsetDetected(final InputStream in, final Metadata tikadata) throws IOException {
		if (tikadata.get(Metadata.CONTENT_ENCODING) == null) {

			in.mark(maxBuffer);
			final UniversalEncodingDetector detector = new UniversalEncodingDetector();
			final Charset charset = detector.detect(in, tikadata);
			in.reset();

			if (charset != null) {
				tikadata.set(Metadata.CONTENT_ENCODING, charset.name());
			}
		}
		
	}

	private void extractUsingTika(
			final Metadata tikadata, 
			final jabot.metapi.Metadata meta, 
			final InputStream markSupported,
			final boolean withTesseract
	) throws IOException, ExtractionException 
	{
		ParseContext parseContext = new ParseContext();
		parseContext.set(Parser.class, PARSER);

		if (withTesseract) {
			PDFParserConfig pdfConfig = new PDFParserConfig();
			pdfConfig.setExtractAnnotationText(true);
			pdfConfig.setExtractInlineImages(true);
			pdfConfig.setExtractUniqueInlineImagesOnly(true);	
			parseContext.set(PDFParserConfig.class, pdfConfig);
			
			final TesseractOCRConfig tesseractConfig = new TesseractOCRConfig();
			tesseractConfig.setLanguage(toTesseractLanguage(meta.get(NotableKeys.LANGUAGES)));
			parseContext.set(TesseractOCRConfig.class, tesseractConfig);
		}
		
		final ExtractTextTikaHandler textExtractor = new ExtractTextTikaHandler(meta);
		try {
			PARSER.parse(markSupported, textExtractor, tikadata, parseContext);
		} catch (final SAXException | TikaException ex) {
			throw new ExtractionException(ex);
		}
	}

	private String toTesseractLanguage(final List<String> list) {
		if (list == null || list.isEmpty()) {
			return "eng";
		}
		final StringBuilder ret = new StringBuilder(list.size()*4);
		for (String lang : list) {
			if (lang != null) {
				lang = lang.trim();
				if (!lang.isEmpty()) {
					if(ret.length()>0) {
						ret.append('+');
					}
					ret.append(lang);
				}
			}
		}
		return ret.toString();
	}

	private Metadata translateFromApiMeta(final jabot.metapi.Metadata meta) {
		final Metadata ret = new Metadata();
		
		ret.set(TikaMetadataKeys.RESOURCE_NAME_KEY, meta.getSingle(NotableFileKeys.FILENAME));
		return ret;
	}

	private void translateMetaToApi(final Metadata tikadata, final jabot.metapi.Metadata meta) {
		for (final String name : tikadata.names()) {
			final String [] values = tikadata.getValues(name);
			meta.set(name, values == null ? null : Arrays.asList(values));
		}
	}
}
