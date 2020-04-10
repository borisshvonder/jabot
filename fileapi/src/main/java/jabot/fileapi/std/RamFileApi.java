package jabot.fileapi.std;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.fileapi.FileAlreadyExistsException;
import jabot.fileapi.FileApi;
import jabot.fileapi.FileApiException;
import jabot.fileapi.FileNotFoundException;
import jabot.fileapi.InvalidPathException;
import jabot.fileapi.NotADirectoryException;

/**
 * Memory-backed {@link FileApi}
 */
public class RamFileApi implements FileApi {
	// IMPLEMENTATION NOTES
	//
	// note.001 java.util collections
	//          This implementation is designed primarily for unit tests and is not supposed to be scalable. Thus, for
	//          portability reasons, the java.util classes are preferred over realtime jalopy collection classes
	// 
	// note.002 extensive logging
	//          Since this class is used in unit tests, it is very log-intensive supporting the tester when discovering 
	//          bugs. Most logging is done on a TRACE level though.
	//
	// note.003 synchronization and locking
	//          For simplicity, all public methods that deal with shared state are synchronized
	
	private static final Logger LOG = LoggerFactory.getLogger(RamFileApi.class);
	private static final String SCHEME="testfs";
	private static final String SCHEME_WITH_SEP=SCHEME+":/";
	
	private static final InputStream DEFERRED_FAILING_NOT_A_DIRECTORY_untEAM = 
			new DeferredFailingInputStream("Can't read from directory");
	
	
	/** if true, the filesystem may produce very strange behaviors that is 100% up to the spec, but may cause clients
	 * that have incorrect assumptions to fail. For example, it may produce URIs that have double-dots "..' in them,
	 * may create phantom files on-fly (as if some other process has created them) and so on.
	 */
	private boolean stressTest = true;
	
	private final Map<String, Object> root = new HashMap<>();
	
	public boolean isStressTest() {
		return stressTest;
	}

	public void setStressTest(boolean stressTest) {
		this.stressTest = stressTest;
	}

	@Override
	public String getScheme() {
		return SCHEME;
	}

	@Override
	public synchronized boolean exists(final URI path) {
		logCall("exists", path);
		assertScheme(path);
		
		final Object target = findTarget(path);
		final boolean result = target != null;
		logResult(result, "exists", path);
		return result;
	}
	@Override
	public synchronized boolean isFile(final URI path) {
		logCall("isFile", path);
		assertScheme(path);
		
		final Object target = findTarget(path);
		final boolean result = target instanceof File;
		logResult(result, "isFile", path);
		return result;
	}

	@Override
	public synchronized boolean isDirectory(final URI path) {
		logCall("isDirectory", path);
		assertScheme(path);
		
		final Object target = findTarget(path);
		final boolean result = target instanceof Map;
		logResult(result, "isDirectory", path);
		return result;
	}

	@Override
	public synchronized Stream<URI> listDirectory(final URI path) throws NotADirectoryException, FileApiException {
		logCall("listDirectory", path);
		assertScheme(path);
		
		final Object target = findTarget(path);
		if (target instanceof Map) {
			final Map<?, ?> asMap = (Map<?, ?>)target;
			final Stream<URI> result = asMap.keySet().stream()
					.map(key -> join(path, key.toString()))
					.map(uri -> {logResult(uri, "listDirectory", path); return uri;});
			
			logResult(result, "listDirectory", path);
			return result;
		}
		throw logError(new NotADirectoryException(path, target+" is not a directory"), "listDirectory", 
				path);
	}

