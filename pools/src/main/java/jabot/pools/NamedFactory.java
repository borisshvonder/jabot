package jabot.pools;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javolution.text.TextBuilder;

class NamedFactory implements ThreadFactory {
	private final String name;
	private final int maxNameSize;
	private final boolean daemon;
	private final AtomicInteger count = new AtomicInteger();
	
	public NamedFactory(String name, boolean daemon) {
		this.name = name;
		this.daemon = daemon;
		this.maxNameSize = name == null ? 0 : (name+"-"+Integer.MAX_VALUE).length();
	}
	
	public String getName() {
		return name;
	}

	public boolean isDaemon() {
		return daemon;
	}

	@Override
	public Thread newThread(Runnable r) {
		TextBuilder nameBuilder = new TextBuilder(maxNameSize);
		nameBuilder.append(name);
		nameBuilder.append('-');
		nameBuilder.append(count.incrementAndGet());
		
		Thread ret = new Thread(r);
		ret.setName(nameBuilder.toString());
		ret.setDaemon(daemon);
		return ret;
	}
	
}