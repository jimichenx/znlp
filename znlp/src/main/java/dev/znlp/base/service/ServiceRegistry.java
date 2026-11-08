package dev.znlp.base.service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务注册表实现类
 *
 * <p>提供服务的注册和智能查找功能，支持基于继承关系和接口实现关系的服务查找。
 * 实现了{@link ServiceLocator}接口，作为依赖注入容器的核心组件。</p>
 *
 * <p>主要功能特点：</p>
 * <ul>
 *   <li>支持直接类型匹配查找</li>
 *   <li>支持基于继承关系的最匹配查找</li>
 *   <li>支持基于接口实现关系的服务查找</li>
 *   <li>线程安全的并发访问</li>
 *   <li>支持单例实例快速注册</li>
 *   <li>支持通过类注册（自动构造函数注入）</li>
 *   <li>支持单例和原型两种服务模式</li>
 *   <li>提供类型安全的服务获取</li>
 * </ul>
 *
 * @author jimichan
 * @since 1.0.0
 */
public class ServiceRegistry implements ServiceLocator {

    private final ConcurrentHashMap<Class<?>, ServiceFactory<?>> factories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Class<?>> lookupCache = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<Class<?>>> creating = ThreadLocal.withInitial(HashSet::new);
    private final ServiceLocator parent;

    /**
     * 默认构造函数（无父容器）
     */
    public ServiceRegistry() {
        this(null);
    }

    /**
     * 构造函数（指定父容器）
     *
     * @param parent 父容器，当本地找不到服务时委托给父容器
     */
    public ServiceRegistry(ServiceLocator parent) {
        this.parent = parent;
    }

    /**
     * 用于标记查找失败的占位类型，避免在缓存中存储 null。
     */
    private static final class NoServiceMarker {
    }

    private static final Class<?> NO_SERVICE = NoServiceMarker.class;

    /**
     * 注册服务工厂
     *
     * <p>将服务类型与对应的服务工厂关联注册，后续可通过该服务类型获取服务实例。</p>
     *
     * @param <T>     服务类型泛型
     * @param clazz   服务类型的Class对象，作为注册和查找的键
     * @param factory 服务工厂实例，用于创建服务对象
     */
    public <T> void registerFactory(Class<T> clazz, ServiceFactory<T> factory) {
        factories.put(clazz, factory);
        lookupCache.clear();
    }

    /**
     * 注册服务工厂（可指定单例模式）
     *
     * <p>将服务类型与对应的服务工厂关联注册，支持指定是否使用单例模式。
     * 如果设置为单例模式，工厂只会创建一个实例，后续调用都会返回同一个实例。</p>
     *
     * @param <T>       服务类型泛型
     * @param clazz     服务类型的Class对象，作为注册和查找的键
     * @param factory   服务工厂实例，用于创建服务对象
     * @param singleton true表示使用单例模式，false表示使用原型模式（每次返回新实例）
     */
    public <T> void registerFactory(Class<T> clazz, ServiceFactory<T> factory, boolean singleton) {
        if (singleton) {
            // 如果需要单例模式，包装原始工厂
            factories.put(clazz, new SingletonWrapperFactory<>(factory));
        } else {
            // 如果不需要单例模式，直接使用原始工厂
            factories.put(clazz, factory);
        }
        lookupCache.clear();
    }

    /**
     * 注册服务工厂类（可指定单例模式）
     *
     * <p>将服务类型与对应的服务工厂类关联注册，支持指定是否使用单例模式。
     * 该方法会自动实例化工厂类，并支持构造函数注入。</p>
     *
     * @param <T>          服务类型泛型
     * @param serviceClazz 服务类型的Class对象，作为注册和查找的键
     * @param factoryClazz 服务工厂类的Class对象，用于创建工厂实例
     * @param singleton    true表示使用单例模式，false表示使用原型模式（每次返回新实例）
     */
    public <T> void registerFactory(Class<T> serviceClazz, Class<? extends ServiceFactory<T>> factoryClazz, boolean singleton) {
        // 创建工厂实例（支持构造函数注入）
        ServiceFactory<T> factory = createFactoryInstance(factoryClazz);

        if (singleton) {
            // 如果需要单例模式，包装原始工厂
            factories.put(serviceClazz, new SingletonWrapperFactory<>(factory));
        } else {
            // 如果不需要单例模式，直接使用原始工厂
            factories.put(serviceClazz, factory);
        }
        lookupCache.clear();
    }

    /**
     * 创建工厂实例（支持构造函数注入）
     *
     * <p>使用依赖注入器创建工厂实例，支持构造函数注入。</p>
     *
     * @param <T>          服务类型泛型
     * @param factoryClazz 工厂类的Class对象
     * @return 工厂实例
     */
    private <T> ServiceFactory<T> createFactoryInstance(Class<? extends ServiceFactory<T>> factoryClazz) {
        DependencyInjector injector = new DependencyInjector(this);
        return injector.createInstance(factoryClazz);
    }

    /**
     * 便捷方法：注册单例实例
     *
     * <p>快速注册一个单例服务实例，内部会创建一个返回该实例的服务工厂。</p>
     *
     * @param <T>      服务类型泛型
     * @param clazz    服务类型的Class对象，作为注册和查找的键
     * @param instance 单例服务实例，后续获取时将直接返回该实例
     */
    public <T> void registerSingleton(Class<T> clazz, T instance) {
        registerFactory(clazz, new ConstantInstanceFactory<>(instance));
    }

