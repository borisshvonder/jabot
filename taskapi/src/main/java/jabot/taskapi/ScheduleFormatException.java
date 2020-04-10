package jabot.taskapi;

public class ScheduleFormatException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	private final String text;
	
	public ScheduleFormatException(String text) {
		super(text);
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
