package jabot.marshall.stdadapters;

import java.util.Date;

import jabot.marshall.TypeAdapter;

/**
 * Standard type adapter for date: serializes date as long string value (millis)
 */
public class DateAdapter implements TypeAdapter<Date> {

	@Override
	public Class<Date> getHandledType() {
		return Date.class;
	}

	@Override
	public String serialize(final Date value) {
		return String.valueOf(value.getTime());
	}

	@Override
	public Date deserialize(final String marshalled) {
		return new Date(Long.parseLong(marshalled));
	}

}
