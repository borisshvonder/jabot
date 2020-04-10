package jabot.comcon.config;

import java.io.File;
import java.net.URI;

import jabot.common.props.PropsConfig;

public interface Config {
	/** True if all debug functions enabled */
	boolean isDebugMode();
	
	/** Local file database path (/var/db/jabot) */
	File getDB();

	/** Local file task store (/var/db/jabot/tasks */
	File getLocalTaskStore();

	/** Retroshare REST endpoint, http://host:port **/
	URI getRsEndpoint();
	
	/** All configuration parameters as single budle */
	PropsConfig allConfig();
	
}