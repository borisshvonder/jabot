package jabot.idxapi;

import java.util.Objects;

import org.apache.commons.lang3.Validate;

/** Wrapper around string that applies no tokenization */
public final class Untokenized {
	private final String text;

	public Untokenized(final String text) {
		Validate.notNull(text, "text cannot be null");
		
		this.text = text;
	}

	public String getText() {
		return text;
	}

	@Override
	public int hashCode() {
		return text == null ? 0 : text.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof Untokenized) {
			final Untokenized unt = (Untokenized) obj;
			return Objects.equals(text, unt.text);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return text;
	}
	
	
}
