package jabot.taskapi;

public interface TaskHandler<P extends TaskParams, M extends TaskMemento> {
	/**
	 * Handle a task
	 * @param @notnull ctx task execution context
	 */
	void handle(TaskContext<P, M> ctx) throws Exception;
	
	/**
	 * Marshall parameters to string
	 * @param @notnull params
	 * @return @notnull marshalled parameters
	 */
	String marshallParams(P params);

	/**
	 * Unmarshall params from string
	 * @param @notnull marshalled
	 * @return @notnull params
	 */
	P unmarshallParams(String marshalled);

	/**
	 * Marshall memento to string
	 * @param @notnull memento
	 * @return @notnull marshalled memento
	 */
	String marshallMemento(M memento);

	/**
	 * Unmarshall memento from string
	 * @param @notnull marshalled
	 * @return @notnull memento
	 */
	M unmarshallMemento(String marshalled);

}
