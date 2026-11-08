package dev.znlp.base.common;

import dev.znlp.SystemModule;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.ZnlpSystemConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module loader, responsible for loading system modules from classpath and executing module registration logic.
 * <p>
 * The module loader will:
 * <ol>
 *   <li>Search for all {@code META-INF/znlp-modules.idx} configuration files in the classpath</li>
 *   <li>Read the fully qualified module class names from the configuration files (one per line)</li>
 *   <li>Instantiate these module classes</li>
 *   <li>Sort modules by priority</li>
 *   <li>Invoke the {@link SystemModule#register(ServiceRegistry, ZnlpSystemConfig)} method for each module</li>
 *   <li>Filter out modules that are disabled in the configuration</li>
 * </ol>
 * <p>
 * Configuration file format:
 * <pre>
 * # This is a comment
 * dev.znlp.CoreModule
 * dev.znlp.module.ResourceLoaderModule
 * dev.znlp.segment.DictionaryModule
 * </pre>
 * <p>
 * If a module fails to load, it will not affect the loading of other modules. Failed modules will be logged.
 *
 * @author jimichan
 * @see SystemModule
 */
public class ModuleLoader {

    private static final String MODULES_CONFIG_FILE = "META-INF/znlp-modules.idx";

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ModuleLoader.class);

    /**
     * Load all system modules and execute registration logic.
     * <p>
     * This method will:
     * <ol>
     *   <li>Find all module configuration files from classpath</li>
     *   <li>Read and instantiate all module classes</li>
     *   <li>Filter out disabled modules</li>
     *   <li>Sort by priority</li>
     *   <li>Iteratively call the registration method for each module</li>
     * </ol>
     *
     * @param registry Service registry, used to register service components
     * @param config   System configuration object, used to read disabled modules configuration
     */
    public static void loadModules(ServiceRegistry registry, ZnlpSystemConfig config) {
        List<SystemModule> modules = discoverAndInstantiateModules(config);

        for (SystemModule module : modules) {
            try {
                module.register(registry, config);
                logger.debug("Module registered: " + module.getName());
            } catch (Exception e) {
                logger.error("Module registration failed: " + module.getName() + ", Error: " + e.getMessage());
            }
        }
    }

    /**
     * Discover and instantiate all modules.
     * <p>
     * This method will:
     * <ol>
     *   <li>Find all module configuration files from classpath</li>
     *   <li>Read all module class names</li>
     *   <li>Instantiate module classes</li>
     *   <li>Filter out disabled modules</li>
     *   <li>Sort by priority</li>
     * </ol>
     *
     * @param config System configuration object, used to read disabled modules configuration
     * @return List of instantiated and sorted modules
     */
    private static List<SystemModule> discoverAndInstantiateModules(ZnlpSystemConfig config) {
        Set<String> moduleClassNames = new LinkedHashSet<>();

        // Find all module configuration files from classpath
        try {
            Enumeration<URL> resources = getClassLoader().getResources(MODULES_CONFIG_FILE);
            while (resources.hasMoreElements()) {
                URL resourceUrl = resources.nextElement();
                try (InputStream inputStream = resourceUrl.openStream();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Trim whitespace and remove comments
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        moduleClassNames.add(line);
                    }
                } catch (IOException e) {
                    logger.warn("Failed to read module configuration file: {}, Error: {}", resourceUrl, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to find module configuration files: {}", e.getMessage());
        }

        // Instantiate module classes
        List<SystemModule> modules = new ArrayList<>();
        Set<String> disabledModules = getDisabledModules(config);

        for (String className : moduleClassNames) {
            try {
                Class<?> clazz = Class.forName(className, true, getClassLoader());
                if (!SystemModule.class.isAssignableFrom(clazz)) {
                    logger.warn("Class {} does not implement SystemModule interface, skipping", className);
                    continue;
                }

                SystemModule module = (SystemModule) clazz.getDeclaredConstructor().newInstance();

                // Check if the module is disabled
                if (disabledModules.contains(module.getName())) {
                    logger.info("Module disabled, skipping: {}", module.getName());
                    continue;
                }

                modules.add(module);
            } catch (ClassNotFoundException e) {
                logger.warn("Module class not found: {}", className);
            } catch (Exception e) {
                logger.warn("Failed to instantiate module class: {}, Error: {}", className, e.getMessage());
            }
        }

        // Sort by priority (smaller values come first)
        modules.sort(Comparator.comparingInt(SystemModule::getPriority));

        return modules;
    }

    /**
     * Get the set of disabled module names.
     * <p>
     * Read the list of disabled modules from configuration. The configuration item is {@code disabled-modules},
     * and the value is a comma-separated list of module names, e.g., {@code "module1,module2,module3"}.
     *
     * @param config System configuration object
     * @return Set of disabled module names, or an empty set if the configuration does not exist or is empty
     */
    private static Set<String> getDisabledModules(ZnlpSystemConfig config) {
        String disabledModulesConfig = config.getDisabledModules();
        if (disabledModulesConfig == null || disabledModulesConfig.trim().isEmpty()) {
            return Collections.emptySet();
        }

        return Arrays.stream(disabledModulesConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Get the class loader used for loading classes and resources.
     * <p>
     * Prefer using the current thread's context class loader; if it doesn't exist, use the class loader of the current class.
     *
     * @return Class loader
     */
    private static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ModuleLoader.class.getClassLoader();
        }
        if (classLoader == null) {
            throw new IllegalStateException("Could not get class loader");
        }
        return classLoader;
    }
}