package jabot.taskri;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import jabot.taskapi.InvalidIdException;
import jabot.taskapi.TaskContext;
import jabot.taskapi.TaskHandler;
import jabot.taskapi.TaskHandlerId;
import jabot.taskapi.TaskMemento;
import jabot.taskapi.TaskParams;

public class TaskHandlerIdImplTest {
	@Mock
	TaskerRI owner1;
	
	@Mock
	TaskerRI owner2;
	
	private TaskHandlerIdImpl id;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		id = new TaskHandlerIdImpl(owner1, new Handler1());
	}
	
	@Test
	public void testToStringEquals() {
		Assert.assertEquals(id.toString(), new TaskHandlerIdImpl(owner1, new Handler1()).toString());
	}

	@Test(expected=InvalidIdException.class)
	public void testRecastWrongOwner() {
		TaskHandlerIdImpl.recast(owner2, id);
	}

	@Test(expected=InvalidIdException.class)
	public void testRecastWrongClass() {
		TaskHandlerIdImpl.recast(owner2, Mockito.mock(TaskHandlerId.class));
	}

	@Test
	public void testRecast() {
		Assert.assertSame(id, TaskHandlerIdImpl.recast(owner1, id));
	}
	
	private static final class Handler1 implements TaskHandler<TaskParams, TaskMemento> {

		@Override
		public void handle(TaskContext<TaskParams, TaskMemento> ctx) throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		public String marshallParams(TaskParams params) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TaskParams unmarshallParams(String marshalled) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String marshallMemento(TaskMemento memento) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TaskMemento unmarshallMemento(String marshalled) {
			throw new UnsupportedOperationException();
		}
		
	}
}
