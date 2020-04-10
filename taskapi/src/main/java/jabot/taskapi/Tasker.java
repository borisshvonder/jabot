package jabot.taskapi;

import java.util.Collection;

import org.apache.commons.lang3.Validate;

import jabot.common.surge.Surge;
import jabot.common.types.Timestamp;

/**
 * Tasks control interface
 * @author bpasko
 *
 */
public interface Tasker {
	
	/**
	 * Start the tasker. May not be called twice.
	 * @throws IllegalStateException if already started
	 */
	void startup();
	
	/**
	 * Pause tasks execution for a while. Has no effect on paused tasker.
	 * This is typically an administrator decision to stop tasker (possibly as a last resort at reducing resource usage)
	 */
	void pause();
	
	
	/**
	 * Resume tasks execution. Has no effect on already resumed tasker.
	 */
	void resume();

	/**
	 * Stop the tasker. After tasker is stopped, it is considered invalid and may not be used anymore. It cannot be 
	 * started again.
	 * Has no effect when called multiple times.
	 */
	void shutdown() throws Exception;

	/**
	 * Register task handler 
	 * @param @notnull handler
	 * @return handler id 
	 * @throws TaskHandlerAlreadyRegisteredException when there is already a handler with same moniker registered
	 */
	TaskHandlerId registerHandler(TaskHandler<?, ?> handler);
	
	/**
	 * Find hanlder by class name 
	 * @param @notnull handlerClass class.getName() of the handler
	 * @return handler id or null if no such handler registered
	 */
	TaskHandlerId findHandler(String handlerClass);
	
	/**
	 * Create new task. 
	 * 
	 * By design, you can only create jobs that can be handled by current tasker
	 * 
	 * @param @notnull moniker unique task id
	 * @param @notnull handler task handler
	 * @param @notnull schedule normal schedule
	 * @param @notnull whenFailed alternative schedule to apply when task has failed
	 * @param @nullable params task parameters. When null, use default parameters
	 * @return @notnull task id
	 * @throws TaskAlreadyExistsException when job with same moniker exists
	 * @throws IllegalArgumentException when params is null and handler has no default parameters
	 * @throws InvalidTaskHandlerIdException when hander is invalid
	 */
	default TaskId createTask(String moniker, TaskHandlerId handler, Schedule schedule, TaskParams params) {
		return createTask(moniker, handler, schedule, Schedule.NEVER, params, null);
	};

	/**
	 * Create new task. 
	 * 
	 * By design, you can only create jobs that can be handled by current tasker
	 * 
	 * @param @notnull moniker unique task id
	 * @param @notnull handler task handler
	 * @param @notnull schedule normal schedule
	 * @param @notnull whenFailed alternative schedule to apply when task has failed
	 * @param @nullable params task parameters. When null, use default parameters
	 * @param @nullable memento task memento
	 * @return @notnull task id
	 * @throws TaskAlreadyExistsException when job with same moniker exists
	 * @throws IllegalArgumentException when params is null and handler has no default parameters
	 * @throws InvalidTaskHandlerIdException when hander is invalid
	 */
	TaskId createTask(String moniker, TaskHandlerId handler, Schedule schedule, Schedule whenFailed, TaskParams params, TaskMemento memento);
	
	/**
	 * Remove job. This call always succeeds but there is no guarantee that job will not run if it's schedule is close.
	 * @param @notnull moniker
	 */
	void removeTask(String moniker);
	
	/**
	 * Find task using the job moniker
	 * 
	 * @param @notnull moniker
	 * @return @nullable task id
	 */
	TaskId findTask(String moniker);
	
	/**
	 * The implemenation is allowed to cache all task-related information internally since there is no guarantee that
	 * information will not get stalled due to concurrent execution. 
	 * 
	 * When client needs to obtain freshier information regarding a task, it can use this method to get a freshier state.
	 * 
	 * @param @notnull task
	 * @return @nullable freshier task id or null if task is gone
	 * @throws InvalidTaskIdException when task is invalid
	 */
	default TaskId refresh(TaskId task) {
		Validate.notNull(task, "task cannot be null");
		
		return findTask(task.getMoniker());
	}
	
	/**
	 * Get task state.
	 * 
	 * Note that this information may be outdated. If you need to update the information, use {@link #refresh(TaskId)}
	 * 
	 * @param @notnull task 
	 * @return @notnull schedule
	 * @throws InvalidTaskIdException when task is invalid
	 */
	TaskState getState(TaskId task);
	
	/**
	 * Get normal job schedule.
	 * 
	 * Note that this information may be outdated. If you need to update the information, use {@link #refresh(TaskId)}
	 * 
	 * @param @notnull task 
	 * @return @notnull schedule
	 * @throws InvalidTaskIdException when task is invalid
	 */
	Schedule getSchedule(TaskId task);
	
	/**
	 * Set normal job schedule. The nextRun is immediately recalculated for a job upon calling this method.
	 * Note that in order to run job now you can use <code>setSchedule(schedule.runAt(new Timestamp()));</code>, unless, of 
	 * course, your schedule is NEVER.
	 * 
	 * This method is non-atomic and always succeeds on last-caller-wins basis unless task is removed concurrently.
	 * 
	 * @param @notnull task 
	 * @param @notnull newSchedule
	 * @throws InvalidTaskIdException when task is invalid
	 */
	void setSchedule(TaskId task, Schedule newSchedule);
	
