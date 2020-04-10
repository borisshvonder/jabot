package jabot.taskri.store.marshall;

import jabot.common.types.Timestamp;
import jabot.marshall.TypeAdapter;

public class TimestampAdapter implements TypeAdapter<Timestamp> {

	@Override
	public Class<Timestamp> getHandledType() {
		return Timestamp.class;
	}

	@Override
	public String serialize(final Timestamp value) {
		return String.valueOf(value.asMillis());
	}

	@Override
	public Timestamp deserialize(String marshalled) {
		return Timestamp.fromUTCMillis(Long.parseLong(marshalled));
	}

}
