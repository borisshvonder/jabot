package jabot.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.common.cache.ExpiringCache;
import jabot.common.props.PropsConfig;
import jabot.common.types.Interval;
import jabot.fileapi.FileApi;
import jabot.fileapi.FileApiUtils;
import jabot.idxapi.DelayedIndexingException;
import jabot.jabotmodel.Book;
import jabot.jabotmodel.File;
import jabot.jabotmodel.Metafiler;
import jabot.jindex.CompositeJindex;
import jabot.jindex.Jindex;
import jabot.marshall.Marshall;
import jabot.metapi.ExtractionException;
import jabot.metapi.Metadata;
import jabot.metapi.NotableKeys;
import jabot.metapi.tools.MetadataSerializer;
import jabot.taskapi.Progress;
import jabot.taskapi.TaskContext;
import jabot.taskapi.TaskHandler;
import jabot.taskapi.TaskMemento;
import jabot.taskapi.TaskParams;

public class IngestTaskHandler implements TaskHandler<IngestTaskHandler.Params, IngestTaskHandler.Memento> {
	/** @visiblefortesting */
	static final String CONF_COMMITINTERVAL = IngestTaskHandler.class.getName()+".commitInterval";

	private static final Logger LOG = LoggerFactory.getLogger(IngestTaskHandler.class);
	private final PropsConfig config;
	private Metafiler metafiler;
	private Jindex jindex;
	private FileApi fileApi;
	
	public IngestTaskHandler(final PropsConfig config) {
		this.config = config;
	}

	public void setMetafiler(Metafiler metafiler) {
		this.metafiler = metafiler;
	}

	public void setJindex(Jindex jindex) {
		this.jindex = jindex;
	}

	public void setFileApi(FileApi fileApi) {
		this.fileApi = fileApi;
	}
	
	@Override
	public void handle(final TaskContext<Params, Memento> ctx) throws IOException, DelayedIndexingException {
		final Runner runner = new Runner(ctx);
		runner.run();
	}

	@Override
	public String marshallParams(final Params params) {
		return Marshall.get().toJson(params);
	}

	@Override
	public Params unmarshallParams(final String marshalled) {
		return Marshall.get().fromJson(marshalled, Params.class);
	}

	@Override
	public String marshallMemento(final Memento memento) {
		return Marshall.get().toJson(memento);
	}

	@Override
	public Memento unmarshallMemento(final String marshalled) {
		return Marshall.get().fromJson(marshalled, Memento.class);
	}	
	
	public static class Params implements TaskParams {
		/** Source to read filenames from, must be a file with list of files to ingest. File MUST be UTF-8 **/
		private URI list;
		
		/** List of languages the files are written in (optional)*/
		private List<String> languages;
		
		/** Solr component id to write to (optional) */
		private String backend;

		public URI getList() {
			return list;
		}

		public void setList(URI list) {
			this.list = list;
		}

		public String getBackend() {
			return backend;
		}

		public void setBackend(String backend) {
			this.backend = backend;
		}

		public List<String> getLanguages() {
			return languages;
		}

		public void setLanguages(List<String> languages) {
			this.languages = languages;
		}
	}
	
	public static class Memento implements TaskMemento {
		/** How many files were ingested already. Assumption is that list file does not change between runs */
		private long ingestedCount;

		public long getIngestedCount() {
			return ingestedCount;
		}

		public void setIngestedCount(long ingestedCount) {
			this.ingestedCount = ingestedCount;
		}
		
	}

	private class Runner {
		private final TaskContext<Params, Memento> ctx;
		private final Params params;
		private final ExpiringCache<Object> commitInterval;
		private final boolean russianSupport;
		private Memento memento;
		private Progress progress;
		private Jindex backend;

		public Runner(final TaskContext<Params, Memento> ctx) {
			this.ctx = ctx;
			this.params = ctx.getParams();
			this.memento = ctx.getMemento();
			this.russianSupport = params.getLanguages() != null && params.getLanguages().contains("rus");
			if (this.memento == null) {
				this.memento = new Memento();
			}
			this.progress = ctx.getProgress();
			
			final String configCommitInterval = config.getString(CONF_COMMITINTERVAL, "1MINUTE");
			this.commitInterval = new ExpiringCache<Object>(Interval.unmarshall(configCommitInterval));
			this.backend = resolveBackend(params.getBackend());
		}
		
