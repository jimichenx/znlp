package dev.znlp.base.service;

/**
 * 单例包装工厂
 *
 * <p>对原始 ServiceFactory 进行装饰，确保只返回一个实例（单例模式）。
 * 使用双重检查锁定模式确保线程安全的懒加载单例。</p>
 *
 * @param <T> 服务类型泛型
 * @author jimichan
 * @since 1.0.0
 */
final class SingletonWrapperFactory<T> implements ServiceFactory<T> {

    private final ServiceFactory<T> delegate;
    private volatile T instance;
    private final Object lock = new Object();

    SingletonWrapperFactory(ServiceFactory<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T create() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = delegate.create();
                }
            }
        }
        return instance;
    }
}
