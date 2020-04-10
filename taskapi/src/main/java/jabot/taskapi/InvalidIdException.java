package jabot.taskapi;

public class InvalidIdException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	private final String moniker;
	
	public InvalidIdException(String moniker) {
		super(moniker);
		this.moniker = moniker;
	}

	public String getMoniker() {
		return moniker;
	}
}