		private Jindex resolveBackend(String componentId) {
			return componentId == null ? jindex : ((CompositeJindex)jindex).getComponent(componentId);
		}

		public void run() throws IOException, DelayedIndexingException {
			if (ctx.isAborted()) {
				return;
			}
			if (progress.getTotal() == 0) {
				setTotalLineCountFrom(params.getList());
			}
			
			progress = new Progress(0, progress.getTotal(), 0, 0);
			reportProgress();
			commitInterval.set(getClass());
			
			try(
				final InputStream in = fileApi.readFile(params.getList());
				final InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
				final BufferedReader lineReader = new BufferedReader(reader);
			) {
				long linesDone = 0;
				for(; linesDone < memento.getIngestedCount(); linesDone++) {
					if (ctx.isAborted()) {
						return;
					}
					lineReader.readLine();
					progress = progress.addCurrent(1);
					reportProgress();
					linesDone++;
				}
				String line = lineReader.readLine();
				while (line != null) {
					try {
						if (ctx.isAborted()) {
							return;
						}
						
						final URI uri = FileApiUtils.createURI(fileApi.getScheme(), line);
						
						ingest(uri);
						
					} catch (final RuntimeException | IOException | ExtractionException ex) {
						LOG.warn("Error ingesting file {}", line, ex);
						progress = progress.addFailed(1);
					}
					progress = progress.addCurrent(1);
					reportProgress();
					linesDone++;
					commitIfNeeded(linesDone);
					line = lineReader.readLine();
				}
				commit(linesDone);
			}
		}

		private void ingest(final URI uri) throws IOException, ExtractionException {
			Metadata meta = tryReadMetadata(URI.create(uri+".meta.txt"));
			if (meta == null) {
				meta = new Metadata();
			}
			
			if (params.getLanguages() != null) {
				meta.set(NotableKeys.LANGUAGES, params.getLanguages());
			}
			final File sample = metafiler.sample(uri, meta);
			if (russianSupport && sample instanceof Book) {
				sample.setFilenameRu(sample.getFilename());
				if (sample instanceof Book) {
					final Book asBook = (Book) sample;
					asBook.setTitleRu(asBook.getTitle());
					asBook.setAuthorsRu(asBook.getAuthors());
					
					
					asBook.setRawTextRu(asBook.getRawText());
					asBook.setRawText(null); // conserve index space
					
					asBook.setAnnotationRu(asBook.getAnnotation());
					asBook.setAnnotation(null); // conserve index space
				}
			}
			
			backend.store(sample.getSha1(), sample);
		}
		
		private Metadata tryReadMetadata(final URI metadataFile) {
			try {
				if (fileApi.isFile(metadataFile)) {
					try(final InputStream in = fileApi.readFile(metadataFile)) {
						return MetadataSerializer.read(in);
					}
				}
			} catch (final IOException ex) {
				LOG.warn("Can't read metadata from {}", metadataFile);
			}
			return null;
		}

		private void commitIfNeeded(long linesDone) throws DelayedIndexingException {
			if (commitInterval.isExpired()) {
				commit(linesDone);
			}
		}
		
		private void commit(long linesDone) throws DelayedIndexingException {
			backend.commit();
			memento.setIngestedCount(linesDone);
			ctx.setMemento(memento);
			commitInterval.set(getClass());
			
		}

		public void setTotalLineCountFrom(final URI file) throws IOException {
			long count = 0;
			try(
				final InputStream in = fileApi.readFile(file);
				final InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
				final BufferedReader lineReader = new BufferedReader(reader);
			) {
				String line = lineReader.readLine();
				while (line != null) {
					count++;
					line = lineReader.readLine();
				}
			}
			progress = new Progress(0, count, 0, 0);
			reportProgress();
		}

		private void reportProgress() {
			ctx.setProgress(progress);
		}
	}

}
