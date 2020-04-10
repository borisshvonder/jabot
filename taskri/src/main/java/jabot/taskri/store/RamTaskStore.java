package jabot.taskri.store;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.common.surge.Surge;
import jabot.common.surge.Surgeon;
import jabot.common.types.Timestamp;
import jabot.taskapi.InvalidIdException;
import jabot.taskapi.TaskState;

/**
 * Non-persistent in-memory TaskStore implementation.
 * 
 * Usable for testing and high-perfomance single-node setups.
 * 
 * NOTE: This implementation stores Tasks directly in memory so it aggressively freezes them on way in and out
 *
 */
public class RamTaskStore implements TaskStore {
	// IMPLEMENTATION NOTES
	//
	// note.001 TaskState design
	//          TaskState implementation was designed in such a way that once it is looked up using the internal
	//          indexes, the mutations are made against internal Record and thus don't have to be looked up anymore
	//
	// note.002 Lock-free updates
	//          instead of synchronizing, concurrent collections and ordered updates are used. The Record is 
	//          considered deleted when it's task is set to null and may appear for some time in the index collections

	
	// TODO: Lots of improvements can be made to synchonization using correctly ordered updates... 
	//       But, for simplicity...this would do for now
	private static final Logger LOG = LoggerFactory.getLogger(RamTaskStore.class);
	private final Map<String, Record> primary = new ConcurrentHashMap<String, Record>();
	private final SortedMap<Timestamp, IndexRecord> idxByNextRun = new TreeMap<>();
	private final IndexRecord idxUnscheduled = new IndexRecord(null);
	
	@Override
	public TaskRecord newTask(final Task task) {
		Validate.notNull(task, "task cannot be null");
		task.freeze();
		
		final String moniker = task.getMoniker();
		Validate.notNull(moniker, "task.getMoniker() cannot be null");
		
		final Record newRecord = new Record(task);
		
		if (primary.compute(moniker, (k, v) -> v == null ? newRecord : v) != newRecord) {
			return null;
		}
		
		addToIndex(task.getNextRun(), newRecord);
		
		return TaskStateImpl.fromRecord(this, newRecord);
	}

	@Override
	public TaskRecord find(final String moniker) {
		Validate.notNull(moniker, "moniker cannot be null");
		final Record record = primary.get(moniker);
		return record == null ? null : TaskStateImpl.fromRecord(this, record);
	}

	@Override
	public TaskRecord reload(final TaskRecord state) {
		Validate.notNull(state, "state cannot be null");
		
		final TaskStateImpl oldImpl = TaskStateImpl.recast(this, state);
		return TaskStateImpl.fromRecord(this, oldImpl.record);
	}

	@Override
	public TaskRecord cas(final TaskRecord oldState, final Task newState) {
		Validate.notNull(oldState, "oldState cannot be null");
		Validate.notNull(newState, "newState cannot be null");
		
		final TaskStateImpl oldImpl = TaskStateImpl.recast(this, oldState);
		final String moniker = oldImpl.getMoniker();
		Validate.isTrue(moniker.equals(newState.getMoniker()), "you cannot change task moniker from {} to {}", moniker, newState.getMoniker());

		if (oldImpl.record.task.compareAndSet(oldImpl.task, newState)) {
			final Timestamp oldNextRun = oldImpl.task.getNextRun();
			final Timestamp newNextRun = newState.getNextRun();
			
			if (!Objects.equals(oldNextRun, newNextRun)) {
				// For a brief moment, task MAY appear in both sets. Self-healing is needed when listing
				addToIndex(newNextRun, oldImpl.record);
				removeFromIndex(oldNextRun, oldImpl.record);
			}
			
			return new TaskStateImpl(this, moniker, oldImpl.record, newState);
		} else {
			return null;
		}
	}

	@Override
	public void remove(final String moniker) {
		Validate.notNull(moniker, "moniker cannot be null");
		
		final Record existing = primary.remove(moniker);
		final Task task = existing.task.get();
		if (task != null) {
			removeFromIndex(task.getNextRun(), existing);
			existing.task.set(null);
		}
	}