	/**
	 * Get failed job schedule.
	 * 
	 * Note that this information may be outdated. If you need to update the information, use {@link #refresh(TaskId)}
	 * 
	 * @param @notnull task 
	 * @return @notnull schedule
	 * @throws InvalidTaskIdException when task is invalid
	 */
	Schedule getFailedSchedule(TaskId task);
	
	/**
	 * Set failed job schedule. The nextRun is not recalculated even if job is faulted.
	 * 
	 * This method is non-atomic and always succeeds on last-caller-wins basis unless task is removed concurrently.
	 * 
	 * @param @notnull task 
	 * @param newSchedule
	 * @throws InvalidTaskIdException when task is invalid
	 */
	void setFailedSchedule(TaskId task, Schedule newSchedule);
	
	/**
	 * Get next scheduled to run date for a task. May return null if effective schedule is NEVER
	 * @param @notnull task
	 * @return @nullable next run date
	 * @throws InvalidTaskIdException when task is invalid
	 */
	Timestamp getNextRun(TaskId task);
	
	/**
	 * Get date at which task ran last time
	 * 
	 * @param @notnull task
	 * @return @nullable last run date
	 * @throws InvalidTaskIdException when task is invalid
	 */
	Timestamp getLastRun(TaskId task);

	/**
	 * Get date at which task ran successfully (without errors) last time
	 * 
	 * @param @notnull task
	 * @return @nullable last run date
	 * @throws InvalidTaskIdException when task is invalid
	 */
	Timestamp getLastSuccessfulRun(TaskId task);
	
	
	interface ErrorInfo {
		/** @return @notnull exception or error class name */
		String getErrorClass();
		
		/** @return exception message or null */
		String getMessage();
	}
	/**
	 * Get last error (if any) task failed with
	 * 
	 * @param @notnull task
	 * @return @nullable last error or null if completed successfully
	 * @throws InvalidTaskIdException when task is invalid
	 */
	ErrorInfo getLastError(TaskId task);
	
	/**
	 * Get task parameters.
	 * 
	 * Since task not necessarily can be processed by this node, the parameters for it may or may not be 
	 * marshalled/unmarshalled. The Tasker will never try to unmarshall parameters for handlers it does not have 
	 * registered, thus it returns a String that has json representation of the parameters.
	 * 
	 * @param @notnull task 
	 * @return @notnull parameters
	 * @throws InvalidTaskIdException when task is invalid
	 */
	String getParams(TaskId task);
	
	/**
	 * Get task memento.
	 * 
	 * Since task not necessarily can be processed by this node, the parameters for it may or may not be 
	 * marshalled/unmarshalled. The Tasker will never try to unmarshall mementos for handlers it does not have 
	 * registered, thus it returns a String that has json representation of the memento.
	 * 
	 * Note that this information may be outdated. If you need to update the information, use {@link #refresh(TaskId)}
	 * 
	 * @param @notnull task 
	 * @return @nullable memento
	 * @throws InvalidTaskIdException when task is invalid
	 */
	String getMemento(TaskId task);
	
	/**
	 * Set task memento.
	 * 
	 * The operation has no effect if task is gone. The operation may have no effect if task is running since the 
	 * memento may be cached by the implementation during task run.
	 * 
	 * Last caller wins.
	 * 
	 * This is primarily intended to be triggered by human administrator. Potentially dangerous since there is no type
	 * checks on the actual value class
	 * 
	 * @param @notnull task 
	 * @param @nullable memento
	 * @throws InvalidTaskIdException when task is invalid
	 */
	void setMemento(TaskId task, TaskMemento memento);
	
	/**
	 * Get task progress
	 * 
	 * Note that this information may be outdated. If you need to update the information, use {@link #refresh(TaskId)}
	 * 
	 * @param @notnull task
	 * @return @notnul progress
	 * @throws InvalidTaskIdException when task is invalid
	 */
	Progress getProgress(TaskId task);
	
	/**
	 * List all registered handlers
	 * @return @notnull all handler ids
	 */
	Collection<TaskHandlerId> allHandlers();
	
	
	/**
	 * List all tasks. POTENTIALLY very heavy operation since number of jobs is unbounded.
	 * 
	 * Try restricting your list using {@link #allTasks(Collection)}
	 * 
	 * @return @notnull all task ids ordered by nextRun. Client responsible for closing associated resources.
	 */
	default Surge<TaskId> allTasks() {
		return allTasks(null);
	}
	
	/**
	 * List all tasks for specified handlers
	 * @param @nullable forHandlers list of handlers you want tasks for or null if you need all tasks
	 * @return @notnull all task ids ordered by nextRun. Client responsible for closing associated resources.
	 * @throws InvalidTaskHandlerIdException when one or more handers are invalid
	 */
	Surge<TaskId> allTasks(Collection<String> forHandlers);
	
}
