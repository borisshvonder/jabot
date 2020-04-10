package jabot.taskri;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import jabot.taskapi.InvalidIdException;
import jabot.taskapi.TaskId;
import jabot.taskri.store.TaskStore.TaskRecord;

public class TaskIdImplTest {
	@Mock
	TaskerRI owner1;
	
	@Mock
	TaskerRI owner2;
	
	@Mock
	TaskRecord state;
	
	private TaskIdImpl id;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		
		when(state.getMoniker()).thenReturn("job");
		
		id = new TaskIdImpl(owner1, state);
	}

	@Test
	public void testToStringEquals() {
		Assert.assertEquals(id.toString(), new TaskIdImpl(owner1, state).toString());
	}

	@Test(expected=InvalidIdException.class)
	public void testRecastWrongOwner() {
		TaskIdImpl.recast(owner2, id);
	}

	@Test(expected=InvalidIdException.class)
	public void testRecastWrongClass() {
		TaskIdImpl.recast(owner2, Mockito.mock(TaskId.class));
	}

	@Test
	public void testRecast() {
		Assert.assertSame(id, TaskIdImpl.recast(owner1, id));
	}

}
