package jabot.jindex;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.beanutils.FluentPropertyBeanIntrospector;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.idxapi.Document;
import jabot.idxapi.Field;
import jabot.idxapi.Field.Storage;
import jabot.idxapi.Field.Type;
import jabot.idxapi.FieldValue;
import jabot.idxapi.Untokenized;
/**
 * Introspects (via reflection) a java class and maps object fields to {@link Field}s
 * 
 * How it supposed to be used:
 * <ul>
 *   <li>Each class that is supposed to be indexed via {@link Jindex} should have static initialized that 
 *       configures and sets appropriate {@Link Mapper} for the class into {@link ModelMappersInventory}:
 *       <code><pre>
 *       public class MyClass {
 *         static {
 *           ModelIntrospector&lt;MyClass&gt; introspector = new ModelIntrospector&lt;&gt;(MyClass.class);
 *           introspector.getMapping("field").setType(...);
 *           introspector.getMapping("field").setStorage(Storage.STORED_INDEXED);
 *           Mapper&lt;MyClass&gt; mapper = instrospector.buildMapper();
 *           MyClass inflatedObject = ...;
 *           mapper.taste(inflatedObject);
 *           ModelMappersInventory.registerMapper(mapper);
 *         }
 *       ...
 *       </pre></code>
 *   </li>
 *   
 *   <li>Due to type erasure, the type for java Collections cannot be determined from the class. You can either
 *       explicitly specify it using <code>introspector.getMapping("field").setType(...);</code>, or (preferred and
 *       more elegant approach) you can create a "sample" inflated object with all collections populated with at 
 *       least one element. Then you can use {@link Mapper#taste(Object)} method to finalize type resolving</li>
 *       
 *   <li>Maps currently are not supported (RATIONALE: the set of searcheable fields should be finite. You have to
 *       specify explicitly which fields you want to search on)</li>
 *       
 *   <li>Any unsupported type (including maps) will still be coerced to {@value Type#TEXT}. That means that the
 *       field will be searcheable, but, most likely, won't be retrievable</li>
 *       
 *   <li>If you don't specify an explicit static mapper initialization in your class, then default rules will apply:
 *       the {@link ModelMappersInventory} will create a default mapper with all collections/unsupposted types 
 *       coerced to {@value Type#TEXT}</li>
 * </ul>
 * @treadunsafe
 */
public class ModelIntrospector<T> {
	private static final String CLASS_FIELD_NAME="class";
	private static final Logger LOG = LoggerFactory.getLogger(FieldMapping.class);
	private static final Untokenized EMPTY_UNTOKENIZED = new Untokenized("");
	private static final PropertyUtilsBean PROPUTILS;
	static {
		PROPUTILS = new PropertyUtilsBean();
		PROPUTILS.addBeanIntrospector(new FluentPropertyBeanIntrospector());
	}
	private final Class<T> model;
	private final Map<String, MutableMapping> mappings;

	public ModelIntrospector(final Class<T> model) {
		this.model = model;
		final PropertyDescriptor[] descriptors = PROPUTILS.getPropertyDescriptors(model);
		this.mappings = new HashMap<>(descriptors.length-1);
		for (final PropertyDescriptor descriptor : descriptors) {
			final String name = descriptor.getName();
			if (!CLASS_FIELD_NAME.equals(name)) {
				mappings.put(descriptor.getName(), new MutableMapping(model, descriptor));
			}
		}
	}
	
	public MutableMapping getMapping(final String fieldName) {
		return mappings.get(fieldName);
	}
	
	public Mapper<T> buildMapper() {
		return new Mapper<T>(model, mappings.values());
	}
	
	/** @immutable */
	public static class Mapper<T> {
		private final Class<T> model;
		private final Map<String, FieldMapping> forwardMappings;
		private final Map<String, FieldMapping> reverseMappings;
		private final AtomicReference<List<Field>> storedFields = new AtomicReference<>();
		private final FieldValue classFieldValue;
		
