package jabot.jindex;

import java.util.List;

/**
 * Composite {@link Jindex}, that is the jindex built on top of one or more jindexes, called "component" (or, "subcomponent")
 * jindexes. Each subcomponent jindex may serve one or more java classes. Two or more components may serve same class, in 
 * that case special rules for updating apply. There should be a "default" catch-all component that is selected when no
 * component is registered for specific model class.
 * 
 * In other words, composite jindex routes requests basing on the model class. That is very useful from administation and
 * management perspecive: different models can be kept in different repositories with different schemas.
 * 
 * No polymorphism supported, that is if Book extends File and component serves File, it does not serve Book unless explicitly
 * stated.
 * 
 * Requests are routed basing on following rules:
 * <ul>
 *   <li>store requests are routed to the first component that can handle specific object class. Failing that, store calls
 *       are directed to "default" component</li>
 *   <li>{@link #removeByKey(jabot.idxapi.Untokenized)} requests are routed to all components.</li>
 *   <li>{@link #removeByQuery(Class, org.apache.lucene.search.Query)} requests are routed to components that can handle 
 *       particular object class. If no such components, requests are routed to "default" component. </li>
 *   <li>search requests are routed to components that can handle specific object class. If no such components, "default" 
 *       component is used. Results are mixed in round-robin fashion: 1st-from-component1, 1st-from-component2, 
 *       2nd-from-component1,2nd-from-component2,... </li>
 * </ul> 
 *
 */
public interface CompositeJindex extends Jindex {
	
	/**
	 * @return all ids of all subcomponents of this composite
	 */
	public List<String> getAllComponents();
	
	/**
	 * @return the id of the default component
	 */
	public String getDefaultComponent();

	/** get subcomponent by it's unique id */
	public Jindex getComponent(String id);
	
	/**
	 * @return the list of classes particular component handles
	 */
	public List<Class<?>> handles(String id);
	

}
