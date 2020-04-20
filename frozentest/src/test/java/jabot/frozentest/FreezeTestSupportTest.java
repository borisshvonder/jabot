package jabot.frozentest;

import org.junit.Assert;
import org.junit.Test;

import jabot.frozen.Freeze;
import jabot.frozen.Freezing;

public class FreezeTestSupportTest {
	
	@Test
	public void testFrozen() {
		final FrozenObj obj = new FrozenObj();
		obj.setSubObj(new FrozenObj());
		final FrozenObj copy = FreezeTestSupport.unitTest(obj, o -> o.setValue(100));
		Assert.assertEquals(100, copy.getValue());
	}
	

	private static class FrozenObj implements Freezing<FrozenObj> {
		protected int value;
		protected FrozenObj subObj;
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
			final FrozenObj copy = new FrozenObj();
			copy.setValue(getValue());
			copy.setSubObj(subObj == null ? null : getSubObj().defrost());
			return copy;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			Freeze.ensureNotFrozen(this);
			
			this.value = value;
		}

		public FrozenObj getSubObj() {
			return subObj;
		}

		public void setSubObj(FrozenObj subObj) {
			Freeze.ensureNotFrozen(this);
			this.subObj = subObj;
		}
	}
	
	@Test(expected=AssertionError.class)
	public void testUnitTestWillThrowWhenSetterHasNoCheck() {
		FreezeTestSupport.unitTest(new FrozenObj() {
			@Override
			public void setValue(int value) {
				this.value = value;
			}
		}, o -> o.setValue(100));
	}
	
	@Test(expected=AssertionError.class)
	public void testUnitTestWillThrowWhenDefrostRetrunsNull() {
		FreezeTestSupport.unitTest(new FrozenObj() {
			@Override
			public FrozenObj defrost() {
				return null;
			}
		}, o -> o.setValue(100));
	}
	
	@Test(expected=AssertionError.class)
	public void testUnitTestWillThrowWhenDefrostRetrunsSelf() {
		FreezeTestSupport.unitTest(new FrozenObj() {
			@Override
			public FrozenObj defrost() {
				return this;
			}
		}, o -> o.setValue(100));
	}

	@Test(expected=AssertionError.class)
	public void testUnitTestWillThrowWhenDefrostRetrunsFrozen() {
		FreezeTestSupport.unitTest(new FrozenObj() {
			@Override
			public FrozenObj defrost() {
				final FrozenObj ret = super.defrost();
				ret.freeze();
				return ret;
			}
		}, o -> o.setValue(100));
	}
	
}
