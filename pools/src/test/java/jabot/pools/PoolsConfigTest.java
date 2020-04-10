package jabot.pools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import jabot.common.props.PropsConfig;
import jabot.common.types.Interval;
import jabot.pools.PoolsConfig.ShutdownHook;

public class PoolsConfigTest {
	private final Properties props = new Properties();
	
	@Test
	public void testRealPool() {
		props.setProperty("pools", "pool1, pool2");
		try {
			PoolsConfig.init(makeConfig());
			Assert.assertNotNull(PoolsConfig.get("pool1"));
			Assert.assertNotNull(PoolsConfig.get("pool2"));
			
			PoolsConfig.get("pool1").execute(()->{});
			
			try {
				PoolsConfig.get("not-existing");
				Assert.fail();
			} catch (AssertionError ex){}
			
		} finally {
			PoolsConfig.shutdownAll(Interval.SECOND);
		}
	}
	
	@Test
	public void test_real_shutdownAll_will_fail_if_not_initialized() {
		try {
			PoolsConfig.shutdownAll(Interval.SECOND);
			Assert.fail();
		} catch (AssertionError ex){}
	}
	
	@Test
	public void test_real_get_will_fail_if_not_initialized() {
		try {
			PoolsConfig.get("not-existing");
			Assert.fail();
		} catch (AssertionError ex){}
	}
	
	@Test
	public void testAllPropsSuccess() throws IOException {
		final Properties props = new Properties();
		try(final InputStream in = getClass().getResourceAsStream("/PoolsConfigTest/PoolsConfigTest_allProps.properties")) {
			props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		PoolsConfig poolsConfig = new PoolsConfig(new PropsConfig(props), "pools");
		try {
			poolsConfig.initInstance();
			final ThreadPool pool0 = poolsConfig.getPool("pool0");
			Assert.assertEquals((int)(Runtime.getRuntime().availableProcessors()*1.5), pool0.getMaximumPoolSize());
			Assert.assertEquals(10, pool0.getKeepAliveTime(TimeUnit.SECONDS));
			Assert.assertTrue(pool0.getRejectedExecutionHandler() instanceof DiscardPolicy);
			Assert.assertEquals(10*Runtime.getRuntime().availableProcessors(), pool0.getQueueRemainingCapacity());
					
			NamedFactory fact = (NamedFactory)pool0.getThreadFactory();
			Assert.assertEquals("pool0", fact.getName());
			Assert.assertTrue(fact.isDaemon());
		} finally {
			poolsConfig.shutdown(Interval.MILLISECOND);
		}
	}
	
	@Test
	public void testBadRejectHandler() throws IOException {
		props.setProperty("pools", "pool0");
		props.setProperty("pools.pool0.rejectionHandler", "badhandler");
		PoolsConfig poolsConfig = new PoolsConfig(makeConfig(), "pools");
		try {
			poolsConfig.initInstance();
			Assert.fail();
		} catch (AssertionError ex) {
			// ignore
		} finally {
			poolsConfig.shutdown(Interval.MILLISECOND);
		}
	}
	
	@Test
	public void testBadRejectHandlerClass() throws IOException {
		props.setProperty("pools", "pool0");
		props.setProperty("pools.pool0.rejectionHandler", "java.lang.String");
		PoolsConfig poolsConfig = new PoolsConfig(makeConfig(), "pools");
		try {
			poolsConfig.initInstance();
			Assert.fail();
		} catch (AssertionError ex) {
			// ignore
		} finally {
			poolsConfig.shutdown(Interval.MILLISECOND);
		}
	}
	
	@Test
	public void testShutdownHook() throws IOException, InterruptedException {
		props.setProperty("pools", "pool0");
		final PoolsConfig poolsConfig = new PoolsConfig(makeConfig(), "pools");
		poolsConfig.initInstance();
		final ShutdownHook hook = new ShutdownHook(poolsConfig);
		hook.run();
		hook.join();
		Assert.assertTrue(poolsConfig.allPoolsStopped());
	}
	
	@Test(expected=AssertionError.class)
	public void testCannotHaveDuplicatedNames() throws IOException, InterruptedException {
		props.setProperty("pools", "pool0,  pool0 ");
		final PoolsConfig poolsConfig = new PoolsConfig(makeConfig(), "pools");
		poolsConfig.initInstance();
	}
	
	@Test(expected=AssertionError.class)
	public void testCannotHaveNoPools() throws IOException, InterruptedException {
		props.setProperty("pools", "");
		final PoolsConfig poolsConfig = new PoolsConfig(makeConfig(), "pools");
		poolsConfig.initInstance();
	}
	
	private PropsConfig makeConfig() {
		return new PropsConfig(props);
	}
}
