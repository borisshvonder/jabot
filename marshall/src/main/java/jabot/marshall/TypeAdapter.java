package jabot.marshall;

import java.text.ParseException;

/**
 * Type adapters convert values to/from strings. 
 */
public interface TypeAdapter<T> {
	
	/**
	 * Note that TypeAdapter is not polymorhic, that is it will not handle subtypes of the type
	 * 
	 * @return @notnull class of the handled type
	 */
	Class<T> getHandledType();
	
	/**
	 * Serialize value to string
	 * 
	 * @param @notnull value
	 * @return @nullable result
	 */
	String serialize(T value);
	
	/**
	 * @param @notnull marshalled
	 * @return @nullabe deserialized value
	 */
	T deserialize(String marshalled) throws ParseException;
}
