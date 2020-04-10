package jabot.taskri.store.marshall;

import jabot.marshall.TypeAdapter;
import jabot.taskapi.Progress;

public class ProgressAdapter implements TypeAdapter<Progress>{

	@Override
	public Class<Progress> getHandledType() {
		return Progress.class;
	}

	@Override
	public String serialize(Progress value) {
		return value.marshall();
	}

	@Override
	public Progress deserialize(String marshalled) {
		return Progress.unmarshall(marshalled);
	}

}
