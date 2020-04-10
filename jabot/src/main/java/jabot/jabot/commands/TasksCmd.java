package jabot.jabot.commands;

import java.io.IOException;
import java.util.List;

import jabot.comcon.Cmd;
import jabot.comcon.ServiceCore;
import jabot.common.Texter;
import jabot.common.surge.Surge;
import jabot.common.types.Interval;
import jabot.common.types.IntervalUtils;
import jabot.common.types.Timestamp;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;
import jabot.taskapi.Progress;
import jabot.taskapi.Schedule;
import jabot.taskapi.TaskId;
import jabot.taskapi.TaskState;
import jabot.taskapi.Tasker;
import jabot.taskapi.Tasker.ErrorInfo;

public class TasksCmd implements Cmd {
	private static final Texter TXT = new Texter();
	
	@Override
	public String getName() {
		return "TASKS";
	}

	@Override
	public String getHelp() {
		return "[N:10] Display top N tasks";
	}

	@Override
	public void execute(
			final ServiceCore core, 
			final Lobby lobby, 
			final ReceivedMessage message, 
			final List<String> args
	) throws IOException {
		
		int n = 10;
		if (!args.isEmpty()) {
			if (args.size()==1) {
				n = Integer.parseInt(args.get(0));
			} else {
				throw new IllegalArgumentException("Invalid number of arguments "+args);
			}
		}
		
		final Timestamp now = Timestamp.now();
		final Texter.Builder reply = TXT.build();
		reply.append("Tasks\n");
		final Tasker tasker = core.getTasker();
		try(final Surge<TaskId> tasks = tasker.allTasks()) {
			TaskId task = tasks.next();
			while (task != null && n-->0) {
				reply.append(task.getMoniker()).append(": ");
				formatStatus(reply, tasker, now, task);
				reply.append("\n");
				task = tasks.next();
			}
		}
		lobby.post(reply.toString());
	}
	
	private void formatStatus(final Texter.Builder b, final Tasker tasker, final Timestamp now, final TaskId task) {
		final TaskState state = tasker.getState(task);
		final Schedule schedule = tasker.getSchedule(task);
		final Timestamp lastRun = tasker.getLastRun(task);
		final Timestamp lastSuccess = tasker.getLastSuccessfulRun(task);
		final ErrorInfo lastError = tasker.getLastError(task);
		final Timestamp nextRun = tasker.getNextRun(task);
		final Progress progress = tasker.getProgress(task);
		
		if (TaskState.RUNNING == state) {
			b.append("RUNNING ");
		}
		
		b.append(schedule.getType().name());
		
		if (schedule.getInterval() != null) {
			b.append('(').append(schedule.getInterval().toString()).append(')');
		}
		
		if (nextRun != null) {
			b.append(" NEXT");
			humanReadable(b, nextRun.since(now));
		}
		
		if (lastSuccess != null) {
			b.append(" SUCCESS");
			humanReadable(b, lastSuccess.since(now));
		}
		
		b.append(' ').append(progress.toString());
		
		if (lastRun != null && (lastSuccess == null || lastSuccess.before(lastRun))) {
			b.append(" LASTRUN");
			humanReadable(b, lastRun.since(now));
			
			if (lastError != null) {
				b.append(" ERROR ").append(lastError.getErrorClass()).append(": ").append(lastError.getMessage());
			}
		}
	}

	private void humanReadable(final Texter.Builder b, final Interval interval) {
		int length = b.length();
		IntervalUtils.humanReadable(b, interval, 2);
		final boolean intervalEmpty = length == b.length();
		if (intervalEmpty) {
			b.append(" now");
		}
	}

}