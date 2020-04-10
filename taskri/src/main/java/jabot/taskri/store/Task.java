package jabot.taskri.store;

import static jabot.frozen.Freeze.ensureNotFrozen;

import jabot.common.types.Timestamp;
import jabot.frozen.Freezing;
import jabot.marshall.Marshall;
import jabot.taskapi.Progress;
import jabot.taskapi.Schedule;
import jabot.taskapi.TaskState;
import jabot.taskri.store.marshall.IntervalAdapter;
import jabot.taskri.store.marshall.ProgressAdapter;
import jabot.taskri.store.marshall.ScheduleAdapter;
import jabot.taskri.store.marshall.TimestampAdapter;

/**
 * Task description. To facilitate marshalling, this class has standard javabean interface.
 *
 */
public class Task implements Freezing<Task>{
	static {
		Marshall.get().configure().addTypeAdapter(new IntervalAdapter());
		Marshall.get().configure().addTypeAdapter(new ScheduleAdapter());
		Marshall.get().configure().addTypeAdapter(new TimestampAdapter());
		Marshall.get().configure().addTypeAdapter(new ProgressAdapter());
	}
	private TaskState state;
	private String moniker;
	private String handlerMoniker;
	private Schedule schedule;
	private Schedule failedSchedule;
	private Timestamp lastRunStart;
	private Timestamp lastRunFinish;
	private Timestamp lastSuccess;
	private String lastErrorClass;
	private String lastErrorMessage;
	private Timestamp nextRun;

	/** Params are supposed to be supported by Marshall */
	private String params;
	
	/** Mementos are supposed to be supported by Marshall */
	private String memento;
	
	private Progress progress;
	
	/** if true, any modification will throw FrozenException **/
	private transient boolean frozen;
	
	@Override
	public boolean isFrozen() {
		return frozen;
	}

	@Override
	public void freeze() {
		frozen = true;
	}

	@Override
	public Task defrost() {
		final Task copy = new Task();
		copy.setState(state);
		copy.setMoniker(moniker);
		copy.setHandlerMoniker(handlerMoniker);
		copy.setSchedule(schedule);
		copy.setFailedSchedule(failedSchedule);
		copy.setLastRunStart(lastRunStart);
		copy.setLastRunFinish(lastRunFinish);
		copy.setLastErrorClass(lastErrorClass);
		copy.setLastErrorMessage(lastErrorMessage);
		copy.setNextRun(nextRun);
		copy.setParams(params);
		copy.setMemento(memento);
		copy.setProgress(progress);
		return copy;
	}
	
	public TaskState getState() {
		return state;
	}

	public void setState(TaskState state) {
		ensureNotFrozen(this);
		this.state = state;
	}

	public String getMoniker() {
		return moniker;
	}

	public void setMoniker(final String moniker) {
		ensureNotFrozen(this);
		this.moniker = moniker;
	}

	public String getHandlerMoniker() {
		return handlerMoniker;
	}

	public void setHandlerMoniker(final String handlerMoniker) {
		ensureNotFrozen(this);
		this.handlerMoniker = handlerMoniker;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setSchedule(final Schedule schedule) {
		ensureNotFrozen(this);
		this.schedule = schedule;
	}

	public Schedule getFailedSchedule() {
		return failedSchedule;
	}

	public void setFailedSchedule(final Schedule failedSchedule) {
		ensureNotFrozen(this);
		this.failedSchedule = failedSchedule;
	}

	public Timestamp getLastRunStart() {
		return lastRunStart;
	}

	public void setLastRunStart(final Timestamp lastRunStart) {
		ensureNotFrozen(this);
		this.lastRunStart = lastRunStart;
	}

	public Timestamp getLastRunFinish() {
		return lastRunFinish;
	}

	public void setLastRunFinish(final Timestamp lastRunFinish) {
		ensureNotFrozen(this);
		this.lastRunFinish = lastRunFinish;
	}
	
	public Timestamp getLastSuccess() {
		return lastSuccess;
	}

	public void setLastSuccess(final Timestamp lastSuccess) {
		ensureNotFrozen(this);
		this.lastSuccess = lastSuccess;
	}

	public String getLastErrorClass() {
		return lastErrorClass;
	}

	public void setLastErrorClass(String lastErrorClass) {
		ensureNotFrozen(this);
		this.lastErrorClass = lastErrorClass;
	}

	public String getLastErrorMessage() {
		return lastErrorMessage;
	}

	public void setLastErrorMessage(String lastErrorMessage) {
		ensureNotFrozen(this);
		this.lastErrorMessage = lastErrorMessage;
	}

	public Timestamp getNextRun() {
		return nextRun;
	}

	public void setNextRun(final Timestamp nextRun) {
		ensureNotFrozen(this);
		this.nextRun = nextRun;
	}

	public String getParams() {
		return params;
	}

	public void setParams(final String params) {
		ensureNotFrozen(this);
		this.params = params;
	}

	public String getMemento() {
		return memento;
	}

	public void setMemento(final String memento) {
		ensureNotFrozen(this);
		this.memento = memento;
	}

	public Progress getProgress() {
		return progress;
	}

	public void setProgress(final Progress progress) {
		ensureNotFrozen(this);
		this.progress = progress;
	}
	
}
