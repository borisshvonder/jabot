package jabot.rsapi;

/**
 * Signals generic API error, not retriable
 */
public class ApiError extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ApiError() {
		super();
	}

	public ApiError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ApiError(String message, Throwable cause) {
		super(message, cause);
	}

	public ApiError(String message) {
		super(message);
	}

	public ApiError(Throwable cause) {
		super(cause);
	}

	
}
