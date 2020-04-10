package jabot.taskri;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import jabot.common.types.Interval;
import jabot.taskapi.Tasker;
import jabot.taskapi.tests.TaskerTestBase;
import jabot.taskri.exec.Executor;
import jabot.taskri.store.TaskStore;

public class TaskerRITest extends TaskerTestBase {

	@Override
	protected Tasker createTasker() {
		final TaskerRI result = new TaskerRI();
		result.setThrottleIdle(Interval.MILLISECOND);
		return result;
	}

	@Override
	protected void dispose(Tasker tasker) {}
	
	@Test
	public void testGettersSetters() {
		final TaskerRI fixture = new TaskerRI();
		
		final TaskStore store = Mockito.mock(TaskStore.class);
		final Executor executor = Mockito.mock(Executor.class);
		
		fixture.setStore(store);
		Assert.assertSame(store, fixture.getStore());

		fixture.setExecutor(executor);
		Assert.assertSame(executor, fixture.getExecutor());
	}


}
