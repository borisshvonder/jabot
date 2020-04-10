package jabot.fileapi;

import java.net.URI;
import java.util.regex.Pattern;

import javolution.text.TextBuilder;

public class FileApiUtils {
	private static final Pattern SCHEME = Pattern.compile(".*?:/");
	
	/**
	 * Create URI from string. If scheme is missing, apply defaultScheme
	 * @param @notnull url
	 * @param @nullable defaultScheme
	 * @return
	 */
	public static URI createURI(final String defaultScheme, final String url) {
		if (defaultScheme != null && !SCHEME.matcher(url).lookingAt()) {
			String normalized = normalizePath(url);
			if ("file".equals(defaultScheme)) {
				final java.io.File asFile = new java.io.File(normalized);
				return asFile.toURI();
			} else {
				final TextBuilder b = new TextBuilder(normalized.length());
				b.append(defaultScheme).append("://").append(normalized);
				return URI.create(b.toString());
			}
		} else {
			return URI.create(url);
		}
	}
	
	public static String normalizePath(final String path) {
		final TextBuilder b = new TextBuilder(path.length());

		final String [] parts = path.split("/");
		
		if (path.startsWith("/")) {
			//preserve slash
			b.append('/');
		}
		
		boolean first = true;
		for (final String part : parts) {
			if (!part.isEmpty() && !part.equals(".")) {
				if (first) {
					first = false;
				} else {
					b.append('/');
				}
				b.append(part);
			}
		}
		
		if (path.endsWith("/")) {
			// preserve trailing slash
			b.append('/');
		}
		
		return b.toString();
	}
}