    /**
     * 自动注册服务类（默认单例模式，支持构造函数注入）
     *
     * <p>自动分析类的构造函数，创建支持依赖注入的服务工厂。
     * 注册后，每次getService都会返回同一个实例（单例模式）。</p>
     *
     * <p>该方法会自动：</p>
     * <ul>
     *   <li>分析类的构造函数参数</li>
     *   <li>从ServiceRegistry获取依赖的服务实例</li>
     *   <li>通过构造函数创建服务实例并注入依赖</li>
     *   <li>使用懒加载单例模式，确保线程安全</li>
     * </ul>
     *
     * @param <T>   服务类型泛型
     * @param clazz 要注册的服务类
     */
    @Deprecated()
    public <T> void registerClass(Class<T> clazz) {
        registerSingletonClass(clazz);
    }

    /**
     * 注册单例服务类（支持构造函数注入）
     *
     * <p>注册后，每次getService都会返回同一个实例。
     * 使用懒加载模式，首次获取时才创建实例。</p>
     *
     * @param <T>   服务类型泛型
     * @param clazz 要注册的服务类
     */
    public <T> void registerSingletonClass(Class<T> clazz) {
        registerFactory(clazz, new SingletonServiceFactory<>(clazz, this));
    }

    private static <T> ServiceFactory<T> prototypeFactoryFor(Class<T> clazz, ServiceLocator locator) {
        return () -> {
            DependencyInjector injector = new DependencyInjector(locator);
            return injector.createInstance(clazz);
        };
    }

    /**
     * 注册原型服务类（支持构造函数注入）
     *
     * <p>注册后，每次getService都会创建新的实例。
     * 适用于需要多个独立实例的场景。</p>
     *
     * @param <T>   服务类型泛型
     * @param clazz 要注册的服务类
     */
    public <T> void registerPrototypeClass(Class<T> clazz) {
        registerFactory(clazz, prototypeFactoryFor(clazz, this));
    }

    /**
     * 注册服务类（指定单例模式）
     *
     * <p>根据singleton参数决定是单例模式还是原型模式。</p>
     *
     * @param <T>       服务类型泛型
     * @param clazz     要注册的服务类
     * @param singleton true表示单例模式，false表示原型模式
     */
    public <T> void registerClass(Class<T> clazz, boolean singleton) {
        if (singleton) {
            registerSingletonClass(clazz);
        } else {
            registerPrototypeClass(clazz);
        }
    }

    /**
     * 根据服务类型获取服务实例
     *
     * <p>智能查找与指定类型最匹配的已注册服务实例。首先尝试直接匹配，如果失败则
     * 查找所有可分配的已注册类型，计算继承距离并返回距离最小的最匹配服务实例。</p>
     *
     * <p>如果未找到已注册的服务，会尝试自动创建该类的实例并注入依赖（原型模式）。</p>
     *
     * @param <T>   服务类型泛型
     * @param clazz 服务类型的Class对象，用于查找匹配的服务
     * @return 匹配的服务实例，不会返回null
     * @throws ServiceNotFoundException 如果找不到匹配的服务或无法创建实例
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> clazz) {
        // 使用缓存加速查找，包含正向和负向结果
        Class<?> cachedClass = lookupCache.computeIfAbsent(clazz, key -> {
            Class<?> match = ServiceTypeMatcher.findBestMatch(factories.keySet(), key);
            return match != null ? match : NO_SERVICE;
        });

        // 1. 如果本地找到匹配的注册信息，使用本地的
        if (cachedClass != NO_SERVICE) {
            return getLocalService(cachedClass);
        }

        // 2. 如果本地没找到，且有父容器，委托给父容器
        if (parent != null) {
            try {
                return parent.getService(clazz);
            } catch (ServiceNotFoundException e) {
                // 父容器也没找到，忽略异常，继续尝试本地自动创建
            }
        }

        // 3. 尝试自动创建实例（本地）
        return tryAutoCreateInstance(clazz);
    }

    /**
     * 从本地已注册的工厂获取服务实例
     */
    @SuppressWarnings("unchecked")
    private <T> T getLocalService(Class<?> bestMatchClass) {
        // 检测循环依赖
        Set<Class<?>> creatingSet = creating.get();
        if (creatingSet.contains(bestMatchClass)) {
            throw new ServiceNotFoundException("Circular dependency detected for: " + bestMatchClass.getName());
        }

        ServiceFactory<?> factory = factories.get(bestMatchClass);
        if (factory != null) {
            try {
                creatingSet.add(bestMatchClass);
                return (T) factory.create();
            } finally {
                creatingSet.remove(bestMatchClass);
            }
        } else {
            throw new ServiceNotFoundException("Service factory not found for: " + bestMatchClass.getName());
        }
    }

    /**
     * 尝试自动创建未注册服务的实例
     *
     * <p>当服务未注册时，尝试通过依赖注入器自动创建该类的实例。
     * 每次调用都会创建新的实例（原型模式）。</p>
     *
     * @param <T>   服务类型泛型
     * @param clazz 要创建的服务类型
     * @return 创建的服务实例
     * @throws ServiceNotFoundException 如果无法创建实例
     */
    private <T> T tryAutoCreateInstance(Class<T> clazz) {
        try {
            // 检测循环依赖
            Set<Class<?>> creatingSet = creating.get();
            if (creatingSet.contains(clazz)) {
                throw new ServiceNotFoundException("Circular dependency detected for: " + clazz.getName());
            }

            try {
                creatingSet.add(clazz);
                DependencyInjector injector = new DependencyInjector(this);
                return injector.createInstance(clazz);
            } finally {
                creatingSet.remove(clazz);
            }
        } catch (ServiceNotFoundException e) {
            // 重新抛出原始异常
            throw e;
        } catch (Exception e) {
            throw new ServiceNotFoundException(
                    "Failed to auto-create instance for: " + clazz.getName() + 
                    ". Reason: " + e.getMessage(), e);
        }
    }

}
