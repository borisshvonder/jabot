package jabot.taskapi;

/**
 * An opaque task id
 */
public interface TaskId {
	
	/** @return @notnull unique task moniker */
	String getMoniker();
	
	/** 
	 * Get handler moniker. Note that there is no guarantee that you will be able to find this moniker using 
	 * {@link Tasker#findHandler(String)} since it is perfectly legal to have tasks that cannot be handled by all 
	 * taskers
	 * 
	 * @return @notnull task handler moniker */
	String getHandlerMoniker();
}