	@Override
	public synchronized void move(final URI source, final URI dest, boolean overwrite) throws FileApiException {
		logCall("move", source, dest, overwrite);
		assertScheme(source);
		assertScheme(dest);
		
		final Object sourceObj = findTarget(source);
		if (sourceObj == null) {
			throw logError(new FileApiException(source, "Does not exists"), "move", source, dest, overwrite);
		}

		final Object targetObj = findTarget(dest);
		if (sourceObj == targetObj) {
			return;
		}
		// Following is quite ineffective, but since it is a test code, performance is traded for simplicity
		if (targetObj != null) {
			
			if (!overwrite) {
				
				throw logError(new FileApiException(dest,"cannot overwrite "+dest), "move", source, 
						dest, overwrite);
				
			} else if (isDirectory(dest) && listDirectory(dest).findAny().isPresent()) {

				throw logError(new FileApiException(dest, "cannot overwrite non-empty "+dest), "move", 
						source, dest, overwrite);
				
			}
		}
		
		final URI dir = dirname(dest);
		final String name = basename(dest);
		final Object targetDir = findTarget(dir);
		if (targetDir instanceof Map) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> asMap = (Map<String, Object>)targetDir;
			LOG.info("moved {}->{}", source, dest);
			asMap.put(name, sourceObj);
			
			final Map<?, ?> sourceDir = (Map<?, ?>)findTarget(dirname(source));
			sourceDir.remove( basename(source));
		} else {
			throw new AssertionError(dir+" is supposed to be a directory, but it is not!");
		}
	}

	@Override
	public synchronized boolean remove(final URI path) throws FileApiException {
		logCall("remove", path);
		assertScheme(path);

		final URI dir = dirname(path);
		final String name = basename(path);
		final Object targetDir = findTarget(dir);
		if (targetDir instanceof Map) {
			final Map<?, ?> asMap = (Map<?, ?>) targetDir;
			Object targetObj = asMap.get(name);
			if (targetObj==null) {
				return false;
			} else if (targetObj instanceof Map && !((Map<?, ?>)targetObj).isEmpty()) {
				throw logError(new FileApiException(path, "Can't remove non-empty dir"), "remove", path);
			}
			return asMap.remove(name) != null;
		} else {
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized void mkdirs(final URI path) throws FileApiException {
		logCall("mkdirs", path);
		assertScheme(path);
		
		final List<String> components = split(path.getPath());
		
		Map<String, Object> parent = root;
		final StringBuilder breadcrumps = new StringBuilder(SCHEME_WITH_SEP.length() + path.getPath().length());
		breadcrumps.append(SCHEME_WITH_SEP);
		for (final String name : components) {
			breadcrumps.append('/');
			breadcrumps.append(name);
			Object target = parent.get(name);
			if (target == null) {
				target = new HashMap<String, Object>();
				parent.put(name, target);
			}

			if (target instanceof Map) {
				parent = (Map<String, Object>) target;
			} else {
				breadcrumps.append(" is not a directory");
				throw logError(new FileApiException(path, breadcrumps.toString()), "mkdirs", path);
			}
		}
		logResult(null, "mkdirs", path);
	}

	@Override
	public synchronized OutputStream createFile(final URI path) throws FileApiException, FileAlreadyExistsException {
		logCall("createFile", path);
		assertScheme(path);

		final URI dir = dirname(path);
		final String name = basename(path);
		final Object targetDir = findTarget(dir);
		if (targetDir instanceof Map) {
			
			@SuppressWarnings("unchecked")
			final Map<String, Object> asMap = (Map<String, Object>) targetDir;
			
			if (asMap.containsKey(name)) {
				throw logError(new FileAlreadyExistsException(path), "createFile", path);
			} else {
				final File newFile = new File();
				asMap.put(name, newFile);
				final OutputStream result = newFile.out();
				logResult(result, "createFile", path);
				return result;
			}
		} else {
			throw logError(new FileApiException(path, "folder "+dir+" does not exists"), "createFile", path);
		}
	}

	@Override
	public OutputStream appendFile(final URI path) throws FileApiException, FileNotFoundException {
		logCall("appendFile", path);
		assertScheme(path);
		
		final Object target = findTarget(path);
		if (target == null) {
			throw logError(new FileNotFoundException(path), "appendFile", path);
		} else if (target instanceof File) {
			final OutputStream result = ((File)target).out();
			logResult(result, "appendFile", path);
			return result;
		} else {
			throw logError(new FileApiException(path, "target is not a file"), "appendFile", path);
		}
	}

	@Override
	public InputStream readFile(final URI path) throws FileApiException, FileNotFoundException {
		logCall("readFile", path);
		
		final Object target = findTarget(path);
		if (target == null) {
			throw logError(new FileNotFoundException(path), "readFile", path);
		} else if (target instanceof File) {
			final InputStream result = ((File)target).in();
			logResult(result, "readFile", path);
			return result;
		} else {
			final InputStream result = DEFERRED_FAILING_NOT_A_DIRECTORY_untEAM;
			logResult(result, "readFile", path);
			return result;
		}
	}
	private Object findTarget(final URI path) {
		final List<String> components = split(path.getPath());
		Object ret = root;
		for (final String component : components) {
			if (!component.isEmpty()) {
				if (ret instanceof Map) {
					Map<?, ?> asMap = (Map<?, ?>) ret;
					ret = asMap.get(component);
				} else {
					// not found
					
				}
			}
		}
		return ret;
	}
	

	private void logCall(final String method, final Object ... params) {
		if (LOG.isInfoEnabled()) {
			final StringBuilder b = new StringBuilder();
			appendCall(b, method, params);
			LOG.info(b.toString());
		}
	}

	private void logResult(final Object result,final String method, final Object ... params) {
		if (LOG.isInfoEnabled()) {
			final StringBuilder b = new StringBuilder();
			appendCall(b, method, params);
			b.append("=>").append(result);
			LOG.info(b.toString());
		}
	}
	
	private void appendCall(final StringBuilder b, final String method, final Object... params) {
		b.append(method).append('(');
		boolean first = true;
		for (final Object param : params) {
			if (first) {
				first = false;
			} else {
				b.append(',');
			}
			b.append(param);
		}
		b.append(')');
	}
	
	private <T extends Throwable> T logError(T error, final String method, final Object... params) {
		if (LOG.isWarnEnabled()) {
			final StringBuilder b = new StringBuilder();
			appendCall(b, method, params);
			b.append(" !!! ").append(error);
			LOG.warn(b.toString());
		}
		return error;
	}
	
	private String basename(final URI uri) throws FileApiException {
		final String path = uri.getPath();
		final List<String> components = split(path);
		return components.get(components.size()-1);
	}
	
	private URI dirname(final URI uri) {
		final String path = uri.getPath();
		final List<String> components = split(path);
		final StringBuilder b = new StringBuilder(path.length()+SCHEME_WITH_SEP.length());
		b.append(SCHEME_WITH_SEP);
		boolean first = true;
		for (int i=0; i<components.size()-1; i++) {
			if (first) {
				first = false;
			} else {
				b.append('/');
			}
			b.append(components.get(i));
		}
		return URI.create(b.toString());
	}
	
	private List<String> split(final String path) {
		final List<String> ret = new LinkedList<>();
		
		int start = 0;
		int end = path.indexOf('/');
		while (end>=0){
			final String component = path.substring(start, end);
			if (!component.isEmpty()) {
				ret.add(component);
			}
			start = end + 1;
			end = path.indexOf('/', start);
		}
		ret.add(path.substring(start));
		return ret;
	}
	
	private URI join(final URI base, final String name) {
		final String baseStr = base.toString();
		return URI.create( baseStr.endsWith("/") ? baseStr+name : baseStr + "/" + name );
	}

	private void assertScheme(final URI path) {
		Validate.notNull(path, "path cannot be null");
		Validate.notBlank(path.getPath(), "path.getPath() cannot be blank");
		if (!SCHEME.equals(path.getScheme())) {
			throw logError(new InvalidPathException(path, "scheme not supported"), "assertScheme", path);
		}
	}
	
	private static class File {
		private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
		public OutputStream out() {
			return new OutView();
		}
		
		public InputStream in() {
			return new InView();
		}

		private final class OutView extends OutputStream {
			private final Thread boundTo = Thread.currentThread();
			private boolean closed = false;
			
			@Override
			public void write(final int b) throws IOException {
				assertValid();
				
				synchronized(stream) {
					stream.write(b);
				}
			}
	
			@Override
			public void write(final byte[] b) throws IOException {
				assertValid();
				
				synchronized(stream) {
					stream.write(b);
				}
			}
	
			@Override
			public void write(final byte[] b, final int off, final int len) throws IOException {
				assertValid();
				
				synchronized(stream) {
					stream.write(b, off, len);
				}
			}
	
			@Override
			public void flush() throws IOException {
				assertValid();
			}

			@Override
			public void close() throws IOException {
				assertThreadBound();
				closed = true;
			}
			
			private void assertValid() throws IOException {
				assertThreadBound();
				if (closed) {
					throw new IOException("Stream closed");
				}
			}
			
			private void assertThreadBound() {
				if (boundTo != Thread.currentThread()) {
					throw new AssertionError("Stream bound to thread "+boundTo+" but accessed from "+Thread.currentThread());
				}
			}
		
		}
		
		private final class InView extends InputStream {
			private final ByteArrayInputStream in;
			private final Thread boundTo = Thread.currentThread();
			private boolean closed = false;

			public InView() {
				synchronized(stream) {
					this.in = new ByteArrayInputStream(stream.toByteArray());
				}
			}

			@Override
			public int read() throws IOException {
				assertValid();
				
				return in.read();
			}

			@Override
			public int read(final byte[] b) throws IOException {
				assertValid();
				// TODO: Add more mangling
				return in.read(b);
			}

			@Override
			public int read(final byte[] b, final int off, final int len) throws IOException {
				assertValid();
				// TODO: Add more mangling
				return in.read(b, off, len);
			}

			@Override
			public long skip(long n) throws IOException {
				assertValid();
				
				return in.skip(n);
			}

			@Override
			public int available() throws IOException {
				assertValid();
				// TODO: Add more mangling
				return in.available();
			}

			@Override
			public void mark(final int readlimit) {
				try {
					assertValid();
				} catch (final IOException ex) {
					throw new UncheckedIOException(ex);
				}
				in.mark(readlimit);
			}

			@Override
			public void reset() throws IOException {
				assertValid();
				in.reset();
			}

			@Override
			public boolean markSupported() {
				assertThreadBound();
				return in.markSupported();
			}
			
			@Override
			public void close() throws IOException {
				assertThreadBound();
				closed = true;
			}

			private void assertValid() throws IOException {
				assertThreadBound();
				if (closed) {
					throw new IOException("Stream closed");
				}
			}
			
			private void assertThreadBound() {
				if (boundTo != Thread.currentThread()) {
					throw new AssertionError("Stream bound to thread "+boundTo+" but accessed from "+Thread.currentThread());
				}
			}
		}
	}
	
	private static final class DeferredFailingInputStream extends InputStream{
		private final String message;
		
		public DeferredFailingInputStream(String message) {
			this.message = message;
		}

		@Override
		public int read() throws IOException {
			throw new IOException(message);
		}
		
	}
}
