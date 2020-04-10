package jabot.taskapi;

public class InvalidTaskIdException extends InvalidIdException {
	private static final long serialVersionUID = 1L;

	public InvalidTaskIdException(String moniker) {
		super(moniker);
	}

}
