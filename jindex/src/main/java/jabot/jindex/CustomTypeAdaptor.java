package jabot.jindex;

import jabot.idxapi.Field.Type;

public interface CustomTypeAdaptor<T> {
	Type getType();
	
	Object fromValueToType(T value);
	
	T fromTypeToValue(Object converted);
}
