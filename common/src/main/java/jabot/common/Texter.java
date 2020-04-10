package jabot.common;

import javolution.text.Text;
import javolution.text.TextBuilder;

/**
 * Thin wrapper around javolution TextBuilder that provides adaptable initial
 * sizing. This class guarantees that (eventually) no reallocations will be
 * made.
 * 
 * @threadsafe
 */
public class Texter {
	private static final int DEFAULT_CAPACITY = 16;

	/** adaptive capacity */
	// No need to synchronize. The worst case that will happend is that more
	// reallocations will be required per thread.
	private int capacity = DEFAULT_CAPACITY;

	public Builder build() {
		return new Builder();
	}

	public final class Builder {
		private final TextBuilder backend;

		private Builder() {
			backend = new TextBuilder(capacity);
		}

		/**
		 * Appends the specified string to this text builder. If the specified
		 * string is <code>null</code> this method is equivalent to
		 * <code>append("null")</code>.
		 *
		 * @param str
		 *            the string to append or <code>null</code>.
		 * @return <code>this</code>
		 */
		public Builder append(final String str) {
			backend.append(str);
			return this;
		}

		/**
		 * Appends a subsequence of the specified string. If the specified
		 * character sequence is <code>null</code> this method is equivalent to
		 * <code>append("null")</code>.
		 *
		 * @param str
		 *            the string to append or <code>null</code>.
		 * @param start
		 *            the index of the first character to append.
		 * @param end
		 *            the index after the last character to append.
		 * @return <code>this</code>
		 * @throws IndexOutOfBoundsException
		 *             if <code>(start < 0) || (end < 0) 
		 *         || (start > end) || (end > str.length())</code>
		 */

		public Builder append(final String str, final int start, final int end) {
			backend.append(str, start, end);
			return this;
		}

		/**
		 * Appends the specified character.
		 *
		 * @param c
		 *            the character to append.
		 * @return <code>this</code>
		 */
		public Builder append(final char c) {
			backend.append(c);
			return this;
		}

		/**
		 * Appends the decimal representation of the specified <code>int</code>
		 * argument.
		 *
		 * @param i
		 *            the <code>int</code> to format.
		 * @return <code>this</code>
		 */
		public Builder append(final int i) {
			backend.append(i);
			return this;
		}

		/**
	     * Appends the decimal representation of the specified <code>long</code>
	     * argument.
	     *
	     * @param  l the <code>long</code> to format.
	     * @return <code>this</code>
	     */
		public Builder append(final long l) {
			backend.append(l);
			return this;
		}
		
	    /**
	     * Removes the characters between the specified indices.
	     * 
	     * @param start the beginning index, inclusive.
	     * @param end the ending index, exclusive.
	     * @return <code>this</code>
	     * @throws IndexOutOfBoundsException if <code>(start < 0) || (end < 0) 
	     *         || (start > end) || (end > this.length())</code>
	     */
		public Builder delete(final int start, final int end) {
			backend.delete(start, end);
			return this;
		}

		/**
		 * Returns the <code>String</code> representation of this
		 * {@link TextBuilder}.
		 *
		 * @return the <code>java.lang.String</code> for this text builder.
		 */
		public String toString() {
			adjustCapacity();
			return backend.toString();
		}
	
		/** Clear the builder */
		public void clear() {
			backend.clear();
		}

		/**
		 * Returns the {@link Text} corresponding to this {@link TextBuilder}.
		 *
		 * @return the corresponding {@link Text} instance.
		 */
		public Text toText() {
			adjustCapacity();
			return backend.toText();
		}
		
		public int length() {
			return backend.length();
		}

		private void adjustCapacity() {
			if (backend.length() > capacity) {
				capacity = backend.length();
			}
		}

	}
}