		public Mapper(final Class<T> model, final Collection<MutableMapping> mappings) {
			Validate.notNull(model, "model cannot be null");
			Validate.notNull(mappings, "mappings cannot be null");
			
			this.model = model;
			
			// All mappings are FORWARD (from bean to doc) 
			// but only some of them are REVERSE (doc to bean)
			Map<String, FieldMapping> unmutable = new HashMap<>(mappings.size());
			int reverseCount = 0;
			for (final MutableMapping mutable : mappings) {
				if (!mutable.ignore) {
					final FieldMapping mapping = mutable.buildMapping();
					unmutable.put(mapping.getBasicName(), mapping);
					
					final Storage storage = mapping.storage;
					if (storage != Storage.INDEXED) {
						reverseCount++;
					}
				}
			}
			forwardMappings = Collections.unmodifiableMap(unmutable);
			
			unmutable = new HashMap<>(reverseCount);
			for (final FieldMapping mapping : forwardMappings.values()) {
				final Storage storage = mapping.storage;
				if (storage != Storage.INDEXED) {
					reverseCount++;
					unmutable.put(mapping.getBasicName(), mapping);
				}
			}
			reverseMappings = Collections.unmodifiableMap(unmutable);
			
			classFieldValue = new FieldValue(new Field("class", Type.UNTOKENIZED, Storage.STORED_INDEXED, false), 
					new Untokenized(model.getName()));
		}
		
		public Class<T> getModel() {
			return model;
		}
		
		public FieldValue getClassFieldValue() {
			return classFieldValue;
		}

		/**
		 * instead of manually specifying each field type, you can create a dummy
		 * fully-populated (inflated) model object and let the Mapper figure out all
		 * collection types
		 * 
		 * @param @notnull inflatedModel
		 */
		public void taste(final T inflatedModel) {
			bean2doc(EMPTY_UNTOKENIZED, inflatedModel);
		}
		
		public FieldMapping getMapping(String fieldBasicName) {
			return forwardMappings.get(fieldBasicName);
		}
		
		public Map<String, FieldMapping> getAllMappings() {
			return forwardMappings;
		}
		
		public List<Field> getStoredFields() {
			List<Field> result = storedFields.get();
			if (result == null) {
				synchronized(storedFields) {
					final List<Field> stored = new ArrayList<>(reverseMappings.size());
					for(final FieldMapping mapping : reverseMappings.values()) {
						stored.add(mapping.getField());
					}
					result = Collections.unmodifiableList(stored);
					if (!storedFields.compareAndSet(null, result)) {
						result = storedFields.get();
					}
				}
			}
			return result;
		}
		
		public Document bean2doc(final Untokenized pk, final T bean) {
			Validate.notNull(pk, "pk cannot be null");
			Validate.notNull(bean, "bean cannot be null");
			
			final Document ret = new Document(pk);
			ret.add(classFieldValue);
			for (final FieldMapping mapping : forwardMappings.values()) {
				final FieldValue fv = mapping.fromBean(bean);
				if (fv != null) {
					ret.add(fv);
				}
			}
			
			return ret;
		}
		
		public T doc2bean(final Document doc) {
			Validate.notNull(doc, "doc cannot be null");
			
			try {
				final T bean = model.newInstance();
				for (final FieldValue fv : doc.getFields()) {
					if (!classFieldValue.getField().equals(fv.getField())) {
						final String fieldName = fv.getField().getBasicName();
						final FieldMapping mapping = reverseMappings.get(fieldName);
						if (mapping == null) {
							LOG.warn("{}: Field mapping for {} not found, field is ignored", model, fieldName);
						} else {
							mapping.toBean(bean, fv);
						}
					}
				}
				return bean;
			} catch (final InstantiationException | IllegalAccessException ex) 
			{
				throw new AssertionError("Can't create "+ model +
						" value, this is not supposed to happen since I have reflected on this class", ex);
			}
		}
	}
	
	public static class MutableMapping {
		private final Class<?> model;
		private final PropertyDescriptor objectField;
		private final boolean multivalued;
		private String basicName;
		private Type type;
		private Storage storage;
		private boolean ignore;
		private CustomTypeAdaptor customAdaptor;

		/** if true, search this field when no field specified in query (default field) */
		private boolean defaultField = true;
		
		public MutableMapping(final Class<?> model, final PropertyDescriptor objectField) {
			this.model = model;
			this.objectField = objectField;
			this.multivalued = Collection.class.isAssignableFrom(objectField.getPropertyType());
			this.basicName = objectField.getName();
			this.type = resolveType(objectField);
			this.storage = Storage.INDEXED;
		}

