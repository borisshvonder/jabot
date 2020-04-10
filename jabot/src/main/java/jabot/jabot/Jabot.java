package jabot.jabot;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.comcon.CmdExecutor;
import jabot.comcon.ServiceCore;
import jabot.common.types.Interval;
import jabot.fileapi.std.StdFileApi;
import jabot.idxsolr.SolrIndexManager;
import jabot.jabot.commands.AboutCmd;
import jabot.jabot.commands.BookCmd;
import jabot.jabot.commands.CommandsCmd;
import jabot.jabot.commands.HelpCmd;
import jabot.jabot.commands.IndexstatsCmd;
import jabot.jabot.commands.IngestCmd;
import jabot.jabot.commands.KillCmd;
import jabot.jabot.commands.StopCmd;
import jabot.jabot.commands.TasksCmd;
import jabot.jabot.commands.VersionCmd;
import jabot.jabotmodel.Metafiler;
import jabot.jabotmodel.MetafilerImpl;
import jabot.jindex.DefaultCompositeJindex;
import jabot.jindex.DefaultJindex;
import jabot.jindex.Jindex;
import jabot.metika.TikaExtractor;
import jabot.pools.PoolsConfig;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;
import jabot.rsrest2.RetroshareV2;
import jabot.taskapi.Schedule;
import jabot.taskapi.TaskHandlerId;
import jabot.taskapi.Tasker;
import jabot.taskri.TaskerRI;
import jabot.taskri.exec.ThreadPoolAdapter;
import jabot.taskri.stdhandlers.CleanupTaskHandler;
import jabot.taskri.store.DumbFileStore;
import jabot.tasks.IngestTaskHandler;

public class Jabot {
	private static final Logger LOG = LoggerFactory.getLogger(Jabot.class);
	private static final int HIGHEST_PRIORITY = 1000;
	private static final long IDLE_SLEEP = 2000L;
	private final ServiceCore services = new ServiceCore();
	private final JabotCommandLineOptions options;
	private StreamLobby console;
	private List<Lobby> allLobbies = new ArrayList<>();
	
	public Jabot(String[] args) throws IOException, ParseException {
		options = new JabotCommandLineOptions();
		options.parse(args);
		services.setConfig(options);
	}
	
	public void run() throws Exception  {
		try {
			PropertyConfigurator.configure(options.getLog4jConfiguration());
			PoolsConfig.init(options.allConfig());
			initDB();
			initMetafiler();
			initIndexes();
			initFileApi();
			initRetroshare();
			initCommands();
			attachLobbies();
			attachConsole();
			clearLobbies();
			initTasker();
			mainLoop();
		} finally {
			final Tasker tasker = services.getTasker();
			if (tasker != null) {
				tasker.shutdown();
			}

			final SolrIndexManager manager = services.getSolrManager();
			if (manager != null) {
				manager.close();
			}

			PoolsConfig.shutdownAll(Interval.MINUTE);
		}
	}
	
	private void initDB() {
		if (!options.getDB().exists()) {
			if (!options.getDB().mkdirs()) {
				throw new RuntimeException("Cannot create "+options.getDB());
			}
		} else if (!options.getDB().isDirectory()) {
			throw new RuntimeException(options.getDB()+" is not a folder");
		}
	}

	private void initMetafiler() {
		final TikaExtractor metadataExtractor = new TikaExtractor();
		services.setMetadataExtractor(metadataExtractor);
		
		final Metafiler metafiler = new MetafilerImpl(metadataExtractor);
		services.setMetafiler(metafiler);
	}

	private void initIndexes() throws IOException {
		final SolrIndexManager manager = new SolrIndexManager(options.allConfig());
		services.setSolrManager(manager);
		manager.init();
		
		final Collection<String> components = manager.listComponents();
		final DefaultCompositeJindex composite = new DefaultCompositeJindex(components.size());
		for (final String component : components) {
			final Jindex jindex = new DefaultJindex(manager.getIndex(component));
			composite.addComponent(component, jindex, manager.getParams(component));
		}
		services.setJindex(composite);
	}

	private void initRetroshare() {
		services.setRetroshare(new RetroshareV2(options.allConfig(), options.getRsEndpoint()));
	}

	private void initFileApi() {
		services.setFileApi(new StdFileApi());
	}
	
