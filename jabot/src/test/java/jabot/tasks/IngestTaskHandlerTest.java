package jabot.tasks;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import jabot.fileapi.FileApi;
import jabot.fileapi.std.RamFileApi;
import jabot.idxapi.Untokenized;
import jabot.jabotmodel.File;
import jabot.jabotmodel.Metafiler;
import jabot.jindex.CompositeJindex;
import jabot.jindex.Jindex;
import jabot.metapi.Metadata;
import jabot.taskapi.TaskHandlerId;
import jabot.taskri.testsupport.TaskerTestHarness;
import jabot.tasks.IngestTaskHandler.Memento;
import jabot.tasks.IngestTaskHandler.Params;
import org.junit.Assert;

public class IngestTaskHandlerTest {
	
	@Mock Metafiler metafiler;
	@Mock CompositeJindex jindex;
	
	private TaskerTestHarness th;
	private IngestTaskHandler fixture;
	private TaskHandlerId handler;
	private Params sampleParams;
	private Memento memento;
	private FileApi fileApi;
	
	@Before
	public synchronized void setUp() {
		MockitoAnnotations.initMocks(this);
		
		th = new TaskerTestHarness();
		fileApi = new RamFileApi();
		
		fixture = new IngestTaskHandler(th.getPropsConfig());
		fixture.setFileApi(fileApi);
		fixture.setMetafiler(metafiler);
		fixture.setJindex(jindex);
		
		handler = th.getTasker().registerHandler(fixture);
		
		sampleParams = new Params();
		sampleParams.setList(URI.create("testfs:/list.txt"));
		memento = new Memento();
		th.startup();
	}

	@After
	public synchronized void killAllTasks() throws Exception {
		th.shutdown();
	}
	
	@Test
	public void test_params_and_memento() {
		th.assertParamsMarshallingSupported(fixture, sampleParams);
		th.assertMementoMarshallingSupported(fixture, memento);
	}
	
	@Test
	public void testIngestSingleFile() throws Exception {
		final URI fileToIngest = URI.create("testfs:/file.txt");
		writeLinesToFile(sampleParams.getList(), fileToIngest.toString());
		
		final File sampled = new File();
		sampled.setSha1(new Untokenized("aabb"));
		
		when(metafiler.sample(eq(fileToIngest), any())).thenReturn(sampled);
		
		th.runOnce(handler, sampleParams);
		
		verify(jindex).store(sampled.getSha1(), sampled);
		verify(jindex, atLeastOnce()).commit();
	}
	
	@Test
	public void test_existing_metadata() throws Exception {
		final URI fileToIngest = URI.create("testfs:/file.txt");
		writeLinesToFile(sampleParams.getList(), fileToIngest.toString());
		
		final URI metadataFile = URI.create("testfs:/file.txt.meta.txt");
		writeLinesToFile(metadataFile, "Title=great book", "Authors=[a1,a2]");
		
		final File sampled = new File();
		sampled.setSha1(new Untokenized("aabb"));
		
		final ArgumentCaptor<Metadata> meta = ArgumentCaptor.forClass(Metadata.class);
		when(metafiler.sample(eq(fileToIngest), meta.capture())).thenReturn(sampled);
		
		th.runOnce(handler, sampleParams);
		
		verify(jindex).store(sampled.getSha1(), sampled);
		verify(jindex, atLeastOnce()).commit();
		Assert.assertEquals("great book", meta.getValue().getSingle("Title"));
		Assert.assertEquals(Arrays.asList("a1", "a2"), meta.getValue().get("Authors"));
	}
	
	@Test
	public void testIngestToBackend() throws Exception {
		final URI fileToIngest = URI.create("testfs:/file.txt");
		writeLinesToFile(sampleParams.getList(), fileToIngest.toString());
		sampleParams.setBackend("backend1");
		final Jindex backend1 = Mockito.mock(Jindex.class);
		when(jindex.getComponent("backend1")).thenReturn(backend1);
		
		final File sampled = new File();
		sampled.setSha1(new Untokenized("aabb"));
		
		when(metafiler.sample(eq(fileToIngest), any())).thenReturn(sampled);
		
		th.runOnce(handler, sampleParams);
		
		verify(backend1).store(sampled.getSha1(), sampled);
		verify(backend1, atLeastOnce()).commit();
	}
	
	@Test
	public void testRestartIngestion() throws Exception {
		final URI fileToIngest1 = URI.create("testfs:/file1.txt");
		final URI fileToIngest2 = URI.create("testfs:/file2.txt");
		writeLinesToFile(sampleParams.getList(), fileToIngest1.toString(), fileToIngest2.getPath().toString());
		
		final File sampled = new File();
		sampled.setSha1(new Untokenized("aabb"));
		
		memento.setIngestedCount(1);
		when(metafiler.sample(eq(fileToIngest2), any())).thenReturn(sampled);
		
		th.runOnce(handler, sampleParams, memento);
		
		verify(jindex).store(sampled.getSha1(), sampled);
		verify(jindex, atLeastOnce()).commit();
	}
	
	@Test
	public void testForceCommit() throws Exception {
		th.getProps().setProperty(IngestTaskHandler.CONF_COMMITINTERVAL, "0HOURS");

		writeLinesToFile(sampleParams.getList(), "missing_file");
		
		th.runOnce(handler, sampleParams, memento);
		verify(jindex, times(2)).commit();
	}
	
	private void writeLinesToFile(final URI file, final String ... lines) throws IOException {
		try(
				Writer writer = new OutputStreamWriter(fileApi.createFile(file), StandardCharsets.UTF_8);
				PrintWriter pw = new PrintWriter(writer)
		) {
			for (final String line : lines) {
				pw.println(line);
			}
		}
		
	}
}
