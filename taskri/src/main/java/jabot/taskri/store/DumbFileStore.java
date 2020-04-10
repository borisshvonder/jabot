package jabot.taskri.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.common.cache.ExpiringCache;
import jabot.common.surge.Surge;
import jabot.common.types.Interval;
import jabot.common.types.Timestamp;
import jabot.fileapi.FileApi;
import jabot.marshall.Marshall;
import jabot.taskapi.TaskState;

/**
 * Very dumb persistent file-based store. Extremely inefficient, since it rewrites the file on each change. Therefore,
 * it would not scale beyond 10-100 jobs, but the implementation is extremely simple.
 *
 */
public class DumbFileStore extends RamTaskStore {
	private static final Logger LOG = LoggerFactory.getLogger(DumbFileStore.class);
	static {
		//make sure SerializedTasks static initializers has run
		new SerializedTasks();
	}
	private final FileApi fileApi;
	
	/** File to serialize tasks to. Note that there will be another file (backendFile.new) used for safer job updates*/
	private final URI backendFile;
	
	/** Temporary file to serialize tasks to */
	private final URI tempBackendFile;

	private ExpiringCache<Object> save = new ExpiringCache<>(Interval.SECOND);
	
	private volatile boolean needSave;

	public DumbFileStore(final FileApi fileApi, final URI backendFile) {
		this.fileApi = fileApi;
		this.backendFile = backendFile;
		this.tempBackendFile = URI.create(backendFile+".new");
		
		loadTasks();
	}
	
	public void setSaveInterval(final Interval interval) {
		save = new ExpiringCache<>(interval); 
	}

	@Override
	public TaskRecord newTask(final Task task) {
		final TaskRecord result = super.newTask(task);
		saveTasks();
		return result;
	}

	@Override
	public TaskRecord cas(final TaskRecord oldState, final Task newState) {
		final TaskRecord result = super.cas(oldState, newState);
		if (result != null) {
			saveTasks();
		} else {
			saveIfNeeded();
		}
		return result;
	}

	@Override
	public void remove(final String moniker) {
		super.remove(moniker);
		saveTasks();
	}
	
	@Override
	public TaskRecord find(final String moniker) {
		saveIfNeeded();
		return super.find(moniker);
	}

	@Override
	public TaskRecord reload(final TaskRecord state) {
		saveIfNeeded();
		return super.reload(state);
	}

	@Override
	public Surge<TaskRecord> listScheduled(
			final Collection<String> forHandlers, 
			final Collection<TaskState> forStates,
			final Timestamp scheduledBefore
	) {
		saveIfNeeded();
		return super.listScheduled(forHandlers, forStates, scheduledBefore);
	}

	@Override
	public Surge<TaskRecord> listScheduledToNeverRun() {
		saveIfNeeded();
		return super.listScheduledToNeverRun();
	}

	private void loadTasks() {
		try {
			if (fileApi.exists(backendFile)) {
				try(final InputStream in = fileApi.readFile(backendFile)) {
					final SerializedTasks tasks = Marshall.get().fromStream(in, SerializedTasks.class);
					LOG.debug("Loading tasks timestamp={}", tasks.getSaved());
					for (final Task task : tasks.getTasks()) {
						if (task.getState() == TaskState.RUNNING) {
							// it is possible we serialized running task, need to reschedule it again
							task.setState(TaskState.SCHEDULED);
						}
						super.newTask(task);
					}
					LOG.info("Loaded {} tasks timestamp={}", tasks.getTasks().size(), tasks.getSaved());
				}
			}
		} catch (final IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
	
	private void saveTasks() {
		needSave = true;
		if (save.isExpired()) {
			try {
				saveImpl();
			} catch (final IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
	}
	
	private void saveIfNeeded() {
		if (needSave && save.isExpired()) {
			try {
				saveImpl();
			} catch (final IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
	}

	private synchronized void saveImpl() throws IOException {
		if (save.isExpired()) { //Double-check to make sure we don't save from two threads too often
			final SerializedTasks tasks = new SerializedTasks();
			tasks.setSaved(Timestamp.now());
			tasks.setTasks(new LinkedList<Task>());
			
			addTasks(tasks.getTasks(), super.listScheduled(null, null, null));
			addTasks(tasks.getTasks(), super.listScheduledToNeverRun());
			
			fileApi.remove(tempBackendFile);
			try(final OutputStream out = fileApi.createFile(tempBackendFile)) {
				Marshall.get().toStream(tasks, out);
			}
			fileApi.move(tempBackendFile, backendFile, true);
			save.set(null);
			needSave = false;
		}
	}

	private void addTasks(final List<Task> accumulator, final Surge<TaskRecord> it) throws IOException {
		try {
			TaskRecord state = it.next();
			while(state != null) {
				accumulator.add(state.getTask());
				state = it.next();
			}
		} finally {
			it.close();
		}
	}

	static final class SerializedTasks {
		// make sure Task static initializer has run
		static {
			new Task();
		}
		private Timestamp saved;
		private List<Task> tasks;
		
		public Timestamp getSaved() {
			return saved;
		}
		
		public void setSaved(Timestamp saved) {
			this.saved = saved;
		}
		
		public List<Task> getTasks() {
			return tasks;
		}
		
		public void setTasks(List<Task> tasks) {
			this.tasks = tasks;
		}
	}
}
