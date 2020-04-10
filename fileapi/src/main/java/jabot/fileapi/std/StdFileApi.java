package jabot.fileapi.std;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;

import jabot.common.Texter;
import jabot.fileapi.FileAlreadyExistsException;
import jabot.fileapi.FileApi;
import jabot.fileapi.FileApiException;
import jabot.fileapi.FileNotFoundException;
import jabot.fileapi.InvalidPathException;
import jabot.fileapi.NotADirectoryException;

/**
 * File API backed by {@link java.nio.FileSystem}. Default constructor uses "file" scheme and default local filesystem.
 */
public class StdFileApi implements FileApi {
	private final FileSystem backend;
	private final String scheme;
	
	public StdFileApi(final FileSystem backend, final String scheme) {
		this.backend = backend;
		this.scheme = scheme;
	}
	
	private StdFileApi(final URI rootUri) {
		this(FileSystems.getFileSystem(rootUri), rootUri.getScheme());
	}
	
	public StdFileApi() {
		this(URI.create("file:/"));
	}

	@Override
	public String getScheme() {
		return scheme;
	}

	@Override
	public boolean exists(final URI path) {
		return Files.exists(asPath(path));
	}

	@Override
	public boolean isFile(final URI path) {
		return Files.isRegularFile(asPath(path));
	}

	@Override
	public boolean isDirectory(URI path) {
		return Files.isDirectory(asPath(path));
	}

	@Override
	public Stream<URI> listDirectory(final URI path) throws NotADirectoryException, FileApiException {
		final Texter texter = new Texter();
		
		try {
			return Files.list(asPath(path))
					.map(p -> join(texter, path, p.getFileName().toString()));
		} catch (final NotDirectoryException ex) {
			throw new NotADirectoryException(path, ex);
		} catch (final IOException ex) {
			throw new FileApiException(path, ex);
		}
	}

	@Override
	public void move(final URI source, final URI dest, boolean overwrite) throws FileApiException {
		try {
			final Path sourcePath = asPath(source);
			final Path destPath = asPath(dest);
			if (overwrite) {
				Files.move(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
			} else {
				Files.move(sourcePath, destPath);
			}
		} catch (final IOException ex) {
			throw new FileApiException(Arrays.asList(source, dest), ex);
		}
	}

	@Override
	public boolean remove(final URI path) throws FileApiException {
		try {
			return Files.deleteIfExists(asPath(path));
		} catch (final IOException ex) {
			throw new FileApiException(path, ex);
		}
	}

	@Override
	public void mkdirs(final URI path) throws FileApiException {
		try {
			Files.createDirectories(asPath(path));
		} catch (final IOException ex) {
			throw new FileApiException(path, ex);
		}
	}

	@Override
	public OutputStream createFile(final URI path) throws FileApiException, FileAlreadyExistsException {
		try {
			return Files.newOutputStream(asPath(path), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
		} catch (final java.nio.file.FileAlreadyExistsException ex) {
			throw new FileAlreadyExistsException(path, ex);
		} catch (final IOException ex) {
			throw new FileApiException(path, ex);
		}
	}

	@Override
	public OutputStream appendFile(URI path) throws FileApiException, FileNotFoundException {
		try {
			return Files.newOutputStream(asPath(path), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		} catch (final java.nio.file.NoSuchFileException ex) {
			throw new FileNotFoundException(path, ex);
		} catch (final IOException ex) {
			throw new FileApiException(path, ex);
		}
	}

	@Override
	public InputStream readFile(final URI path) throws FileApiException, FileNotFoundException {
		try {
			return Files.newInputStream(asPath(path));
		} catch (final java.nio.file.NoSuchFileException ex) {
			throw new FileNotFoundException(path, ex);
		} catch (final IOException ex) {
			throw new FileApiException(path, ex);
		}
	}
	 
	private Path asPath(final URI path) {
		assertScheme(path);
		
		return backend.provider().getPath(path);
	}
	
	private void assertScheme(final URI path) {
		Validate.notNull(path, "path cannot be null");
		Validate.notBlank(path.getPath(), "path.getPath() cannot be blank");
		if (!scheme.equals(path.getScheme())) {
			throw new InvalidPathException(path, "scheme not supported");
		}
	}

	private URI join(final Texter texter, final URI base, final String name) {
		final String baseStr = base.toString();
		final Texter.Builder b = texter.build();
		b.append(baseStr);
		if (!baseStr.endsWith("/")) {
			b.append('/');
		}
		b.append(name);
		return URI.create(b.toString());
	}
}
