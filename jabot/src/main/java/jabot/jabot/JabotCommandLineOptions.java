package jabot.jabot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.Validate;
import org.glassfish.jersey.logging.LoggingFeature;

import jabot.comcon.config.Config;
import jabot.common.props.PropsConfig;

public class JabotCommandLineOptions implements Config {
	private static final String FOOTER="Shipped configuration parameters can be overridden via configuration file\n"+
	                                   "Some of configuration file parameters can be overridden via command line";
	
	private static final String CONF_DEBUG="jabot.debug";
	private static final String CONF_CONFIG="jabot.conf";
	private static final String CONF_ENDPOINT="jabot.web-interface";
	private static final String CONF_LOG4J_FILE="log4j.appender.file.File";
	private static final String CONF_LOG4J_LEVEL="log4j.appender.file.Threshold";
	private static final String CONF_DB="jabot.db";
	private static final String CONF_LOCAL_TASK_STORE="jabot.db.tasks";
	private static final String CONF_LOG_TRAFFIC="log-traffic";
	private static final String CONF_SOLR="solr.default";
	
	
	private final Options options;
	private final PropsConfig allConfig;
	private boolean helpRequested;
	private boolean debugMode;
	private URI rsEndpoint;
	private java.io.File localDB, localTaskStore;
	private Properties log4jConfiguration;

	public JabotCommandLineOptions() throws IOException {
		this.allConfig = new PropsConfig(loadShippedConfiguration());
		
		options = new Options();
		options.addOption("h", "help", false, "display help");
		options.addOption("d", CONF_DEBUG, false, makeOptionDescr(CONF_DEBUG, "turn ON debug mode"));
		options.addOption("c", CONF_CONFIG, true, makeOptionDescr(CONF_CONFIG, "config file"));
		options.addOption("l", CONF_LOG4J_FILE, true, makeOptionDescr(CONF_LOG4J_FILE, "log file"));
		
		options.addOption("t", CONF_LOG4J_LEVEL, true, makeOptionDescr(CONF_LOG4J_FILE, 
				"logging level (TRACE|DEBUG|INFO|WARN|ERROR)"));
		
		options.addOption("", CONF_LOG_TRAFFIC, false, makeOptionDescr(CONF_LOG_TRAFFIC, "log all traffic"));
		
		options.addOption("w", CONF_ENDPOINT, true, 
				makeOptionDescr(CONF_DEBUG, "web interface endpoint, http://host:port"));

		options.addOption("b", CONF_DB, true, makeOptionDescr(CONF_DB, "local file db path (default: /var/db/jabot)"));
		
		options.addOption("s", CONF_SOLR, true, makeOptionDescr(CONF_SOLR, 
				"solr endpoint (default: embedded:/var/db/jabot, you can try http://127.0.0.1:8983/solr/core0)"));
		
	}
	
	private String makeOptionDescr(final String option, final String descr) {
		final String defaultVal = allConfig.getString(option, null);
		return descr+" [default:"+defaultVal+"]";
	}

	public boolean helpRequested() {
		return helpRequested;
	}

	public void printHelp(final String commandName, final PrintWriter pw) {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(pw, formatter.getWidth(), commandName, null, options, formatter.getLeftPadding(),
				formatter.getDescPadding(), FOOTER, true);
	}

	public void parse(String[] args) throws ParseException, IOException {
		final CommandLineParser parser = new DefaultParser();
		final CommandLine cmd = parser.parse(options, args);

		helpRequested = cmd.hasOption("help");

		final File siteConfigFile = new File(cmd.getOptionValue(CONF_CONFIG, allConfig.getString(CONF_CONFIG, null)));
		if (siteConfigFile.isFile()) {
			allConfig.override(loadSiteConfiguration(siteConfigFile));
		}

		overrideBoolean(cmd, CONF_DEBUG);
		overrideString(cmd, CONF_CONFIG);
		overrideString(cmd, CONF_ENDPOINT);
		overrideString(cmd, CONF_LOG4J_FILE);
		overrideString(cmd, CONF_LOG4J_LEVEL);
		overrideString(cmd, CONF_DB);
		overrideBoolean(cmd, CONF_LOG_TRAFFIC);
		overrideString(cmd, CONF_SOLR);
		
		overrideJerseyClientLogging();

		initConfig();
	}
	
	private void overrideJerseyClientLogging() {
		final String log4jLogLevel = allConfig.getString(CONF_LOG4J_LEVEL, "WARN");
		final String jerseyLogLevel;
		switch(log4jLogLevel) {
		case "ERROR": jerseyLogLevel = "SEVERE"; break;
		case "WARN": jerseyLogLevel = "WARNING"; break;
		case "INFO": jerseyLogLevel = "INFO"; break;
		default: jerseyLogLevel = "FINEST"; break;
		}
		allConfig.asProps().setProperty(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, jerseyLogLevel);
		
		if (allConfig.getBoolean(CONF_LOG_TRAFFIC, false)) {
			
			allConfig.asProps().setProperty(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, 
					LoggingFeature.Verbosity.PAYLOAD_ANY.name());
			
			allConfig.asProps().setProperty("org.apache.http.wire", "TRACE");
		}
	}

	private void overrideBoolean(final CommandLine cmd, final String param) {
		final boolean value = cmd.hasOption(param);
		allConfig.asProps().setProperty(param, String.valueOf(value));
	}
	
	private void overrideString(final CommandLine cmd, final String param) {
		final String value = cmd.getOptionValue(param);
		if (value != null) {
			allConfig.asProps().setProperty(param, value);
		}
	}

	public void initConfig() {
		debugMode = allConfig.getBoolean(CONF_DEBUG, false);
		rsEndpoint = URI.create(allConfig.getString("jabot.web-interface", null));
		localDB = new File(allConfig.getString(CONF_DB, "/var/db/jabot"));
		final String taskStorePath = allConfig.getString(CONF_LOCAL_TASK_STORE, null);
		localTaskStore = taskStorePath == null ? new File(localDB, "tasks") : new File(taskStorePath);
		
		log4jConfiguration = new Properties();
		for (final Map.Entry<?, ?> e : allConfig.asProps().entrySet()) {
			final String key = e.getKey().toString();
			if (key.startsWith("log4j")) {
				final String value = e.getValue().toString();
				log4jConfiguration.setProperty(key, value);
			}
		}
	}

	@Override
	public boolean isDebugMode() {
		return debugMode;
	}

	@Override
	public URI getRsEndpoint() {
		return rsEndpoint;
	}

	@Override
	public File getDB() {
		return localDB;
	}

	@Override
	public File getLocalTaskStore() {
		return localTaskStore;
	}

	@Override
	public PropsConfig allConfig() {
		return allConfig;
	}
	
	public Properties getLog4jConfiguration() {
		return log4jConfiguration;
	}
	
	private static Properties loadSiteConfiguration(final File from) throws IOException {
		try (final InputStream in = new FileInputStream(from)) {
			final Properties ret = new Properties();
			ret.load(in);
			return ret;
		}
	}

	private static Properties loadShippedConfiguration() throws IOException {
		try (final InputStream in = JabotCommandLineOptions.class.getResourceAsStream("/shippedConfig.properties")) {
			Validate.notNull(in, "Cannot read shippedConfig.properties");
			final Properties ret = new Properties();
			ret.load(new InputStreamReader(in, StandardCharsets.UTF_8));
			return ret;
		}
	}

}
