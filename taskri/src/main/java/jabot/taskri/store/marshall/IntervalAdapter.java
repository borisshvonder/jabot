package jabot.taskri.store.marshall;

import jabot.common.types.Interval;
import jabot.marshall.TypeAdapter;

public class IntervalAdapter implements TypeAdapter<Interval> {

	@Override
	public Class<Interval> getHandledType() {
		return Interval.class;
	}

	@Override
	public String serialize(final Interval value) {
		return value.marshall();
	}

	@Override
	public Interval deserialize(final String marshalled) {
		return Interval.unmarshall(marshalled);
	}

}
