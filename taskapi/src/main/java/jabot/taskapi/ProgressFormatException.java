package jabot.taskapi;

public class ProgressFormatException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	private final String text;
	
	public ProgressFormatException(String text, Throwable cause) {
		super(text, cause);
		this.text = text;
	}
	
	public ProgressFormatException(String text) {
		super(text);
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
