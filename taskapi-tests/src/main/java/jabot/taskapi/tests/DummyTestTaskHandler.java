package jabot.taskapi.tests;

import org.apache.commons.lang3.Validate;

import jabot.taskapi.TaskContext;
import jabot.taskapi.TaskHandler;
import jabot.taskapi.TaskMemento;
import jabot.taskapi.TaskParams;

/** FOR TESTING ONLY */
public class DummyTestTaskHandler implements TaskHandler<DummyTestTaskHandler.Params, DummyTestTaskHandler.Memento> {
	public static final String THROW_ERROR="throw";
	
	@Override
	public void handle(final TaskContext<Params, Memento> ctx) throws Exception {
		ctx.setProgress(ctx.getProgress().addCurrent(1));
		final String value = ctx.getParams() == null ? null : ctx.getParams().value;
		if (THROW_ERROR.equals(value)) {
			throw new RuntimeException("message");
		}
	}

	@Override
	public String marshallParams(final Params params) {
		Validate.notNull(params, "params cannot be null");
		
		return params.value;
	}

	@Override
	public Params unmarshallParams(final String marshalled) {
		Validate.notNull(marshalled, "marshalled cannot be null");

		final Params ret = new Params();
		ret.value = marshalled;
		return ret;
	}

	@Override
	public String marshallMemento(Memento memento) {
		Validate.notNull(memento, "memento cannot be null");
		return memento.value;
	}

	@Override
	public Memento unmarshallMemento(final String marshalled) {
		Validate.notNull(marshalled, "marshalled cannot be null");

		final Memento ret = new Memento();
		ret.value = marshalled;
		return ret;
	}

	public static final class Params implements TaskParams {
		public String value;
	}
	
	public static final class Memento implements TaskMemento {
		public String value;
	}
}
