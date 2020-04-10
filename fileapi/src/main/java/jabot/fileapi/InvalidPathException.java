package jabot.fileapi;

import java.net.URI;

public class InvalidPathException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;

	private final URI path;

	public InvalidPathException(final URI path, final String message) {
		super(message+" @ "+path);
		this.path = path;
	}

	public URI getPath() {
		return path;
	}
	
	
}
