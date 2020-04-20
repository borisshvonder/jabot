package jabot.idxapi;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import jabot.frozen.Freeze;
import jabot.frozen.Freezing;

public class Document implements Freezing<Document> {
	/** @notnull primary key */
	private final Untokenized pk;
	
	private final List<FieldValue> fields = new LinkedList<>();
	private final List<FieldValue> fieldsRO = Collections.unmodifiableList(fields);
	
	private volatile boolean frozen;

	public Document(final Untokenized pk) {
		Validate.notNull(pk, "pk cannot be null");
		
		this.pk = pk;
	}
	
	public Untokenized getPk() {
		return pk;
	}

	public void add(final FieldValue value) {
		Freeze.ensureNotFrozen(this);
		
		// It is an error to add two values for the same field, but it will not be detected for perfomance reasons here
		// and should be don by the implementation of the search API
		fields.add(value);
	}
	
	public List<FieldValue> getFields() {
		return fieldsRO;
	}

	@Override
	public boolean isFrozen() {
		return frozen;
	}

	@Override
	public void freeze() {
		frozen = true;
	}

	@Override
	public Document defrost() {
		final Document ret = new Document(pk);
		for (final FieldValue v : fields) {
			ret.add(v);
		}
		return ret;
	}
	
	public FieldValue getValue(final String fieldFullName) {
		Validate.notNull(fieldFullName, "fieldFullName cannot be null");
		
		for(final FieldValue fv : fields) {
			if (fieldFullName.equals(fv.getField().getName())) {
				return fv;
			}
		}
		
		return null;
	}
}