		private final Type resolveType(final PropertyDescriptor desc) {
			final Class<?> propertyType = desc.getPropertyType();
			final CustomTypeAdaptor<?> custom = tryResolveCustomTypeAdaptor(propertyType); 
			if (custom != null) {
				final Type customType = custom.getType();
				if (LOG.isDebugEnabled()) {
					LOG.debug("{}: Detected field {} {} type as {} with custom type adaptor {}", 
						new Object[] {model, propertyType, desc.getName(), customType, custom});
				}
				this.customAdaptor = custom;
				return customType;
			}
			
			final Type precise = Type.fromJavaType(propertyType);
			if (precise != null) {
				LOG.debug("{}: Detected field {} {} type as {}", 
						new Object[] {model, propertyType, desc.getName(), precise});
				return precise;
			} else if (Collection.class.isAssignableFrom(propertyType)) {
				
				LOG.info("Type erasure prevents from detecting type of collection field {} {}, deferring the decision",
						propertyType, desc.getName());
				
				return null;
			} else {
				LOG.warn("Cannot precisely map field {} {}, deferring the decision", propertyType, desc.getName());
				// last-chance
				return null;
			}
		}
		
		private static CustomTypeAdaptor<?> tryResolveCustomTypeAdaptor(final Class<?> propertyType) {
			if (propertyType == URI.class) {
				return UriCustomTypeAdaptor.INSTANCE;
			} else {
				return null;
			}
		}

		FieldMapping buildMapping() {
			return new FieldMapping(objectField, basicName, type, storage, multivalued, customAdaptor, defaultField);
		}
		
		public String getBasicName() {
			return basicName;
		}

		public MutableMapping setBasicName(String basicName) {
			this.basicName = basicName;
			return this;
		}

		public Type getType() {
			return type;
		}

		public MutableMapping setType(Type type) {
			this.type = type;
			return this;
		}

		public Storage getStorage() {
			return storage;
		}
		
		public MutableMapping setStorage(Storage storage) {
			this.storage = storage;
			return this;
		}
		
		public boolean isDefaultField() {
			return defaultField;
		}

		public void setDefaultField(boolean defaultField) {
			this.defaultField = defaultField;
		}

		/** Do not index this field anyhow */
		public void ignore() {
			this.ignore = true;
		}

		public boolean isMultivalued() {
			return multivalued;
		}

		public CustomTypeAdaptor getCustomAdaptor() {
			return customAdaptor;
		}

		public MutableMapping setCustomAdaptor(final CustomTypeAdaptor customAdaptor) {
			this.customAdaptor = customAdaptor;
			if (customAdaptor != null) {
				this.type = customAdaptor.getType();
			}
			return this;
		}
		
		
	}
	
	/** @threadsafe **/
	public static final class FieldMapping {
		private final PropertyDescriptor objectField;
		private final boolean multivalued;
		private final String basicName;
		private final Storage storage;
		private final CustomTypeAdaptor customAdaptor;
		private final boolean defaultField;
		
		// Lazilly-initialized when type is deduced
		private final AtomicReference<TypeInfo> typeInfo = new AtomicReference<>();
		
		public FieldMapping(
				final PropertyDescriptor objectField, 
				final String basicName,
				final Type type, 
				final Storage storage,
				final boolean multivalued,
				final CustomTypeAdaptor customAdaptor,
				final boolean defaultField
		) {
			this.objectField = objectField;
			this.multivalued = multivalued;
			this.basicName = basicName;
			this.storage = storage;
			if (type != null) {
				typeInfo.set(new TypeInfo(type, false));
			}
			this.customAdaptor = customAdaptor;
			if (customAdaptor != null) {
				typeInfo.set(new TypeInfo(customAdaptor.getType(), false));
			}
			this.defaultField = defaultField;
		}
		
		public boolean isMultivalued() {
			return multivalued;
		}

		public String getBasicName() {
			return basicName;
		}
		
		public Type getType() {
			final TypeInfo info = typeInfo.get();
			return info == null ? null : info.type;
		}

		public Storage getStorage() {
			return storage;
		}
	
		public boolean isDefaultField() {
			return defaultField;
		}

		public Field getField() {
			return ensureTypeInfo().field;
		}

		public FieldValue fromBean(final Object bean) {
			try{
				final Object beanFieldValue = objectField.getReadMethod().invoke(bean);
				return fromBeanFieldValue(beanFieldValue);
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				throw new AssertionError("Can't read field "+ basicName +
						" value, this is not supposed to happen since I have reflected on this class", ex);
			}
		}

