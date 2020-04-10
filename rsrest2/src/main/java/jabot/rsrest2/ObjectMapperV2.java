package jabot.rsrest2;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
public class ObjectMapperV2 implements ContextResolver<ObjectMapper> {
	private static final ObjectMapper MAPPER;
	static {
		MAPPER = new ObjectMapper();
		MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	@Override
	public ObjectMapper getContext(Class<?> type) {
		return getMapper();
	}

	public static ObjectMapper getMapper() {
		return MAPPER;
	}
}
