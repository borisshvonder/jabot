package jabot.taskapi;

public class InvalidTaskHandlerIdException extends InvalidIdException {
	private static final long serialVersionUID = 1L;
	private final TaskHandlerId handlerId;

	public InvalidTaskHandlerIdException(TaskHandlerId handlerId) {
		super(handlerId.getHandlerClass());
		this.handlerId = handlerId;
	}

	public TaskHandlerId getHandlerId() {
		return handlerId;
	}
	
}