	private void initTasker() {
		final DumbFileStore taskStore = new DumbFileStore(new StdFileApi(), options.getLocalTaskStore().toURI());
		services.setTaskStore(taskStore);
		
		final TaskerRI tasker = new TaskerRI();
		tasker.setStore(taskStore);
		tasker.setExecutor(new ThreadPoolAdapter(PoolsConfig.get("tasker")));
		services.setTasker(tasker);
		tasker.startup();
		
		final CleanupTaskHandler cleaner = new CleanupTaskHandler();
		cleaner.setStore(tasker.getStore());
		final TaskHandlerId cleanupHandler = tasker.registerHandler(cleaner);
		if (tasker.findTask("CleanupTaskHandler") == null) {
			final CleanupTaskHandler.Params params = new CleanupTaskHandler.Params();
			params.setOlderThan(Interval.DAY);
			tasker.createTask("CleanupTaskHandler", cleanupHandler, Schedule.delay(Interval.DAY), params);
		}
		
		final IngestTaskHandler ingester = new IngestTaskHandler(options.allConfig());
		ingester.setFileApi(services.getFileApi());
		ingester.setJindex(services.getJindex());
		ingester.setMetafiler(services.getMetafiler());
		tasker.registerHandler(ingester);
	}

	private void attachConsole() {
		final Reader in = new InputStreamReader(System.in); // default encoding ON PURPOSE!
		console = new StreamLobby(in, System.out);
		services.getCommandExecutor().setLobbyPriority(console, HIGHEST_PRIORITY);
		allLobbies.add(console);
		LOG.info("System console attached");
	}
	private void attachLobbies() {
		boolean success = false;
		while(!success) {
			try {
				allLobbies.clear();
				allLobbies.addAll(services.getRetroshare().getSubscribedLobbies());
				success = true;
			} catch (Exception ex) {
				LOG.warn("Error attaching lobbies, retrying", ex);
			}
		}
	}


	private void clearLobbies() {
		try {
			for (final Lobby lobby : allLobbies) {
				try {
					lobby.clearMessages();
				} catch (Exception ex) {
					LOG.warn("Error while clearing messages from lobby {}", lobby.getName(), ex);
				}
			}
		} catch (Exception ex) {
			LOG.warn("Error while clearing lobbies", ex);
		}
	}

	private void mainLoop() throws InterruptedException {
		LOG.info("Executing commands");
		
		while (!services.isStopRequested()) {
			int messagesReceived = 0;
			
			try {
				processMessages();
			} catch (Exception ex) {
				LOG.warn("Error while processing messages", ex);
			}

			if (messagesReceived == 0) {
				LOG.debug("No messages, sleeping");
				Thread.sleep(IDLE_SLEEP);
			} else {
				LOG.debug("Processed {} messages", messagesReceived);
			}
		}
	}

	private int processMessages() throws IOException {
		int messagesReceived = processMessages(console);
		
		for (final Lobby lobby : allLobbies) {
			messagesReceived += processMessages(lobby);
		}
		
		return messagesReceived;
	}

	private int processMessages(Lobby lobby) throws IOException {
		int ret = 0;
		for (final ReceivedMessage msg : lobby.readMessages()) {
			try {
				services.getCommandExecutor().processMessage(lobby, msg);
				ret++;
			} catch (final Exception ex) {
				LOG.warn("Error processing message {} in lobby {} text: {}", msg.getId(), lobby.getName(), msg.getText());
			}
		}
		return ret;
	}

	private void initCommands() {
		final CmdExecutor executor = new CmdExecutor(services);
		services.setCommandExecutor(executor);
		
		executor.addCmd(0, new HelpCmd());
		executor.addCmd(0, new CommandsCmd());
		executor.addCmd(0, new AboutCmd());
		executor.addCmd(0, new VersionCmd());
		executor.addCmd(0, new TasksCmd());
		//executor.addCmd(0, new FileCmd()); not tested
		executor.addCmd(0, new BookCmd());
		executor.addCmd(0, new IndexstatsCmd());
		executor.addCmd(100, new IngestCmd());
		executor.addCmd(100, new KillCmd());
		executor.addCmd(100, new StopCmd());
	}

	public static void main(final String [] args) {
		try {
			final Jabot jabot = new Jabot(args);
			final JabotCommandLineOptions opts = (JabotCommandLineOptions)jabot.services.getConfig();
			if (opts.helpRequested()) {
				PrintWriter out = new PrintWriter(System.out);
				opts.printHelp("java -jar jabot-XXX.jar", out);
				out.flush();
			} else {
				jabot.run();
			}
		} catch (Exception ex) {
			LOG.error("Fatal error, exiting", ex);
		}
	}
}
