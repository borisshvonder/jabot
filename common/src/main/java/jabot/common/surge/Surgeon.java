package jabot.common.surge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;

/**
 * Swiss-Army-Knife for {#link Surge}s
 */
public final class Surgeon<E> {
	private final Surge<E> backend;
	
	/**
	 * @param @notnull backend 
	 */
	public Surgeon(final Surge<E> backend) {
		Validate.notNull(backend, "backend cannot be null");
		
		this.backend = backend;
	}
	
	public static<E> Surgeon<E> of(final Surge<E> backend) {
		return new Surgeon<>(backend);
	}

	/**
	 * Adapt regular {@link Iteratpr} that does not require closing to {@link Surge}. Note that any null values 
	 * returned by the iterator will be filtered out
	 * @param @notnull it
	 * @return @notnull adapted interface
	 */
	public static<T> Surgeon<T> adapt(final Iterator<T> it) {
		Validate.notNull(it, "it cannot be null");
		
		return new Surgeon<T>(new IteratorAdapter<T>(it));
	}
	
	public static<T> Surgeon<T> open(final Collection<T> coll) {
		Validate.notNull(coll, "coll cannot be null");
		
		return adapt(coll.iterator());
	}
	
	/**
	 * Fetch entire surge, copy and return prefetched surge
	 * @return
	 */
	public static<T> Surgeon<T> prefetch(final Surge<T> surge) {
		Validate.notNull(surge, "surge cannot be null");
		
		final List<T> prefetch = new LinkedList<>();
		T elem = surge.next();
		while (elem != null) {
			prefetch.add(elem);
			elem = surge.next();
		}
		
		return open(prefetch);
	}
	
	public Surge<E> prefetch() {
		final Surgeon<E> prefetch = prefetch(get());
		return prefetch.get();
	}
	
	public Surge<E> get() {
		return backend;
	}
	
	/** 
	 * Filter a stream using a predicate
	 */
	public Surgeon<E> filter(final Predicate<? super E> predicate) {
		Validate.notNull(predicate, "predicate cannot be null");
		
		return new Surgeon<E>(new Filter<E>(backend, predicate));
	}
	
	/** 
	 * Filter a stream until predicate returns true 
	 */
	public Surgeon<E> until(final Predicate<? super E> predicate) {
		Validate.notNull(predicate, "predicate cannot be null");
		
		return new Surgeon<E>(new Until<E>(backend, predicate));
	}
	
	/**
	 * Map Surge of one type to another. Note that null values returned from mapper will be discarded.
	 * @param @notnull mapper
	 */
	public<R> Surgeon<R> map(final Function<? super E, ? extends R> mapper) {
		Validate.notNull(mapper, "mapper cannot be null");
		
		return new Surgeon<R>(new Mapper<E, R>(backend, mapper));
	}
	
	public <R> Surgeon<R> flatMap(final Function<? super E, ? extends Surgeon<? extends R>> mapper) {
		Validate.notNull(mapper, "mapper cannot be null");
		
		return new Surgeon<R>(new FlatMapper<E, R>(backend, mapper));
	}
	
	public Surgeon<E> append(final Surge<? extends E> otherSurge) {
		Validate.notNull(otherSurge, "otherSurge cannot be null");
		
		final List<Surge<? extends E>> backends = new ArrayList<>(2);
		backends.add(backend);
		backends.add(otherSurge);
		
		return new Surgeon<E>(new Appender<E>(backends));
	}

	private static final class IteratorAdapter<E> implements Surge<E> {
		private final Iterator<E> backend;
		
		public IteratorAdapter(Iterator<E> backend) {
			this.backend = backend;
		}

		@Override
		public E next() {
			E ret = null;
			while (ret == null && backend.hasNext()) {
				ret = backend.next();
			}
			return ret;
		}
		
		@Override
		public void close() {}

		
	}
	
	private static final class Filter<E> implements Surge<E> {
		private final Surge<E> backend;
		private final Predicate<? super E> predicate;

		public Filter(final Surge<E> backend, final Predicate<? super E> predicate) {
			this.backend = backend;
			this.predicate = predicate;
		}

		@Override
		public void close() throws IOException {
			backend.close();
		}

		@Override
		public E next() {
			E ret = backend.next();
			while (ret != null && !predicate.test(ret)) {
				ret = backend.next();
			}
			return ret;
		}
		
	}
	
	private static final class Until<E> implements Surge<E> {
		private final Surge<E> backend;
		private final Predicate<? super E> predicate;
		private boolean closed;

		public Until(final Surge<E> backend, final Predicate<? super E> predicate) {
			this.backend = backend;
			this.predicate = predicate;
		}

		@Override
		public void close() throws IOException {
			backend.close();
		}

		@Override
		public E next() {
			if (closed) {
				return null;
			} else {
				final E ret = backend.next();
				closed = ret == null || predicate.test(ret);
				return closed ? null : ret;
			}
		}
		
	}
	
	private static final class Mapper<FROM, TO> implements Surge<TO> {
		private final Surge<FROM> backend;
		private final Function<? super FROM, ? extends TO> mapper;
		
		public Mapper(Surge<FROM> backend, Function<? super FROM, ? extends TO> mapper) {
			this.backend = backend;
			this.mapper = mapper;
		}

		@Override
		public void close() throws IOException {
			backend.close();
		}

		@Override
		public TO next() {
			FROM from = backend.next();
			while (from != null) {
				final TO to = mapper.apply(from);
				if (to != null) {
					return to;
				}
				from = backend.next();
			}
			return null;
		}
	}
	
	private static final class FlatMapper<FROM, TO> implements Surge<TO> {
		private final Surge<FROM> backend;
		private final Function<? super FROM, ? extends Surgeon<? extends TO>> mapper;
		private Surge<? extends TO> currentSurge;
		private boolean closed;
		
		public FlatMapper(Surge<FROM> backend, Function<? super FROM, ? extends Surgeon<? extends TO>> mapper) {
			this.backend = backend;
			this.mapper = mapper;
		}

		@Override
		public void close() throws IOException {
			backend.close();
		}

		@Override
		public TO next() {
			while(!closed) {
				if (currentSurge != null) {
					TO ret = currentSurge.next();
					if (ret != null) {
						return ret;
					}
				}
				FROM from = backend.next();
				closed = from == null;
				if (!closed) {
					final Surgeon<? extends TO> toSurgeon = mapper.apply(from);
					if (toSurgeon != null) {
						currentSurge = toSurgeon.get();
					}
				}
			}
			return null;
		}
	}
	
	private static final class Appender<E> implements Surge<E> {
		private final Iterator<Surge<? extends E>> backends;
		private Surge<? extends E> current;

		public Appender(final List<Surge<? extends E>> backends) {
			this.backends = backends.iterator();
		}

		@Override
		public E next() {
			if (current == null) {
				current = nextBackend();
			}
			E ret = null;
			while (current != null && ret == null) {
				ret = current.next();
				if (ret == null) {
					current = nextBackend();
				}
			}
			return ret;
		}
		
		@Override
		public void close() throws IOException {
			while (current != null) {
				current.close();
				current = nextBackend();
			}
		}

		private Surge<? extends E> nextBackend() {
			return backends.hasNext() ? backends.next() : null;
		}
		
	}
}
