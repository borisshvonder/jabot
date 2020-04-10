package jabot.jindex;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jabot.jindex.ModelIntrospector.Mapper;
import javolution.util.FastMap;

/**
 * Global static mappers inventory.
 * Rationale: each class should provide hints on how to map itself in a static init section
 * 
 * @threadsafe
 *
 */
public class ModelMappersInventory {
	private static final Logger LOG = LoggerFactory.getLogger(ModelMappersInventory.class);
	private static final FastMap<Class<?>, Mapper<?>> MAPPERS = new FastMap<Class<?>, Mapper<?>>().shared();
	
	public static void registerMapper(final Mapper<?> mapper) {
		Validate.notNull(mapper, "mapper cannot be null");
		
		MAPPERS.put(mapper.getModel(), mapper);
	}
	
	@SuppressWarnings("unchecked")
	public static<T> Mapper<T> getMapper(final Class<T> forClass) {
		MAPPERS.computeIfAbsent(forClass, k -> {
			LOG.info("Creating default mapper for class {}", forClass);
			final ModelIntrospector<T> introspector = new ModelIntrospector<>(forClass);
			return introspector.buildMapper();
		});
		final Mapper<?> mapper = MAPPERS.get(forClass);
		assert mapper != null: "Got null mapper where it is never supposed to be!";
		final Class<?> actualModel = mapper.getModel();
		assert actualModel == forClass : "Existing mapper model is not the same as we expected!";
		return (Mapper<T>)mapper;
	}
}
