package jabot.metapi;

public class ExtractionException extends Exception{
	private static final long serialVersionUID = 1L;

	public ExtractionException() {
		super();
	}

	public ExtractionException(final String message, 
			final Throwable cause, 
			final boolean enableSuppression, 
			final boolean writableStackTrace) 
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ExtractionException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ExtractionException(final String message) {
		super(message);
	}

	public ExtractionException(final Throwable cause) {
		super(cause);
	}
	
}