		public FieldValue fromBeanFieldValue(final Object beanFieldValue) {
			if (beanFieldValue == null) {
				return null;
			} else if (multivalued) {
				return fromCollection((Collection<?>)beanFieldValue);
			} else {
				return fromScalar(beanFieldValue);
			}
		}

		public void toBean(final Object bean, final FieldValue value) {
			Object docValue = value.getValue();
			if (customAdaptor != null) {
				if (docValue instanceof Collection<?>) {
					final Collection<?> asColl = (Collection<?>)docValue;
					final List<Object> converted = new ArrayList<>(asColl.size());
					for (final Object obj : asColl) {
						final Object convertedObj = customAdaptor.fromTypeToValue(obj);
						if (convertedObj != null) {
							converted.add(convertedObj);
						}
					}
					docValue = converted;
				} else {
					docValue = customAdaptor.fromTypeToValue(docValue);
				}
			}
			
			final TypeInfo info = typeInfo.get();
			if (info != null) {
				docValue = info.coerce(docValue);
			}
			
			try {
				objectField.getWriteMethod().invoke(bean, docValue);
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				LOG.warn("Can't assign {}={}, field not set", basicName, docValue, ex);
			}
		}
		
		private TypeInfo ensureTypeInfo() {
			TypeInfo info = typeInfo.get();
			if (info == null) {
				info = new TypeInfo(Type.STRING, true);
				if (!typeInfo.compareAndSet(null, info)) {
					info = typeInfo.get();
				}
			}
			return info;
		}
		
		private FieldValue fromCollection(final Collection<?> asColl) {
			final Iterator<?> it = asColl.iterator();
			Object first = null;
			while (first == null && it.hasNext()) {
				first = it.next();
			}
			if (first == null) {
				// no non-null elements, don't create FieldValue
				return null;
			}
					
			final TypeInfo info = resolveTypeInfo(first.getClass());
			final List<Object> recasted = new ArrayList<>(asColl.size());
			recasted.add(info.coerce(adaptFromValueToType(first)));
			while(it.hasNext()) {
				Object next = it.next();
				if (next != null) {
					next = adaptFromValueToType(next);
					if (next != null) {
						recasted.add(info.coerce(next));
					}
				}
			}
			
			return new FieldValue(info.field, recasted);
		}

		private FieldValue fromScalar(final Object beanFieldValue) {
			final TypeInfo info = resolveTypeInfo(beanFieldValue.getClass());
			Object value = adaptFromValueToType(beanFieldValue);
			return new FieldValue(info.field, info.coerce(value));
		}
		
		private Object adaptFromValueToType(final Object value) {
			if (customAdaptor == null) {
				return value;
			} else {
				return customAdaptor.fromValueToType(value);
			}
		}

		private TypeInfo resolveTypeInfo(final Class<?> actualJavaType) {
			TypeInfo info = typeInfo.get();
			if (info == null) {
				// Attempt to detect type
				info = new TypeInfo(actualJavaType);
				if (typeInfo.compareAndSet(null, info)) {
					LOG.warn("Detected field {} {} type as {}", objectField.getPropertyType(), basicName, info.type);
				} else {
					info = typeInfo.get();
				}
			}
			return info;
		}
		
		private final class TypeInfo {
			private final Type type;
			private final boolean coercedToText;
			private final Field field;
			
			public TypeInfo(final Class<?> klass) {
				Type detected = Type.fromJavaType(klass);
				this.coercedToText = detected == null;
				if (coercedToText) {
					detected = Type.STRING;
					LOG.debug("Detected field {} {} type as {}", objectField.getPropertyType(), basicName, detected);
				}
				this.type = detected;
				this.field = new Field(basicName, type, storage, multivalued);
			}
			
			private TypeInfo(final Type type, final boolean coercedToText) {
				this.type = type;
				this.coercedToText = coercedToText;
				this.field = new Field(basicName, type, storage, multivalued);
				if (coercedToText) {
					LOG.debug("Detected field {} {} type as {}", objectField.getPropertyType(), basicName, type);
				}			
			}
			
			public Object coerce(final Object value) {
				if (value == null) {
					return null;
				} else if (coercedToText) {
					return value.toString();
				} else {
					return value;
				}
			}
		}
	}
}
