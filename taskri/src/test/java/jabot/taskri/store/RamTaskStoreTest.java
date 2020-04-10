package jabot.taskri.store;

public class RamTaskStoreTest extends TaskStoreTestBase {

	@Override
	protected RamTaskStore createStore() {
		return new RamTaskStore();
	}

	@Override
	protected void destroyStore(TaskStore store) throws Exception {}
	
}
