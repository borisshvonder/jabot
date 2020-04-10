package jabot.pools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.common.props.PropsConfig;
import jabot.common.types.Interval;

/**
 * Single-point static thread pools configuration
 */
public class PoolsConfig {
	private static AtomicReference<PoolsConfig> INSTANCE = new AtomicReference<>();
	private static final Logger LOG = LoggerFactory.getLogger(PoolsConfig.class);
	private static final String CPUX_TOKEN = "CPUX";
	private final String propBase;
	private final PropsConfig config;
	private Map<String, ThreadPool> pools;
	
	public static void init(final PropsConfig config) {
		init(config, "pools");
	}

	public static void init(final PropsConfig config, final String propBase) {
		Validate.notNull(config, "config cannot be null");
		Validate.notNull(propBase, "propBase cannot be null");

		if (INSTANCE.get() != null) {
			throw new AssertionError("Already initialized");
		}
		PoolsConfig instance = new PoolsConfig(config, propBase);
		if (!INSTANCE.compareAndSet(null, instance)) {
			throw new AssertionError("Already initialized");
		}

		instance.initInstance();
	}
	
	public static void shutdownAll(final Interval interval) {
		PoolsConfig instance = sureGet();
		sureGet().shutdown(interval);
		INSTANCE.compareAndSet(instance, null);
	}

	public static ThreadPool get(final String name) {
		return sureGet().getPool(name);
	}
	
	private static PoolsConfig sureGet() {
		PoolsConfig instance = INSTANCE.get();
		if (instance == null) {
			throw new AssertionError("Not initialized");
		}
		return instance;
	}

	/** @visibleForTesting */
	PoolsConfig(final PropsConfig config, final String propBase) {
		this.config =config;
		this.propBase = propBase;
		Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
	}

	/** @visibleForTesting */
	void initInstance() {
		final String allPoolNames = config.requireString(propBase);
		LOG.debug("poolNames={}", allPoolNames);
		final List<String> poolNames = parsePoolNames(allPoolNames);
		this.pools = new HashMap<>(poolNames.size());

		for (String poolName : poolNames) {
			LOG.debug("Creating pool {}", poolName);
			final ThreadPool pool;
			try {
				pool = createPool(poolName);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
				throw new AssertionError(ex);
			}
			LOG.info("Created pool {}", poolName);
			pools.put(poolName, pool);
		}
		LOG.info("All pools initialized");
	}

	/** @visibleForTesting */
	void shutdown(Interval waitTime) {
		if (pools == null) {
			LOG.debug("No pools to shutdown");
			return;
		}
		for (final Map.Entry<String, ThreadPool> entry : pools.entrySet()) {
			try {
				entry.getValue().shutdown();
			} catch (Exception ex) {
				safeErrorLog("Error while shutting down pool {}", entry.getKey(), ex);
			}
		}
		
		long sleepTime = waitTime.asMillis();
		final long pollTime = Interval.SECOND.asMillis();
		boolean alldone = false;
		while (!alldone && sleepTime > 0) {
			alldone = false;
			try {
				Thread.sleep(pollTime);
				sleepTime -= pollTime;
			} catch (Exception ex) {
				safeErrorLog("Interrrupted while sleeping", ex);
				alldone = true;
			}
			
			alldone = alldone || allPoolsStopped();
		}

		for (final Map.Entry<String, ThreadPool> entry : pools.entrySet()) {
			try {
				entry.getValue().shutdownNow();
			} catch (Exception ex) {
				safeErrorLog("Error while shutdownNow pool {}", entry.getKey(), ex);
			}
		}
		
		try {
			LOG.info("All pools shut down");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** @visibleForTesting */
	boolean allPoolsStopped() {
		for (final Map.Entry<String, ThreadPool> entry : pools.entrySet()) {
			try {
				if (!entry.getValue().isTerminated()) {
					return false;
				}
			} catch (Exception ex) {
				safeErrorLog("Error while checking pool {} status", entry.getKey(), ex);
			}
		}
		return true;
	}

	private void safeErrorLog(String format, Object... objects) {
		try {
			LOG.error(format, objects);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** @visibleForTesting */
	ThreadPool getPool(String name) {
		ThreadPool ret = pools.get(name);
		if (ret == null) {
			throw new AssertionError("No pool " + name + " configured");
		}
		return ret;
	}

	private List<String> parsePoolNames(String allPoolNames) {
		final String[] names = allPoolNames.split(",");
		List<String> ret = new ArrayList<>(names.length);
		for (String name : names) {
			name = name.trim();
			if (name.isEmpty()) {
				throw new AssertionError("Empty pool name: "+allPoolNames);
			}
			if (ret.contains(name)) {
				throw new AssertionError("Duplicate pool name " + name + " in " + allPoolNames);
			}
			ret.add(name);
		}
		return ret;
	}

	private ThreadPool createPool(final String poolName)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		final String base = propBase + "." + poolName;

		final int corePoolSize = parseIntExpression(config.getString(base + ".corePoolSize", "0"));
		final int maximumPoolSize = parseIntExpression(config.getString(base + ".maximumPoolSize", "1"));
		final int queueSize = parseIntExpression(config.getString(base + ".queueSize", "1"));
		final boolean daemon = config.getBoolean(base + ".daemon", false);
		final Interval keepAlive = config.getInterval(base + ".keepAlive", Interval.MINUTE);
		final String rejectionHandler = config.getString(base + ".rejectionHandler", CallerRunsPolicy.class.getName());

		final Class<?> handlerClass = Class.forName(rejectionHandler);
		if (!RejectedExecutionHandler.class.isAssignableFrom(handlerClass)) {
			throw new AssertionError("class " + rejectionHandler + " is not RejectedExecutionHandler");
		}
		final RejectedExecutionHandler handlerInstance = (RejectedExecutionHandler) handlerClass.newInstance();

		return new ThreadPool(corePoolSize, maximumPoolSize, keepAlive.getValue(), keepAlive.getUnit(),
				new LinkedBlockingQueue<Runnable>(queueSize), new NamedFactory(poolName, daemon), handlerInstance);
	}
	
	private int parseIntExpression(String expr) {
		final String normalized = expr.trim().toUpperCase();
		if (normalized.startsWith(CPUX_TOKEN)) {
			final float factor = Float.parseFloat(normalized.substring(CPUX_TOKEN.length()));
			return (int)(Runtime.getRuntime().availableProcessors()*factor);
		} else {
			return Integer.parseInt(normalized);
		}
	}

	static class ShutdownHook extends Thread {
		private final PoolsConfig config;
		
		public ShutdownHook(PoolsConfig config) {
			this.config = config;
		}

		@Override
		public void run() {
			config.shutdown(new Interval(10, TimeUnit.SECONDS));
		}

	}

}
