package dev.znlp.highlight;

import dev.znlp.SystemModule;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.ZnlpSystemConfig;

/**
 * 高亮模块
 * <p>
 * 注册智能高亮服务
 *
 * @author znlp
 */
public class HighlightModule implements SystemModule {

    @Override
    public String getName() {
        return "highlight";
    }

    @Override
    public void register(ServiceRegistry services, ZnlpSystemConfig config) {
        services.registerSingletonClass(SmartHighlighter.class);
    }
}