	@Override
	public Surge<TaskRecord> listScheduled(final Collection<String> forHandlers, final Collection<TaskState> forStates, 
			final Timestamp scheduledBefore) 
	{
		List<TaskRecord> result = new LinkedList<>();
		synchronized (idxByNextRun) {
			for (final Map.Entry<Timestamp, IndexRecord> entry : idxByNextRun.entrySet()) {
				final Timestamp nextRun = entry.getKey();
				if (scheduledBefore != null && nextRun.isAfter(scheduledBefore)) {
					break;
				}
				final IndexRecord idx = entry.getValue();
				for (final Record record : idx.records) {
					final TaskStateImpl impl = TaskStateImpl.fromRecord(this, record);
					if (impl != null && handlerMatches(impl, forHandlers) && stateMatches(impl, forStates)) {
						result.add(impl);
					}
				}
			}
		}
		return Surgeon.open(result).get();
	}
	
	private static boolean handlerMatches(final TaskRecord taskState, final Collection<String> forHandlers) {
		return forHandlers == null ? true : forHandlers.contains(taskState.getTask().getHandlerMoniker());
	}

	private boolean stateMatches(final TaskRecord taskState, final Collection<TaskState> forStates) {
		return forStates == null ? true : forStates.contains(taskState.getTask().getState());
	}

	@Override
	public Surge<TaskRecord> listScheduledToNeverRun() {
		synchronized(idxUnscheduled) {
			return Surgeon.open(idxUnscheduled.records)
					.map(record -> (TaskRecord)TaskStateImpl.fromRecord(this, record))
					.prefetch();
		}
	}

	private void addToIndex(final Timestamp nextRun, final Record rec) {
		if (nextRun == null) {
			synchronized(idxUnscheduled) {
				idxUnscheduled.records.add(rec);
			}
		} else {
			synchronized(idxByNextRun) {
				IndexRecord idx = idxByNextRun.get(nextRun);
				if (idx == null) {
					idx = new IndexRecord(nextRun);
					idxByNextRun.put(nextRun, idx);
				}
				idx.records.add(rec);
			}
		}
	}
	
	private void removeFromIndex(final Timestamp nextRun, final Record rec) {
		if (nextRun == null) {
			synchronized(idxUnscheduled) {
				idxUnscheduled.records.remove(rec);
			}
		} else {
			synchronized(idxByNextRun) {
				IndexRecord idx = idxByNextRun.get(nextRun);
				if (idx == null) {
					LOG.warn("Could not find index record with timestamp {} when removing record {} from index", nextRun, rec);
				} else {
					idx.records.remove(rec);
					if (idx.records.isEmpty()) {
						idxByNextRun.remove(nextRun);
					}
				}
			}
		}
	}
	
	private static final class TaskStateImpl implements TaskRecord {
		private final RamTaskStore owner;
		private final Record record;
		private final Task task;
		private final String moniker;
		
		public TaskStateImpl(RamTaskStore owner, String moniker, Record record, Task task) {
			this.owner = owner;
			this.moniker = moniker;
			this.record = record;
			this.task = task;
		}

		public static TaskStateImpl fromRecord(final RamTaskStore owner, final Record record) {
			final Task task = record.task.get();
			if (task == null) {
				return null;
			} else {
				return new TaskStateImpl(owner, task.getMoniker(), record, task);
			}
		}
		
		public static TaskStateImpl recast(final RamTaskStore owner, final TaskRecord iface) {
			if (iface instanceof TaskStateImpl) {
				final TaskStateImpl ret = (TaskStateImpl) iface;
				if (ret.owner == owner) {
					return ret;
				}
			}
			throw new InvalidIdException(iface.getMoniker());
		}

		@Override
		public String getMoniker() {
			return moniker;
		}

		@Override
		public Task getTask() {
			return task;
		}
	}
	
	private static final class Record {
		private final AtomicReference<Task> task;
		
		public Record(Task task) {
			this.task = new AtomicReference<>(task);
		}
		
		/**For debugging only */
		public String toString() {
			return String.valueOf(task);
		}
	}
	
	private static final class IndexRecord  {
		private final Timestamp nextRun;
		private final List<Record> records = new LinkedList<>();

		public IndexRecord(final Timestamp nextRun) {
			this.nextRun = nextRun;
		}
		
		/**For debugging only */
		public String toString() {
			return String.valueOf(nextRun)+"->"+String.valueOf(records);
		}
	}
	
}
