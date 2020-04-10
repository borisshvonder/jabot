package jabot.idxapi;

import java.time.OffsetDateTime;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

import jabot.common.Texter;
import jabot.common.Texter.Builder;

/**
 * Each {@link Document} indexed by the search api consists of set of fields 
 * with values ({@link FieldValue}.
 * 
 * Fields which have same name but different type or different {@link #storage} options are
 * considered DIFFERENT fields.
 * 
 * By convention, field names should conform to following schema:
 * <code>BASICNAME_TYPE_OPTIONS_MV</code>
 * 
 * Where BASICNAME is field identifier, TYPE is one of the {@link Type} enumeration typeNames, OPTION is one of the
 * {@link Storage} names and _MV is "_mv" in case field is multivalued, empty otherwise.
 * 
 * For example, following full name <code>firstName_str_f_mv</code> has basic name "firstName", 
 * type {@value Type#STRING}, storage option {@link Storage#STORED_INDEXED} and is multivalued field, while 
 * <code>weight_int<int> has basic name "weight", type {@link Type#INT}, storage options {@link Storage#INDEXED} and 
 * is not multivalued
 * 
 * @immutable
 */
public final class Field {
	public static enum Type {
		BINARY(Binary.class, "binary"),
		UNTOKENIZED(Untokenized.class, "unt"),
		INT(Integer.class, "int"),
		LONG(Long.class, "long"),
		FLOAT(Float.class, "float"),
		DOUBLE(Double.class, "double"),
		DATETIME(OffsetDateTime.class, "date"),
		STRING(String.class, "str"), // MUST BE BEFORE ALL OTHER STRING_XX
		STRING_RU(String.class, "strru");
		
		public final Class<?> javaType;
		public final String typeName;
		
		private Type(final Class<?> javaType, final String typeName) {
			this.javaType = javaType;
			this.typeName = typeName;
		}
		
		public static Type fromName(final String typeName) {
			for (final Type t : values()) {
				if (t.typeName.equals(typeName)) {
					return t;
				}
			}
			return null;
		}
		
		public static Type fromJavaType(final Class<?> javaType) {
			final Class<?> boxedType = ClassUtils.primitiveToWrapper(javaType);
			for (final Type t : Type.values()) {
				if (boxedType == t.javaType) {
					return t;
				}
			}
			return null;
		}

		private static Class<?> unbox(final Class<?> javaType2) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	public static enum Storage { 
		STORED("s"), 
		INDEXED(""), 
		STORED_INDEXED("f");
		
		public final String name;

		private Storage(final String name) {
			this.name = name;
		}

		public static Storage fromName(final String name) {
			for (final Storage s : values()) {
				if (s.name.equals(name)) {
					return s;
				}
			}
			return null;
		}
	}
	
	private static final Texter FIELDNAME_TXT = new Texter();
	
	/** @lazyinit field type. Only a selected set of types is permitted, see {@link FieldValue} **/
	private Type type;
	
	/** @lazyinit field name */
	private String name;
	
	/** @lazyinit field basic name */
	private String basicName;

	/** @lazyinit field storage option */
	private Storage storage;
	
	/** @lazyinit multivalued field or not */
	private boolean multivalued;
	
	public Field(final String basicName, final Type type, final Storage storage, final boolean multivalued) {
		Validate.notNull(basicName, "basicName cannot be null");
		Validate.notNull(type, "type cannot be null");
		Validate.notNull(storage, "storage cannot be null");
		
		this.type = type;
		this.basicName = basicName;
		this.storage = storage;
		this.multivalued = multivalued;
		
		if (ModuleAssertions.isEnabled()) {
			validate();
		}
	}
	
	public Field(final String name) {
		Validate.notNull(name, "name cannot be null");
		
		this.name = name;
		if (ModuleAssertions.isEnabled()) {
			validate();
		}
	}

	/** Initialize any lazy-init variable and check correctness
	 * @throws FieldFormatException if field name format invalid
	 */
	public void validate() {
		if (name == null) {
			rebuildName();
		} else {
			parseName();
		}
	}
	
	public String getBasicName() {
		parseName();
		return basicName;
	}

	public Type getType() {
		parseName();
		return type;
	}

	public Storage getStorage() {
		parseName();
		return storage;
	}
	
	public boolean isMultivalued() {
		parseName();
		return multivalued;
	}

	public String getName() {
		rebuildName();
		return name;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof Field) {
			final Field other = (Field) obj;
			return getName().equals(other.getName());
		}
		return false;
	}

	@Override
	public String toString() {
		return getName();
	}
	
	private void rebuildName() {
		if (name == null) {
			final Builder b = FIELDNAME_TXT.build();
			b.append(basicName).append('_').append(type.typeName);
			if (!storage.name.isEmpty()) {
				b.append('_').append(storage.name);
			}
			if (multivalued) {
				b.append("_mv");
			}
			this.name = b.toString();
		}
	}

	private void parseName() {
		if (type == null) {
			final NameTokenizer tokenizer = new NameTokenizer(name);
			String token = tokenizer.ensureNextToken();
			
			boolean multi = false;
			if ("mv".equals(token)) {
				multi = true;
				token = tokenizer.ensureNextToken();
			}
			
			Storage s = Storage.fromName(token);
			if (s == null) {
				s = Storage.INDEXED;
			} else {
				token = tokenizer.ensureNextToken();
			}
			
			Type t = Type.fromName(token);
			if (t == null) {
				throw new FieldFormatException(name);
			}
			
			String basic = tokenizer.rest();
			if (basic.isEmpty()) {
				throw new FieldFormatException(name);
			}

			this.basicName = basic;
			this.type = t;
			this.storage = s;
			this.multivalued = multi;
		}
	}

	private static final class NameTokenizer {
		private final String name;
		int lastDelim;
		
		public NameTokenizer(final String name) {
			this.name = name;
			this.lastDelim=name.length();
		}
		
		public String ensureNextToken() {
			final String ret = nextToken();
			if (ret == null) {
				throw new FieldFormatException(name);
			}
			return ret;
		}
		
		public String nextToken() {
			if (lastDelim<=0) {
				return null;
			}
			int nextDelim = name.lastIndexOf('_', lastDelim-1);
			final String ret = name.substring(nextDelim+1, lastDelim);
			lastDelim = nextDelim;
			return ret;
		}
		
		public String rest() {
			if (lastDelim<=0) {
				return "";
			}
			final String ret = name.substring(0, lastDelim);
			lastDelim=-1;
			return ret;
		}
	}
}
