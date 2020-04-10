package jabot.idxapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * A {@link Document} consists of set of FieldValue's
 * 
 * @immutable
 */
public final class FieldValue {
	/** @notnull field type */
	private final Field field;
	
	/** @notnull field value */
	private final Object value;
	
	public FieldValue(final String field, final Object value) {
		this(new Field(field), value);
	}

	public FieldValue(final Field field, final Object value) {
		Validate.notNull(field, "field cannot be null");
		Validate.notNull(value, "value cannot be null");
		
		this.field = field;
		this.value = value;
		
		if (ModuleAssertions.isEnabled()) {
			validate();
		}
	}
	
	public FieldValue(final String field, final Collection<?> values) {
		this(new Field(field), values);
	}
	
	public FieldValue(final Field field, final Collection<?> values) {
		Validate.notNull(field, "field cannot be null");
		Validate.notNull(values, "values cannot be null");

		this.field = field;
		this.value = protect(values);
		
		if (ModuleAssertions.isEnabled()) {
			validate();
		}
	}
	
	/**
	 * Validate field value against field type
	 */
	public void validate() {
		if (value != null) {
			if (field.isMultivalued()) {
				if (value instanceof Collection) {
					final Collection<?> asColl = (Collection<?>)value;
					for (final Object obj : asColl) {
						if (obj == null) {
							throw new FieldValueInvalidException(field, value);
						}
						validateType(obj);
					}
				} else {
					throw new FieldValueInvalidException(field, value);
				}
			} else {
				validateType(value);
			}
		}
	}
	
	private void validateType(final Object val) {
		if (val != null && val.getClass() != field.getType().javaType) {
			throw new FieldValueInvalidException(field, value);
		}
	}

	public Field getField() {
		return field;
	}

	public Object getValue() {
		return value;
	}

	private static List<Object> protect(final Collection<?> list) {
		if (list == null) {
			return null;
		} else {
			final List<Object> copy = new ArrayList<>(list);
			return Collections.unmodifiableList(copy);
		}
	}
	
	@Override
	public String toString() {
		return field.toString()+"="+value;
	}
}
