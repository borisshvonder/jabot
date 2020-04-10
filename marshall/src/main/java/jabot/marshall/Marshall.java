package jabot.marshall;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import jabot.marshall.stdadapters.DateAdapter;

/**
 * Marshalls object to/from json, supports optional type adapters and static configuration.
 * 
 * This class easies replacing the backend implementation (Jackson) to some extent, but not completely since some of the
 * methods produce {@link JsonNode}, which are Jackson-specific.
 * 
 * @threadsafe
 */
public class Marshall {
	private static final Marshall INSTANCE;
	static {
		INSTANCE = new Marshall()
				.clone()
				.setIngnoreTransient(true)
				.supressNullFields(true)
				.clone();
		
		INSTANCE.configure().addTypeAdapter(new DateAdapter());
	}
	private final Configuration configuration;
	private volatile JacksonImplementation impl;
	
	public static Marshall get() {
		return INSTANCE;
	}
	
	public Marshall() {
		this(new Configuration());
	}
	
	private Marshall(final Configuration configuration) {
		this.configuration = configuration;
	}
	
	public Configurator configure() {
		return new Configurator(this);
	}
	
	public Cloner clone() {
		return new Cloner(this);
	}
	
	/**
	 * Convert arbitrary javabean to json
	 * @param @notnull javabean
	 * @return @notnull json
	 */
	public String toJson(final Object javabean) {
		Validate.notNull(javabean, "javabean cannot be null");
		
		try {
			return impl().mapper.writeValueAsString(javabean);
		} catch (final JsonMappingException ex) {
			throw new MarshallingException(ex, javabean);
		} catch (final JsonProcessingException ex) {
			throw new AssertionError("No IOExceptions should happen since processing is done in-memory", ex);
		} catch (final RuntimeException ex) {
			throw new MarshallingException(ex, javabean);
		}
	}
	
	/**
	 * Convert arbitrary javabean to json
	 * @param @notnull javabean
	 * @param @notnull out write output to this stream
	 * @throws IOException 
	 */
	public void toStream(final Object javabean, final OutputStream out) throws IOException {
		Validate.notNull(javabean, "javabean cannot be null");
		Validate.notNull(out, "out cannot be null");
		
		try {
			impl().mapper.writeValue(out, javabean);
		} catch (final JsonProcessingException ex) {
			throw new AssertionError("No IOExceptions should happen since processing is done in-memory", ex);
		} catch (final RuntimeException ex) {
			throw new MarshallingException(ex, javabean);
		}
	}
	
	/**
	 * Deserialize json back to javabean
	 * @param @notnull json
	 * @param @notnull klass
	 * @return 
	 */
	public <T> T fromJson(final String json, final Class<T> javaBeanClass) {
		Validate.notNull(json, "json cannot be null");
		Validate.notNull(javaBeanClass, "javaBeanClass cannot be null");
		
		try {
			return impl().mapper.readValue(json, javaBeanClass);
		} catch (final JsonParseException | JsonMappingException | RuntimeException ex) {
			throw new UnmarshallingException(ex, json);
		} catch (final IOException ex) {
			throw new AssertionError("No IOExceptions should happen since processing is done in-memory", ex);
		}
	}

	/**
	 * Deserialize json back to javabean
	 * @param @notnull in inspustream to read from
	 * @param @notnull klass
	 * @return 
	 * @throws IOException 
	 */
	public <T> T fromStream(final InputStream in, final Class<T> javaBeanClass) throws IOException {
		Validate.notNull(in, "in cannot be null");
		Validate.notNull(javaBeanClass, "javaBeanClass cannot be null");
		
		try {
			return impl().mapper.readValue(in, javaBeanClass);
		} catch (final JsonParseException | JsonMappingException | RuntimeException ex) {
			throw new UnmarshallingException(ex, "<InputStream>");
		}
	}
	
	private JacksonImplementation impl() {
		JacksonImplementation ret = impl;
		if (ret == null) {
			synchronized(this) {
				ret = impl;
				if (ret == null) {
					ret = configuration.implement();
					impl = ret;
				}
			}
		}
		return ret;
	}
	/**
	 * This configurator is "safe" in a sense that it allows "adding" features and "relaxing" the Marshall while not 
	 * allowing to "remove" feature or "constrain" the Marshall any further. 
	 * 
	 * The configurator applies changes to the current Marshall instance only. If you need to constrain the Marshall,
	 * use Cloner instead.
	 *
	 */
	public static class Configurator {
		protected final Marshall marshall;
	
