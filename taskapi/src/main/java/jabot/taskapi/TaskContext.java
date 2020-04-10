package jabot.taskapi;

/**
 * Task execution context
 */
public interface TaskContext<P extends TaskParams, M extends TaskMemento> {
	
	/** @return @notnull reference to the tasker */
	Tasker getTasker();
	
	/** @return true IFF task is aborted */
	boolean isAborted();
	
	/** @return @notnull task parameters */
	P getParams();
	
	/** @return @nullable task memento, the value may be set and preserved between task runs */
	M getMemento();
	
	/**
	 * Set task memento. It's value will be preserved between task runs
	 * @param @nullable newMemento new value
	 */
	void setMemento(M newMemento);
	
	/** @return @notnull current job progress */
	Progress getProgress();
	
	/**
	 * Report task progress
	 * 
	 * @param @notnull progress
	 */
	void setProgress(Progress progress);
}
