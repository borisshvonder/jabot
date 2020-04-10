package jabot.taskapi;

public class TaskHandlerAlreadyRegisteredException extends InvalidIdException {
	private static final long serialVersionUID = 1L;
	
	public TaskHandlerAlreadyRegisteredException(String moniker) {
		super(moniker);
	}
}
