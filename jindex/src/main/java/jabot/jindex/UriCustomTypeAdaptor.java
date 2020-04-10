package jabot.jindex;

import java.net.URI;

import jabot.idxapi.Field.Type;
import jabot.idxapi.Untokenized;

public class UriCustomTypeAdaptor implements CustomTypeAdaptor<URI> {

	public static final UriCustomTypeAdaptor INSTANCE = new UriCustomTypeAdaptor();
	
	@Override
	public Type getType() {
		return Type.STRING;
	}

	@Override
	public Object fromValueToType(URI value) {
		return value.toString();
	}

	@Override
	public URI fromTypeToValue(Object converted) {
		return URI.create(converted.toString());
	}

	private UriCustomTypeAdaptor() {}
	
}
