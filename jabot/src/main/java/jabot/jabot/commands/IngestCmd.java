package jabot.jabot.commands;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import jabot.comcon.Cmd;
import jabot.comcon.ServiceCore;
import jabot.common.Texter;
import jabot.fileapi.FileApiUtils;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;
import jabot.taskapi.Schedule;
import jabot.taskapi.TaskHandlerId;
import jabot.taskapi.TaskId;
import jabot.taskapi.Tasker;
import jabot.tasks.IngestTaskHandler;

public class IngestCmd implements Cmd {
	private final Texter texter = new Texter();

	@Override
	public String getName() {
		return "INGEST";
	}

	@Override
	public String getHelp() {
		return "uri [languages [backend]] : ingest files listed at uri";
	}

	@Override
	public void execute(
			final ServiceCore core, 
			final Lobby lobby, 
			final ReceivedMessage message, 
			final List<String> args
	) throws IOException {
		Validate.isTrue(1 <= args.size() && args.size() <= 3, "Expected 1-3 arguments, got: {}", args);
		final String uri = args.get(0);
		final String filename = getSafeFilename(uri);
		final URI list = FileApiUtils.createURI(core.getFileApi().getScheme(), uri);
		final IngestTaskHandler.Params params = new IngestTaskHandler.Params();
		params.setList(list);
		params.setLanguages(parseLanguages(args.size()>=2 ? args.get(1) : ""));
		params.setBackend(args.size() >=3 ? args.get(2) : null);
		
		final TaskHandlerId handler = core.getTasker().findHandler(IngestTaskHandler.class.getName());
		Validate.notNull(handler, "handler not registered");
		
		final Tasker tasker = core.getTasker();
		
		final String taskName = generateUniqueTaskName(tasker, filename);
		final TaskId id = tasker.createTask(taskName, handler, Schedule.once(), params);
		
		final Texter.Builder b = texter.build();
		b.append(id.toString());
		b.append(" created");
		
		lobby.post(b.toString());
	}
	
	private List<String> parseLanguages(final String languages) {
		final String [] parts = languages.split(",");
		final List<String> ret = new ArrayList<>(parts.length);
		for (String part : parts) {
			part = part.trim();
			if (!part.isEmpty()) {
				ret.add(part);
			}
		}
		return ret;
	}

	private String generateUniqueTaskName(final Tasker tasker, final String filename) {
		String taskName = filename;
		int counter = 2;
		TaskId existing = tasker.findTask(taskName);
		if (existing == null) {
			return taskName;
		}
		final Texter.Builder b = texter.build();
		b.append(filename).append("_");
		final int baselen = b.length();
		
		while (existing != null) {
			b.delete(baselen, b.length());
			b.append(String.valueOf(counter));
			taskName = b.toString();
			counter++;
			existing = tasker.findTask(taskName);
		}
		return taskName;
	}

	private String getSafeFilename(final String uri) {
		final int lastSlash = uri.lastIndexOf('/');
		return uri.substring(lastSlash+1).replaceAll("\\s+", "_");
	}

}
