package jabot.fileapi;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

public class FileApiException extends IOException {
	private static final long serialVersionUID = 1L;

	private final Collection<URI> paths;

	public FileApiException(final URI path, final String message) {
		this(Arrays.asList(path), message);
	}

	public FileApiException(final Collection<URI> paths, final String message) {
		super(message+" @ "+paths);
		this.paths = paths;
	}
	
	public FileApiException(final URI path, final IOException cause) {
		this(Arrays.asList(path), cause);
	}
	
	public FileApiException(Collection<URI> paths, final IOException cause) {
		super("@ "+paths, cause);
		this.paths = paths;
	}

	public Collection<URI> getPath() {
		return paths;
	}
}
