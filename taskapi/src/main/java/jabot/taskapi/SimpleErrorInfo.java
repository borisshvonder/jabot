package jabot.taskapi;

import org.apache.commons.lang3.Validate;

/** A simple {@link Tasker.ErrorInfo} implementation over an Throwable */
public class SimpleErrorInfo implements Tasker.ErrorInfo {
	private final String errorClass;
	private final String message;

	/**
	 * @param @notnull errorClass
	 * @param message
	 */
	public SimpleErrorInfo(final String errorClass, final String message) {
		Validate.notNull(errorClass, "errorClass cannot be null");
		
		this.errorClass = errorClass;
		this.message = message;
	}
	
	public SimpleErrorInfo(final Throwable error) {
		this(error.getClass().getName(), error.getMessage());
	}

	@Override
	public String getErrorClass() {
		return errorClass;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		final int esitmatedLength = errorClass.length() + 2 + (message == null ? 0 : 2+message.length());
		final StringBuilder b = new StringBuilder(esitmatedLength);
		b.append(errorClass).append('(');
		if (message != null) {
			b.append('"').append(message).append('"');
		}
		b.append(')');
		return b.toString();
	}
	
}
