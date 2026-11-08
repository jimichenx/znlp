package dev.znlp;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.ZnlpSystemConfig;

/**
 * 核心模块，负责注册系统核心组件。
 * <p>
 * 该模块注册以下服务：
 * <ul>
 *   <li>{@link NlpResourceLoader} - 资源加载器服务</li>
 * </ul>
 * <p>
 * 该模块具有较高优先级（10），确保在大多数其他模块之前加载。
 *
 * @author jimichan
 * @see SystemModule
 */
public final class CoreModule implements SystemModule {

    @Override
    public String getName() {
        return "core";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public void register(ServiceRegistry registry, ZnlpSystemConfig config) {
        // Register resource loader service
        registry.registerSingletonClass(NlpResourceLoader.class);
    }
}