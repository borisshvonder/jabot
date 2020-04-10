package jabot.taskri.store;

import java.util.Collection;

import jabot.common.surge.Surge;
import jabot.common.types.Timestamp;
import jabot.taskapi.TaskState;

/**
 * Stores and indexes tasks
 */
public interface TaskStore {
	/** An @immutable task state, represents some fixed (in time) state of the task */
	interface TaskRecord {
		/** Task moniker is considered a primary key for the task*/
		String getMoniker();
		
		/** 
		 * Return the underlying task. It is up to the implementation to {@link Task#freeze()} to insure multithreaded
		 * safety. Should implementation choose NOT to freeze task, the modifications to the task should NEVER have 
		 * any effect to the task state itself, in other words, the state should be valid regardless.
		 * 
		 * @return @notnull task
		 */
		Task getTask();
	}
	
	/**
	 * Create new task. @atomic operation - returns null if task with the same moniker already exists.
	 * 
	 * It is up to the implementation to {@link Task#freeze()} it immediately or not.
	 * 
	 * @param @notnull task
	 * @return @nullable new task state or null if task with same moniker already exists
	 */
	TaskRecord newTask(Task task);
	
	/**
	 * Find task by it's moniker
	 * 
	 * @param @notnull moniker
	 * @return task state or null if no such state exists (for ex. state was removed)
	 */
	
	TaskRecord find(String moniker);
	
	/**
	 * When {@link #cas(TaskRecord, Task)} returns null, you may reload task state to bring it up to date using this
	 * method.
	 * 
	 * @param @notnull state
	 * @return refreshed state or null if task was deleted
	 */
	default TaskRecord reload(TaskRecord state) {
		// This is naive, the real implementation might be more effective than findTask
		return find(state.getMoniker());
	};
	
	/**
	 * Compare-and-swap task state
	 *
	 * It is up to the implementation to {@link Task#freeze()} it immediately or not.
	 * 
	 * @param @notnull oldState 
	 * @param @notnull newState
	 * @return null if compare-and-swap fails, new state otherwise
	 */
	TaskRecord cas(TaskRecord oldState, Task newState);
	
	/**
	 * Remove some existing task.
	 * 
	 * Unconditional operation, does not have compare-and-swap semantics.
	 * @param @notnull moniker task moniker 
	 * @return true if task was removed by this specific client, false if it was removed by someone else
	 */
	void remove(String moniker);
	
	/**
	 * List tasks in order of {@link Task#getNextRun()}.
	 * 
	 * It is up to the implementation to {@link Task#freeze()} tasks or not
	 * 
	 * @param @nullable forHandlers only list tasks for these handler monikers (optional)
	 * @param @nullable forStates only asks with these states (optional)
	 * @param @nullable scheduledBefore only return tasks with {@link Task#getNextRun()} &lt;= scheduledBefore
	 * @return @notnull iterator. Client responsible for closing associated resources.
	 */
	Surge<TaskRecord> listScheduled(Collection<String> forHandlers, Collection<TaskState> forStates, Timestamp scheduledBefore);
	
	/**
	 * List tasks with nextRun == null. These are tasks with schedule == NEVER or OneShot 
	 * tasks that has been executed already.
	 * 
	 * It is up to the implementation to {@link Task#freeze()} task or not
	 * 
	 * @return @notnull iterator. Client responsible for closing associated resources.
	 */
	Surge<TaskRecord> listScheduledToNeverRun();
}
