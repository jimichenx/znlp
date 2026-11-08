package dev.znlp.base.service;

/**
 * Service Locator Interface
 *
 * <p>This interface defines the standard method for retrieving service instances from a service registry.
 * It serves as part of a dependency injection container, providing type-safe service lookup functionality.</p>
 *
 * <p>Implementations typically work with {@link ServiceRegistry} to provide the following features:</p>
 * <ul>
 *   <li>Locate service instances by service type</li>
 *   <li>Support service lookup based on inheritance and interface implementation relationships</li>
 *   <li>Provide type-safe service retrieval</li>
 *   <li>Auto-create unregistered services with dependency injection (if supported)</li>
 * </ul>
 *
 * @author jimichan
 * @since 1.0.0
 */
public interface ServiceLocator {

    /**
     * Retrieve a service instance by its type
     *
     * <p>This method locates the best matching service implementation from the service registry
     * based on the specified type. If no registered service is found, it will attempt to
     * auto-create an instance with dependency injection (prototype mode).</p>
     *
     * <p>Supported service lookup strategies:</p>
     * <ul>
     *   <li>Exact match: Directly lookup the specified type</li>
     *   <li>Inheritance match: Lookup subclass implementations of the specified type</li>
     *   <li>Interface match: Lookup implementations of the specified interface</li>
     *   <li>Auto-creation: Create new instances with dependency injection if not registered</li>
     * </ul>
     *
     * @param <T>   Service type
     * @param clazz Class object of the service type to retrieve
     * @return The corresponding service instance, never returns null
     * @throws RuntimeException         When the service cannot be found or created
     * @throws ServiceNotFoundException When service is not registered and cannot be auto-created
     */
    <T> T getService(Class<T> clazz);

}
