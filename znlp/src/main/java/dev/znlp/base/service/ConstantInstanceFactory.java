package dev.znlp.base.service;

/**
 * 常量实例工厂
 *
 * <p>始终返回同一个实例的工厂，用于单例实例的快速注册。</p>
 *
 * @param <T> 服务类型泛型
 * @author jimichan
 * @since 1.0.0
 */
final class ConstantInstanceFactory<T> implements ServiceFactory<T> {

    private final T instance;

    ConstantInstanceFactory(T instance) {
        this.instance = instance;
    }

    @Override
    public T create() {
        return instance;
    }
}
