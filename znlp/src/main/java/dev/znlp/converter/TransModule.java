package dev.znlp.converter;

import dev.znlp.SystemModule;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.ZnlpSystemConfig;

/**
 * 繁简转换模块
 * <p>
 * 负责注册繁简转换相关服务
 *
 * @author jimichan
 */
public class TransModule implements SystemModule {

    @Override
    public String getName() {
        return "trans";
    }

    @Override
    public void register(ServiceRegistry services, ZnlpSystemConfig config) {
        services.registerFactory(TransformService.class, TransformServiceFactory.class, true);
    }
}
