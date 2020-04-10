package jabot.taskapi;

import java.util.Objects;

import org.apache.commons.lang3.Validate;

import jabot.common.Texter;

/**
 * Represents progress of the task execution
 */
public class Progress {
	private static final Texter TXT = new Texter();
	/** How many "items" were done out of total */
	private final long current;
	
	/** How many "items" are to do in total. Note it MAY change during task run, see {@link #addTotal(long)} */
	private final long total;
	
	/** How many "items" we are still waiting on */
	private final long waiting;
	
	/** How many "items" are failed to process*/
	private final long  failed;

	public Progress(final long current, final long total, final long waiting, final long failed) {
		this.current = current;
		this.total = total;
		this.waiting = waiting;
		this.failed = failed;
	}
	
	public String marshall() {
		Texter.Builder b = TXT.build();
		b.append(current).append('/').append(total).append('/').append(waiting).append('/').append(failed);
		return b.toString();
	}
	
	public static Progress unmarshall(final String from) {
		Validate.notNull(from, "from cannot be null");
		
		String [] parts = from.split("\\/");
		if (parts.length != 4) {
			throw new ProgressFormatException(from);
		}
		try {
			long current = Long.parseLong(parts[0]);
			long total = Long.parseLong(parts[1]);
			long waiting = Long.parseLong(parts[2]);
			long failed = Long.parseLong(parts[3]);
			
			return new Progress(current, total, waiting, failed);
		} catch (final NumberFormatException ex) {
			throw new ProgressFormatException(from, ex);
		}
	}

	/** Create new progress with items added to "current" counter */
	public Progress addCurrent(final long add) {
		if (add == 0) {
			return this;
		} else {
			return new Progress(current+add, total, waiting, failed);
		}
	}

	/** Create new progress with items added to "total" counter */
	public Progress addTotal(final long add) {
		if (add == 0) {
			return this;
		} else {
			return new Progress(current, total+add, waiting, failed);
		}
	}

	/** Create new progress with items added to "waiting" counter */
	public Progress addWaiting(final long add) {
		if (add == 0) {
			return this;
		} else {
			return new Progress(current, total, waiting+add, failed);
		}
	}

	/** Create new progress with items added to "failed" counter */
	public Progress addFailed(final long add) {
		if (add == 0) {
			return this;
		} else {
			return new Progress(current, total, waiting, failed+add);
		}
	}

	public long getCurrent() {
		return current;
	}

	public long getTotal() {
		return total;
	}

	public long getWaiting() {
		return waiting;
	}

	public long getFailed() {
		return failed;
	}
	
	public float getCompleteRatio() {
		return (float)current/total;
	}

	public int getPercentComplete() {
		return (int)(100*getCompleteRatio());
	}

	@Override
	public int hashCode() {
		return Objects.hash(current, total, failed, waiting);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof Progress) {
			Progress other = (Progress) obj;
			return current == other.current && 
					total == other.total &&
					failed == other.failed &&
					waiting == other.waiting;
		}
		return false;
	}

	@Override
	public String toString() {
		Texter.Builder b = TXT.build();
		b.append(current).append('/').append(total);
		
		if (failed != 0) {
			b.append('/').append(failed).append('f');
		}
		
		if (waiting != 0) {
			b.append('/').append(failed).append('w');
		}
		
		b.append('=').append(getPercentComplete()).append('%');
		
		return b.toString();
	}
}
