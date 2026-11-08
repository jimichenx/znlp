package dev.znlp.converter;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceFactory;
import dev.znlp.config.ZnlpSystemConfig;

/**
 * 繁简体转换服务工厂类
 * <p>
 * 负责创建TransformService实例，该工厂类会：
 * <ol>
 *   <li>加载繁简体转换内置词典</li>
 *   <li>使用内置词典创建TransformService实例</li>
 * </ol>
 * </p>
 *
 * @author jimichan
 */
public class TransformServiceFactory implements ServiceFactory<TransformService> {

    private final ZnlpSystemConfig config;
    private final NlpResourceLoader loader;

    /**
     * 构造函数，注入所需的依赖
     *
     * @param config 系统配置对象，用于获取缓存目录等配置
     * @param loader 资源加载器，用于加载词典文件资源
     */
    public TransformServiceFactory(ZnlpSystemConfig config, NlpResourceLoader loader) {
        this.config = config;
        this.loader = loader;
    }

    /**
     * 创建TransformService实例
     *
     * @return TransformService实例
     */
    @Override
    public TransformService create() {
        TransformInnerDict innerDict = TransformInnerDict.create(config, loader);
        return new TransformServiceImpl(innerDict);
    }
}
