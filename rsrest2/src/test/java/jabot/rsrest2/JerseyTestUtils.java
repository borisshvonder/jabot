package jabot.rsrest2;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JerseyTestUtils {
	private static final Logger LOG = LoggerFactory.getLogger(JerseyTestUtils.class);
	private static final ObjectMapper MAPPER;
	static {
		MAPPER = new ObjectMapper();
		MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
	}

	
	/**
	 * Build a mocked application/json response
	 * 
	 * @param json raw json, single quotes and unquoted identifiers allowed
	 */
	public static Response json(String json) {
		// Using parser allows us to take in non-conformant single-quoted json and convert it to conforming one
		try {
			JsonNode tree = MAPPER.readTree(json);
			return Response.ok(tree.toString()).type(MediaType.APPLICATION_JSON).build();
		} catch (Exception ex) {
			LOG.error("jsonFromResource("+json+")", ex);
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Build a mocked application/json response from the resource at given path (MUST be UTF-8 encoded)
	 * @param path
	 * @return
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 * @throws JsonSyntaxException 
	 * @throws JsonIOException 
	 */
	public static Response jsonFromResource(String path) {
		// Using parser allows us to take in non-conformant single-quoted json and convert it to conforming one
		try (InputStream in = JerseyTestUtils.class.getResourceAsStream(path)) {
			Validate.notNull(in, "Resource not found: {}", path);
			JsonNode tree = MAPPER.readTree(in);
			return Response.ok(tree.toString()).type(MediaType.APPLICATION_JSON).build();
		} catch (Exception ex) {
			LOG.error("jsonFromResource("+path+")", ex);
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Read posted json object
	 */
	public static JsonNode readUTF8JsonFromPost(InputStream in) {
		// Better to convert to string: easier to debug
		String json = null;
		try {
			json = IOUtils.toString(in, StandardCharsets.UTF_8);
			return MAPPER.readTree(json);
		} catch (Exception ex) {
			LOG.error("readUTF8JsonFromPost("+json+")", ex);
			throw new RuntimeException(ex);
		}
	}
	
	private JerseyTestUtils() {}
}
