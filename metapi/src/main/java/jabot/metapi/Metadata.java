package jabot.metapi;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import javolution.util.FastMap;

public class Metadata {
	private final FastMap<String, List<String>> backend = new FastMap<>();

	public List<String> get(final String key) {
		Validate.notNull(key, "key cannot be null");
		
		return backend.get(key);
	}
	

	public String getSingle(final String key) {
		final List<String> vals = get(key);
		return vals == null || vals.isEmpty() ? null : vals.get(0);
	}

	public void set(final String key, final String value) {
		set(key, Arrays.asList(value));
	}
	
	public void set(final String key, final List<String> value) {
		Validate.notNull(key, "key cannot be null");
		
		if (value == null) {
			backend.remove(key);
		} else {
			backend.put(key, value);
		}
	}
	
	public void add(final String key, final String value) {
		Validate.notNull(key, "key cannot be null");
		Validate.notNull(value, "value cannot be null");
		
		List<String> list = backend.get(key);
		if (list == null) {
			list = new LinkedList<>();
			backend.put(key, list);
		}
		if (!list.contains(value)) {
			list.add(value);
		}
	}
	
	public Set<Map.Entry<String, List<String>>> entrySet() {
		return backend.entrySet();
	}

}
