package jabot.metapi.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jabot.metapi.Metadata;

/** Serializes {@link Metadata} to/deserializes from .meta.txt files.
 * .meta.txt files are utf-8 java property files which contains:
 * <ul>
 *  <li>key=value strings</ul>
 *  <li>key=[listval1, listval2,...] lists </ul>
 * </ul>
 *
 * BUGS: Does not properly handle list elements that have comma ',' in them. 
 *       Can't handle scalar values that start with '[' and end with ']'
 */
public final class MetadataSerializer {
	public static Metadata read(final InputStream from) throws IOException {
		final Reader utf8 = new InputStreamReader(from, StandardCharsets.UTF_8);
		final Properties props = new Properties();
		props.load(utf8);
		
		return fromProperties(props);
	}
	
	public static Metadata fromProperties(final Properties props) {
		final Metadata ret = new Metadata();
		for (final Map.Entry<?, ?> entry : props.entrySet()) {
			final String key = entry.getKey().toString().trim();
			final String value = entry.getValue().toString().trim();
			
			if (!key.isEmpty() && !value.isEmpty()) {
				if (value.startsWith("[") && value.endsWith("]")) {
					addList(ret, key, value);
				} else {
					ret.add(key, value);
				}
			}
		}
		
		return ret;
	}
	
	public static void write(final Metadata data, final OutputStream to) throws IOException {
		final Writer w = new OutputStreamWriter(to, StandardCharsets.UTF_8);
		final Properties props = toProperties(data);
		props.store(w, "metapi .meta.txt");
		w.flush();
	}
	
	private static Properties toProperties(final Metadata data) {
		final Properties ret = new Properties();
		
		final StringBuilder b = new StringBuilder();
		
		for (final Map.Entry<String, List<String>> entry : data.entrySet()) {
			final String key = entry.getKey();
			final List<String> value = entry.getValue();
			if (value.size()<=1) {
				final String scalar = value.get(0);
				ret.setProperty(key, scalar);
			} else {
				b.append('[');
				for (final String v : value) {
					if (b.length()>1) {
						b.append(", ");
					}
					b.append(v);
				}
				b.append(']');
				ret.setProperty(key, b.toString());
				b.delete(0, b.length());
			}
		}

		return ret;
	}

	private static void addList(final Metadata ret, final String key, final String value) {
		final List<String> list = new LinkedList<>();
		int start = 1; //skip [
		int end = value.indexOf(',', start+1);
		while(end>0) {
			final String element = substring(value, start, end);
			list.add(element);
			
			start = end+1;
			end = value.indexOf(',', start+1);
		}
		end = value.length()-1;
		if (start < end) {
			final String lastElement = substring(value, start, end);
			list.add(lastElement);
		}
		
		if (!list.isEmpty()) {
			ret.set(key, list);
		}
	}

	private static String substring(final String str, final int start, final int end) {
		int s = start;
		int e = end-1;
		
		while (s<=e) {
			final char c = str.charAt(s);
			if (!Character.isWhitespace(c)) {
				break;
			}
			s++;
		}
		
		while (s<=e) {
			final char c = str.charAt(e);
			if (!Character.isWhitespace(c)) {
				break;
			}
			e--;
		}
		
		if (s>e) {
			return "";
		} else {
			return str.substring(s, e+1);
		}
	}

	private MetadataSerializer() { /* utility class*/ }
}