		protected Configurator(Marshall marshall) {
			this.marshall = marshall;
		}

		/** Allow single quotes to be used in place of double quotes (not quite JSON-conformant, but useful when
		 *  json is placed into double-quoted strings to avoid massive backslashing);
		 */
		public Configurator allowSingleQuotes() {
			internalSetSingleQuotes(true);
			return this;
		}
		
		/** Allow field names not to be quoted at all */
		public Configurator allowUnquotedFields() {
			internalSetUnquotedFields(true);
			return this;
		}
		
		/** Don't fail if json has a property that bean does not during deserialization (ignore unknown fields) */
		public Configurator allowUnknownProperties() {
			internalSetUnknownProperties(true);
			return this;
		}
		
		/** Do not output null fields */
		public Configurator supressNullFields(final boolean suppress) {
			marshall.configuration.setSuppressNullFields(suppress);
			marshall.impl = null;
			return this;
		}
		
		/**
		 * @throws IllegalArgumentException if conflicting adapter already registered
		 */
		public Configurator addTypeAdapter(TypeAdapter<?> adapter) {
			final TypeAdapter<?> existing = marshall.configuration.addTypeAdapter(adapter, false);
			if (existing != null) {
				throw new IllegalArgumentException("Conflicting type adapters for "+adapter.getHandledType()+": "+adapter+" and "+existing);
			} else {
				marshall.impl = null;
				return this;
			}
		}
		
		protected void internalSetSingleQuotes(final boolean allow) {
			marshall.configuration.setSingleQuotes(allow);
			marshall.impl = null;
		}
		
		protected void internalSetUnquotedFields(final boolean allow) {
			marshall.configuration.setUnquotedFields(allow);
			marshall.impl = null;
		}
		
		protected void internalSetUnknownProperties(final boolean allow) {
			marshall.configuration.setUnknownProperties(allow);
			marshall.impl = null;
		}
	}
	
	/**
	 * The cloner gives out a copy of Marshall, thus it is safe to use all configuration methods on that copy
	 */
	public static final class Cloner extends Configurator {

		protected Cloner(final Marshall proto) {
			super(cloneMarshall(proto));
		}
		
		private static Marshall cloneMarshall(final Marshall proto) {
			return new Marshall(proto.configuration.clone());
		}
		
		public Marshall clone() {
			return new Marshall(super.marshall.configuration.clone());
		}

		public Cloner setSingleQuotes(final boolean allow) {
			marshall.configuration.setSingleQuotes(allow);
			// no need to: marshall.impl = null;
			return this;
		}
		
		public Cloner setUnquotedFields(final boolean allow) {
			marshall.configuration.setUnquotedFields(allow);
			// no need to: marshall.impl = null;
			return this;
		}
		
		public Cloner setUnknownProperties(final boolean allow) {
			marshall.configuration.setUnknownProperties(allow);
			// no need to: marshall.impl = null;
			return this;
		}
		
		public Cloner setIngnoreTransient(final boolean ignore) {
			marshall.configuration.setIgnoreTransient(ignore);
			// no need to: marshall.impl = null;
			return this;
		}
		
		public Cloner supressNullFields(final boolean suppress) {
			marshall.configuration.setSuppressNullFields(suppress);
			// no need to: marshall.impl = null;
			return this;
		}
		
		/**
		 * @return existing adapter if any
		 */
		public Cloner replaceTypeAdapter(TypeAdapter<?> adapter) {
			marshall.configuration.addTypeAdapter(adapter, true);
			marshall.impl = null;
			return this;
		}

		@Override
		public Cloner allowSingleQuotes() {
			return (Cloner)super.allowSingleQuotes();
		}

		@Override
		public Cloner allowUnquotedFields() {
			return (Cloner)super.allowUnquotedFields();
		}
		
		@Override
		public Cloner allowUnknownProperties() {
			return (Cloner)super.allowUnknownProperties();
		}

		@Override
		public Cloner addTypeAdapter(TypeAdapter<?> adapter) {
			return (Cloner)super.addTypeAdapter(adapter);
		}

	
	}
	
	protected static final class Configuration {
		private boolean suppressNullFields = false;
		private boolean singleQuotes = false;
		private boolean unquotedFields = false;
		private boolean unknownProperties = false;
		private boolean ignoreTransient = false;
		
		// Using list instead of a map is much more memory-effective, besides type adapters are rarely changed.
		private final List<TypeAdapter<?>> typeAdapters = new LinkedList<>();
		
