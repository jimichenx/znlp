package dev.znlp;

import dev.znlp.base.common.ModuleLoader;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.SystemDefaultConfig;
import dev.znlp.config.ZnlpSystemConfig;

/**
 * 系统内部使用的znlp系统默认实例构建器类，负责构建和配置 {@link ZnlpSystem} 的默认实例。
 * <p>
 * 该类仅供系统内部使用，不对外暴露给用户。其主要职责是：
 * <ul>
 *   <li>构建系统的默认实例</li>
 *   <li>初始化服务注册表并注册必要的核心组件</li>
 *   <li>确保系统实例的正确配置和初始化</li>
 * </ul>
 * <p>
 * 对于用户而言，应该通过 {@link ZnlpSystem#getInstance()} 方法获取系统的单例实例，
 * 或者通过 {@link ZnlpSystem#builder()} 方法获取对外暴露的构建器接口。
 *
 * @see ZnlpSystem
 * @see ZnlpSystemConfig
 * @see SystemDefaultConfig
 */
class DefaultZnlpSystemBuilder {

    private final ZnlpSystemConfig config;

    /**
     * 使用系统默认配置创建构建器实例。
     * <p>
     * 该构造方法仅供系统内部使用，用于构建默认的系统实例。
     * 默认配置通过 {@link SystemDefaultConfig#getInstance()} 获取，
     * 该配置会从配置文件和系统属性中加载。
     */
    DefaultZnlpSystemBuilder() {
        this.config = SystemDefaultConfig.getInstance();
    }

    /**
     * 使用指定的配置创建构建器实例。
     * <p>
     * 该构造方法仅供系统内部使用，用于根据特定配置构建系统实例。
     *
     * @param config 系统配置对象，如果为 null，将使用默认配置
     */
    DefaultZnlpSystemBuilder(ZnlpSystemConfig config) {
        this.config = config;
    }

    /**
     * 构建并返回 {@link ZnlpSystem} 实例。
     * <p>
     * 该方法仅供系统内部使用，负责构建和配置 {@link ZnlpSystem} 实例。
     * 它会执行以下操作：
     * <ol>
     *   <li>创建服务注册表（{@link ServiceRegistry}）</li>
     *   <li>初始化并注册所有必要的服务组件</li>
     *   <li>创建并返回配置完成的 {@link ZnlpSystem} 实例</li>
     * </ol>
     *
     * @return 配置完成的 {@link ZnlpSystem} 实例
     */
    ZnlpSystem build() {
        return new ZnlpSystem(initRegistry());
    }

    /**
     * 初始化服务注册表，注册所有必要的服务组件。
     * <p>
     * 该方法会：
     * <ol>
     *   <li>首先注册 {@link ZnlpSystemConfig} 作为单例（供模块使用）</li>
     *   <li>然后使用 {@link ModuleLoader} 加载所有系统模块，由模块负责注册各自的组件</li>
     * </ol>
     * <p>
     * 模块化的加载方式使得系统默认的注册逻辑可以分散在各个模块中，
     * 提高了代码的可维护性和可扩展性。
     *
     * @param registry 服务注册表实例
     */
    private ServiceRegistry initRegistry() {
        ServiceRegistry registry = new ServiceRegistry();

        // 首先注册配置对象，供模块使用
        registry.registerSingleton(ZnlpSystemConfig.class, config);

        // 使用模块加载器加载所有模块，由模块负责注册各自的组件
        ModuleLoader.loadModules(registry, config);

        return registry;
    }
}
