package dev.znlp;

import dev.znlp.base.service.ServiceNotFoundException;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.ZnlpSystemConfig;

/**
 * znlp 系统核心管理类，负责配置、资源加载和组件管理。
 * <p>
 * 设计目标：
 * 1. 提供灵活的实例化方式，每个实例有独立的配置
 * 2. 支持依赖注入和组件管理
 * 3. 简化资源加载和配置管理
 * 4. 保持向后兼容性
 */
public record ZnlpSystem(ServiceRegistry registry) {

    public static ZnlpSystem getInstance() {
        return Holder.INSTANCE;
    }

    public static DefaultZnlpSystemBuilder builder() {
        return new DefaultZnlpSystemBuilder();
    }

    public static DefaultZnlpSystemBuilder builder(ZnlpSystemConfig config) {
        return new DefaultZnlpSystemBuilder(config);
    }

    /**
     * 根据服务类型获取已注册的服务实例。
     * <p>
     * 该方法是对内部 {@link ServiceRegistry} 的统一访问入口，支持基于继承关系和接口实现关系的智能查找。
     *
     * @param <T>   服务类型泛型
     * @param clazz 服务类型的 Class 对象
     * @return 匹配的服务实例
     * @throws ServiceNotFoundException 如果找不到匹配的服务或服务工厂
     */
    public <T> T getService(Class<T> clazz) {
        return registry.getService(clazz);
    }

    private static class Holder {
        private static final ZnlpSystem INSTANCE = ZnlpSystem.builder().build();
    }
}
