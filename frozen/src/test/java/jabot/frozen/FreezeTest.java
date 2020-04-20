package jabot.frozen;

import org.junit.Test;

public class FreezeTest {
	
	@Test
	public void testNotFrozen() {
		final FrozenObj obj = new FrozenObj();
		Freeze.ensureNotFrozen(obj);
	}
	
	@Test(expected=FrozenException.class)
	public void testFrozen() {
		final FrozenObj obj = new FrozenObj();
		obj.freeze();
		Freeze.ensureNotFrozen(obj);
	}
	

	private static class FrozenObj implements Freezing<FrozenObj> {
		private transient volatile boolean frozen;
		
		@Override
		public boolean isFrozen() {
			return frozen;
		}

		@Override
		public void freeze() {
			frozen = true;
		}

		@Override
		public FrozenObj defrost() {
			return new FrozenObj();
		}
		
	}

}
