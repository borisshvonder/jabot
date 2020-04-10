package jabot.metapi.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.metapi.ExtractionException;
import jabot.metapi.Metadata;
import jabot.metapi.MetadataExtractor;
import jabot.metapi.NotableFileKeys;
import jabot.metapi.NotableKeys;
import jabot.metapi.tools.MetadataSerializer;

public abstract class MetadataExtractorAcceptanceTestBase {

	protected abstract MetadataExtractor createFixture();
	protected abstract void destroyFixture(MetadataExtractor fixture);

	private static final String META_EXT = ".meta";
	private static final Logger LOG = LoggerFactory.getLogger(MetadataExtractorAcceptanceTestBase.class);
	
	private static final Reflections SAMPLES_DISCOVERY;
	static {
		final ConfigurationBuilder b = ConfigurationBuilder.build();
		b.forPackages(MetadataExtractorAcceptanceTestBase.class.getPackage().getName());
		b.addScanners(new ResourcesScanner());
		SAMPLES_DISCOVERY = new Reflections(b);
	}

	private MetadataExtractor fixture;

	@Before
	public void setUp() {
		fixture = createFixture();
	}

	@After
	public void tearDown() {
		destroyFixture(fixture);
	}

	@Test
	public void testExtractFromEmptyStream() throws IOException, ExtractionException {
		final Metadata meta = new Metadata();
		fixture.extractMetadata(new ByteArrayInputStream(new byte[0]), meta);
		Assert.assertEquals(Arrays.asList("application/octet-stream"), meta.get(NotableKeys.CONTENT_TYPE));
		Assert.assertEquals("0", meta.getSingle(NotableKeys.CONTENT_LENTGH));
	}
	
	@Test
	public void testSamples() throws IOException, ExtractionException {
		final List<String> samples = discoverSamples();
		Collections.sort(samples);
		final List<String> errors = new LinkedList<>();
		for (final String sample : samples) {
			LOG.trace("Starting sample {}", sample);
			testSample(errors, sample);
			LOG.trace("Done with sample {}", sample);
		}
		if (errors.size() > 0) {
			Assert.fail(errors.toString());
		}
	}

	private void testSample(final List<String> errors, final String sample) throws IOException, ExtractionException {
		final Metadata expectedMeta = readMetadata(sample+META_EXT);
		
		final Metadata actualMeta = new Metadata();
		actualMeta.add(NotableFileKeys.FILENAME, basename(sample));
		actualMeta.add(NotableKeys.URL, "classpath:/"+sample);
		try(final InputStream in = getResourceAsStream(sample)) {
			fixture.extractMetadata(in, actualMeta);
		}
		
		for (final Map.Entry<String, List<String>> expected : expectedMeta.entrySet()) {
			if (!expected.getKey().equals("Extracted-Text_atLeast")) {
				final List<String> actual = actualMeta.get(expected.getKey());
				if (!expected.getValue().equals(actual)) {
					LOG.warn("Value mismatch in sample {}, key:{}, expected:{}, actual:{}", new Object[]{
							sample, expected.getKey(), expected.getValue(), actual
					});
					errors.add("Sample "+sample+" key "+expected.getKey()+" mismatch");
				}
			}
		}
		
		final String extractedTextAtLeast = expectedMeta.getSingle("Extracted-Text_atLeast");
		if (extractedTextAtLeast!=null) {
			final int atLeast = Integer.parseInt(extractedTextAtLeast);
			final String actualText = actualMeta.getSingle(NotableKeys.EXTRACTED_TEXT);
			if (actualText == null || actualText.length() < atLeast) {
				LOG.warn("Extracted text incorrect in sample {}, expected:at least {} chars, actual:{}", new Object[]{
						sample, atLeast, actualText
				});
				errors.add("Sample "+sample+" has incorrectly extracted text");
			}
		}
	}

	private String basename(final String path) {
		int lastSep = path.lastIndexOf('/');
		return path.substring(lastSep+1);
	}

	private Metadata readMetadata(final String path) throws IOException {
		try(final InputStream in = getResourceAsStream(path)) {
			return MetadataSerializer.read(in);
		}
	}

	private List<String> discoverSamples() {
		final Set<String> metaFiles = SAMPLES_DISCOVERY.getResources(Pattern.compile(".*\\"+META_EXT));
		final List<String> result = new ArrayList<String>(metaFiles.size());
		for (final String metaFile : metaFiles) {
			result.add(metaFile.substring(0, metaFile.length()-META_EXT.length()));
		}
		return result;
	}
	

	private InputStream getResourceAsStream(String resource) {
		final InputStream in = getContextClassLoader().getResourceAsStream(resource);

		return in == null ? getClass().getResourceAsStream(resource) : in;
	}

	private ClassLoader getContextClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

}
