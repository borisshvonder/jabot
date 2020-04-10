package jabot.common.props;

import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jabot.common.types.Interval;

/**
 * Simple configuration via .propetry files
 */
public class PropsConfig {
	private final Properties props = new Properties();

	public PropsConfig() {}
	
	public PropsConfig(final Properties props) {
		override(props);
	}

	public void override(final Properties otherProps) {
		for (final Map.Entry<?, ?> e : otherProps.entrySet()) {
			final String key = e.getKey().toString();
			final String value = e.getValue().toString();
			props.setProperty(key, value);
		}
	}
	
	public Properties asProps() {
		return props;
	}
	
	/** Return all keys beginnging with prefix 
	 * 
	 * @param @nullable prefix if null or empty - return all keys
	 */
	public Collection<String> keysWithPrefix(final String prefix) {
		final String normalizedPrefix = prefix == null ? "" : prefix;
		
		final List<String> ret = new LinkedList<>();
		final Enumeration<?> allKeys = props.keys();
		while (allKeys.hasMoreElements()) {
			final String key = allKeys.nextElement().toString();
			if (key.startsWith(normalizedPrefix)) {
				ret.add(key);
			}
		}
		return ret;
	}

	/**
	 * Read property
	 * @param @notnull key
	 * @return @notnull property string value
	 * @throws MissingPropertyException if property does not exists
	 */
	public String requireString(final String key) {
		final String ret = props.getProperty(key);
		if (ret == null) {
			throw new MissingPropertyException(key);
		}
		return ret;
	}
	
	public String getString(final String key, final String defaultValue) {
		final String ret = props.getProperty(key);
		if (ret == null) {
			return defaultValue;
		}
		return ret;
	}

	public Integer getInt(final String key, final Integer defaultValue) {
		String s = props.getProperty(key);
		if (s == null) {
			return defaultValue;
		} else {
			return Integer.parseInt(s);
		}
	}
	
	public Long getLong(final String key, final Long defaultValue) {
		String s = props.getProperty(key);
		if (s == null) {
			return defaultValue;
		} else {
			return Long.parseLong(s);
		}
	}
	
	public Interval getInterval(final String key, final Interval defaultValue) {
		String s = props.getProperty(key);
		if (s == null) {
			return defaultValue;
		} else {
			return Interval.unmarshall(s);
		}
	}

	public Boolean getBoolean(final String key, final Boolean defaultValue) {
		String s = props.getProperty(key);
		if (s == null) {
			return defaultValue;
		} else {
			s = s.trim().toLowerCase();
			switch(s) {
			case "true": case "y": case "yes": return true;
			case "false": case "n": case "no": return false;
			default: throw new BooleanFormatException(s);
			}
		}
	}
}
