package jabot.idxapi;

/**
 * Turns on/off full assertions mode for idxapi package. If java assertions enabled, enables assertions. If java 
 * assertions disabled, disables assertions. Regardless of the assertions setting, the assertions can be 
 * enabled/disabled using java environment variable <code>-Djabot.idxapi.ModuleAssertions=true</code>
 * or <code>-Djabot.idxapi.ModuleAssertions=false</code>
 */
public class ModuleAssertions {
	private static boolean enabled;
	static {
		assert enable();
		final String envParam = System.getProperty("jabot.idxapi.ModuleAssertions");
		if (envParam != null) {
			enabled = Boolean.parseBoolean(envParam);
		}
	}
	
	public static boolean isEnabled() {
		return enabled;
	}
	
	public static void enable(boolean enable) {
		enabled = enable;
	}
	
	private static boolean enable() {
		enable(true);
		return true;
	}
}