		public synchronized Configuration clone() {
			final Configuration ret = new Configuration();
			ret.suppressNullFields = suppressNullFields;
			ret.singleQuotes = singleQuotes;
			ret.unquotedFields = unquotedFields;
			ret.unknownProperties = unknownProperties;
			ret.ignoreTransient = ignoreTransient;
			ret.typeAdapters.addAll(typeAdapters);
			return ret;
		}
		
		public boolean isSuppressNullFields() {
			return suppressNullFields;
		}

		public void setSuppressNullFields(boolean suppressNullFields) {
			this.suppressNullFields = suppressNullFields;
		}

		public synchronized boolean isSingleQuotes() {
			return singleQuotes;
		}
		
		public synchronized void setSingleQuotes(final boolean singleQuotes) {
			this.singleQuotes = singleQuotes;
		}
		
		public synchronized boolean isUnquotedFields() {
			return unquotedFields;
		}
		
		public synchronized void setUnquotedFields(final boolean unquotedFields) {
			this.unquotedFields = unquotedFields;
		}
		
		public synchronized boolean isUnknownProperties() {
			return unknownProperties;
		}

		public synchronized void setUnknownProperties(boolean unknownProperties) {
			this.unknownProperties = unknownProperties;
		}
		
		public synchronized  boolean isIgnoreTransient() {
			return ignoreTransient;
		}

		public synchronized void setIgnoreTransient(boolean ignoreTransient) {
			this.ignoreTransient = ignoreTransient;
		}

		/**
		 * @return existing adapter if any
		 */
		public synchronized TypeAdapter<?> addTypeAdapter(final TypeAdapter<?> adapter, boolean replaceExisting) {
			final Class<?> klass = adapter.getHandledType();
			
			TypeAdapter<?> existing = null;
			
			final Iterator<TypeAdapter<?>> it = typeAdapters.iterator();
			while (it.hasNext()) {
				final TypeAdapter<?> t = it.next();
				if (klass.equals(t.getHandledType())) {
					existing = t;
					if (replaceExisting) {
						it.remove();
					} else {
						return existing;
					}
				}
			}
			
			typeAdapters.add(adapter);
			return existing;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public synchronized JacksonImplementation implement() {
			final JacksonImplementation ret = new JacksonImplementation();
			
			for (TypeAdapter<?> adapter : typeAdapters) {
				final Class<?> klass = adapter.getHandledType();
				ret.module.addDeserializer(klass, new DeserializationAdapter(adapter));
				ret.module.addSerializer(klass, new SerializationAdapter(adapter));
			}
			ret.mapper.registerModule(ret.module);
			ret.mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, singleQuotes);
			ret.mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, unquotedFields);
			ret.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, !unknownProperties);
			ret.mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, ignoreTransient);
			if (suppressNullFields) {
				ret.mapper.setSerializationInclusion(Include.NON_NULL);
			}
			
			return ret;
		}
	}
	
	private static final class JacksonImplementation {
		private final ObjectMapper mapper = new ObjectMapper();
		private final SimpleModule module = new SimpleModule();
	}
	
	private static final class DeserializationAdapter<T> extends StdDeserializer<T> {
		private static final long serialVersionUID = 1L;
		private final TypeAdapter<T> adapter;
		
		public DeserializationAdapter(TypeAdapter<T> adapter) {
			super(adapter.getHandledType());
			
			this.adapter = adapter;
		}

		@Override
		public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			final JsonNode node = p.getCodec().readTree(p);
			if (node instanceof TextNode) {
				String marshalled = ((TextNode)node).asText();
				if (marshalled == null) {
					return null;
				} else {
					try {
						return adapter.deserialize(marshalled);
					} catch (final ParseException ex) {
						throw new JsonParseException(p, "adapter error", ex);
					}
				}
			} else {
				throw new JsonParseException(p, "Expected string, got: "+node);
			}
		}

		
		
	}
	
	private static final class SerializationAdapter<T> extends StdSerializer<T> {
		private static final long serialVersionUID = 1L;
		private final TypeAdapter<T> adapter;
		
		public SerializationAdapter(TypeAdapter<T> adapter) {
			super(adapter.getHandledType());
			
			this.adapter = adapter;
		}

		@Override
		public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
			String marshalled = adapter.serialize(value);
			if (marshalled == null) {
				gen.writeNull();
			} else {
				gen.writeString(marshalled);
			}
		}
		
	}
}
