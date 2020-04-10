package jabot.taskapi;

public class SimpleErrorInfoFormatException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;

	public SimpleErrorInfoFormatException(final String asString) {
		super(asString);
	}

}
