package jabot.taskri.store;

import java.io.IOException;
import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import jabot.common.surge.Surge;
import jabot.common.types.Interval;
import jabot.fileapi.std.RamFileApi;
import jabot.taskri.store.TaskStore.TaskRecord;

public class DumbFileStoreTest extends RamTaskStoreTest {
	private static final URI JOBSFILE = URI.create("testfs:/jobs.json");
	
	@Override
	protected DumbFileStore createStore() {
		final DumbFileStore result = new DumbFileStore(new RamFileApi(), JOBSFILE);
		result.setSaveInterval(Interval.MILLISECOND);
		return result;
	}
	
	@Test
	public void testSaveLoad() throws IOException {
		final RamFileApi api = new RamFileApi();
		final DumbFileStore store1 = new DumbFileStore(api, JOBSFILE);
		store1.setSaveInterval(Interval.ZERO);
		
		final Task task1 = new Task();
		task1.setMoniker("moniker1");
		
		final Task task2 = new Task();
		task2.setMoniker("moniker2");

		store1.newTask(task1);
		store1.newTask(task2);
		store1.remove(task1.getMoniker());
		
		Assert.assertTrue(api.isFile(JOBSFILE));

		final DumbFileStore store2 = new DumbFileStore(api, JOBSFILE);
		try(final Surge<TaskRecord> it = store2.listScheduledToNeverRun()) {
			final TaskRecord state = it.next();
			Assert.assertEquals(task2.getMoniker(), state.getMoniker());
			Assert.assertNull(it.next());
		}
	}
}
