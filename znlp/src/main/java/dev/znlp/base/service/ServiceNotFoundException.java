package dev.znlp.base.service;

/**
 * 服务未找到异常
 *
 * <p>当尝试从服务注册表中获取不存在的服务时抛出此异常。</p>
 *
 * <p>该异常通常在以下场景中抛出：</p>
 * <ul>
 *   <li>请求的服务类型未注册</li>
 *   <li>请求的服务类型与已注册的服务类型无继承或实现关系</li>
 *   <li>服务工厂创建服务实例时发生异常</li>
 * </ul>
 *
 * @author jimichan
 * @since 1.0.0
 */
public class ServiceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造服务未找到异常
     *
     * @param message 异常消息，包含未找到的服务类型等信息
     */
    public ServiceNotFoundException(String message) {
        super(message);
    }

    /**
     * 构造服务未找到异常，包含原始异常
     *
     * @param message 异常消息，包含未找到的服务类型等信息
     * @param cause   原始异常，用于追踪根本原因
     */
    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
