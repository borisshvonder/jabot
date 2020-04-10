package jabot.taskapi;

public class TaskAlreadyExistsException extends InvalidTaskIdException {
	private static final long serialVersionUID = 1L;

	public TaskAlreadyExistsException(final String moniker) {
		super(moniker);
	}
}
