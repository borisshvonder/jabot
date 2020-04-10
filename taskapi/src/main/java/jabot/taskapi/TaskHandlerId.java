package jabot.taskapi;

/**
 * An opaque task handler id
 */
public interface TaskHandlerId {
	/** @return @notnull handler class name */
	String getHandlerClass();
}
