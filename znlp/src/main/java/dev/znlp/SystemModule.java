package dev.znlp;

import dev.znlp.base.common.ModuleLoader;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.ZnlpSystemConfig;

/**
 * System module interface for implementing modular component registration logic.
 * <p>
 * Each module is responsible for registering its own related service components into {@link ServiceRegistry}.
 * Modules can implement this interface to decentralize the system's default registration logic,
 * making the system more modular and extensible.
 * <p>
 * Implementation classes should:
 * <ul>
 *   <li>Provide a no-argument constructor (instantiated automatically by the system)</li>
 *   <li>Implement registration logic in the {@link #register(ServiceRegistry, ZnlpSystemConfig)} method</li>
 *   <li>Ensure the registration logic is idempotent (can be safely called multiple times)</li>
 * </ul>
 * <p>
 * Modules are automatically loaded and initialized during system construction.
 * The loading order of modules can be controlled by the order in the module configuration file.
 * If a module fails to load, it will not affect the loading of other modules.
 *
 * @author jimichan
 * @see ModuleLoader
 * @see DefaultZnlpSystemBuilder
 */
public interface SystemModule {

    /**
     * Get the module name.
     * <p>
     * The module name is used for logging and configuring disabled modules.
     * It is recommended to use short and meaningful names, such as: "core-dictionary", "resource-loader", etc.
     *
     * @return The module name, should not be null or an empty string
     */
    String getName();

    /**
     * Get the module's priority.
     * <p>
     * Priority is used to control the loading order of modules; smaller values have higher priority (loaded earlier).
     * If multiple modules have the same priority, they are loaded in the order specified in the configuration file.
     * <p>
     * The default priority is 100, which is suitable for most modules. If a module needs to be loaded before others,
     * it can return a smaller value (e.g., 10, 20, etc.). If it needs to be loaded after others,
     * it can return a larger value (e.g., 200, 300, etc.).
     *
     * @return The module priority, default is 100
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Register service components related to this module into the service registry.
     * <p>
     * This method is called during system construction to register service components managed by the module
     * into the {@link ServiceRegistry}. Implementation classes should call various registration methods
     * of {@link ServiceRegistry} (such as {@link ServiceRegistry#registerClass(Class)},
     * {@link ServiceRegistry#registerSingleton(Class, Object)},
     * {@link ServiceRegistry#registerFactory(Class, Class, boolean)}, etc.) to register components.
     * <p>
     * Notes:
     * <ul>
     *   <li>This method may be called multiple times (though current implementations do not),
     *       so it should be idempotent.</li>
     *   <li>If service components need to be accessed during registration, use {@code registry.getService(Class)}.</li>
     *   <li>Only services for which the current module is responsible should be registered to avoid conflicts with other modules.</li>
     * </ul>
     *
     * @param registry The service registry used to register service components
     * @param config   The system configuration object, which can be used to read configuration info to decide how to register components
     */
    void register(ServiceRegistry registry, ZnlpSystemConfig config);
}