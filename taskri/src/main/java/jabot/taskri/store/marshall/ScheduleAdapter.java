package jabot.taskri.store.marshall;

import jabot.marshall.TypeAdapter;
import jabot.taskapi.Schedule;

public class ScheduleAdapter implements TypeAdapter<Schedule> {

	@Override
	public Class<Schedule> getHandledType() {
		return Schedule.class;
	}

	@Override
	public String serialize(Schedule value) {
		return value.marshall();
	}

	@Override
	public Schedule deserialize(String marshalled){
		return Schedule.unmarshall(marshalled);
	}
}
