package jabot.jabot.commands;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.comcon.Cmd;
import jabot.comcon.ServiceCore;
import jabot.rsapi.Lobby;
import jabot.rsapi.ReceivedMessage;

/**
 * "/VERSION" command handler
 */
public class VersionCmd implements Cmd {
	private static final Logger LOG = LoggerFactory.getLogger(VersionCmd.class);
	private static volatile String VERSION;

	@Override
	public String getName() {
		return "VERSION";
	}

	@Override
	public String getHelp() {
		return "Print current program version";
	}

	@Override
	public void execute(ServiceCore core, Lobby lobby, ReceivedMessage message, List<String> args) throws IOException {
		String version = VERSION;
		if (version == null) {
			synchronized(this) {
				version = VERSION;
				if (version == null) {
					version = readVersionFromManifest();
					VERSION = version;
				}
			}
		}
		lobby.post(version);
	}

	private String readVersionFromManifest() {
		try {
			try(InputStream in = getClass().getResourceAsStream("/META-INF/MANIFEST.MF")) {
				Validate.notNull(in, "Manifest not found");
				Properties props = new Properties();
				props.load(in);
				String version = props.getProperty("Jabot-Version");
				Validate.notNull(version, "Jabot-Version key cannot be null");
				return version;
			}
		} catch (Exception ex) {
			LOG.warn("Can't determin program version from manifest file", ex);
			return "UNKNOWN";
		}
	}

}
