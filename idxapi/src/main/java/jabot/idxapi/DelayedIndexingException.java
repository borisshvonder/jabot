package jabot.idxapi;

public class DelayedIndexingException extends Exception {
	private static final long serialVersionUID = 1L;

	public DelayedIndexingException() {
		super();
	}

	public DelayedIndexingException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public DelayedIndexingException(final String message) {
		super(message);
	}

	public DelayedIndexingException(final Throwable cause) {
		super(cause);
	}
	
}
