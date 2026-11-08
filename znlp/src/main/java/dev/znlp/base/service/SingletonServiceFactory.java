package dev.znlp.base.service;

/**
 * 单例服务工厂（线程安全的懒加载）
 *
 * <p>使用双重检查锁定模式确保线程安全的懒加载单例。
 * 首次调用 create 时创建实例，后续调用返回同一个实例。</p>
 *
 * @param <T> 服务类型泛型
 * @author jimichan
 * @since 1.0.0
 */
final class SingletonServiceFactory<T> implements ServiceFactory<T> {

    private final Class<T> clazz;
    private final ServiceLocator locator;
    private volatile T instance;
    private final Object lock = new Object();

    SingletonServiceFactory(Class<T> clazz, ServiceLocator locator) {
        this.clazz = clazz;
        this.locator = locator;
    }

    @Override
    public T create() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    DependencyInjector injector = new DependencyInjector(locator);
                    instance = injector.createInstance(clazz);
                }
            }
        }
        return instance;
    }
}
