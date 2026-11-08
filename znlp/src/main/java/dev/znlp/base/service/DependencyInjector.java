package dev.znlp.base.service;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

/**
 * 依赖注入构建器
 *
 * <p>提供自动构造函数注入功能，通过反射分析构造函数参数，
 * 自动从ServiceLocator获取依赖并创建服务实例。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>自动分析类的构造函数</li>
 *   <li>从ServiceLocator获取构造函数参数对应的依赖服务</li>
 *   <li>通过构造函数创建服务实例并注入依赖</li>
 *   <li>检测并防止循环依赖</li>
 * </ul>
 *
 * @author jimichan
 * @since 1.0.0
 */
public class DependencyInjector {

    public final static InternalLogger logger = InternalLoggerFactory.getInstance(DependencyInjector.class);

    private final ServiceLocator locator;
    private final java.util.Set<Class<?>> creating = new java.util.HashSet<>(); // 循环依赖检测

    /**
     * 构造依赖注入器
     *
     * @param locator 服务定位器，用于获取依赖的服务实例
     */
    public DependencyInjector(ServiceLocator locator) {
        this.locator = locator;
    }

    /**
     * 通过构造函数自动注入依赖创建实例
     *
     * <p>该方法会：</p>
     * <ol>
     *   <li>查找最合适的构造函数（优先使用参数最多的构造函数）</li>
     *   <li>为构造函数的每个参数从ServiceLocator获取对应的依赖服务</li>
     *   <li>通过构造函数创建实例并注入依赖</li>
     *   <li>检测循环依赖并抛出异常</li>
     * </ol>
     *
     * @param <T>   服务类型泛型
     * @param clazz 要创建实例的服务类
     * @return 创建的服务实例，不会返回null
     * @throws ServiceNotFoundException 如果无法创建实例或存在循环依赖
     */
    public <T> T createInstance(Class<T> clazz) {
        // 循环依赖检测
        if (creating.contains(clazz)) {
            throw new ServiceNotFoundException(
                    "Circular dependency detected for: " + clazz.getName());
        }

        try {
            creating.add(clazz);

            // 获取所有构造函数，按照参数数量从多到少排序
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();

            // 按照参数数量从多到少排序
            java.util.Arrays.sort(constructors, (c1, c2) -> Integer.compare(c2.getParameterCount(), c1.getParameterCount()));

            // 尝试每个构造函数，直到找到一个可以使用的
            for (Constructor<?> constructor : constructors) {
                try {
                    int paramCount = constructor.getParameterCount();
                    if (paramCount == 0) {
                        // 无参构造函数，直接使用
                        constructor.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        T instance = (T) constructor.newInstance();
                        return instance;
                    }

                    // 获取构造函数参数
                    Parameter[] parameters = constructor.getParameters();
                    Object[] args = new Object[paramCount];

                    // 尝试为每个参数注入依赖
                    for (int i = 0; i < paramCount; i++) {
                        Class<?> paramType = parameters[i].getType();
                        try {
                            args[i] = locator.getService(paramType);
                        } catch (ServiceNotFoundException e) {
                            // 当前参数无法满足，尝试下一个构造函数
                            args = null;
                            break;
                        }
                    }

                    // 如果所有参数都能满足，使用该构造函数
                    if (args != null) {
                        constructor.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        T instance = (T) constructor.newInstance(args);
                        return instance;
                    }
                } catch (ServiceNotFoundException e) {
                    // 继续尝试下一个构造函数
                    continue;
                } catch (Exception e) {
                    logger.error("Error creating instance of {}", clazz.getName(), e);
                    // 构造函数调用失败，继续尝试下一个构造函数
                    continue;
                }
            }

            // 没有找到可以使用的构造函数
            throw new ServiceNotFoundException(
                    "No suitable constructor found for: " + clazz.getName());

        } finally {
            creating.remove(clazz);
        }
    }

    /**
     * 查找最合适的构造函数
     *
     * <p>优先选择参数最多的构造函数（通常是最具体的构造函数）。
     * 如果没有带参数的构造函数，返回null，表示使用无参构造函数。</p>
     *
     * @param clazz 要查找构造函数的类
     * @return 最合适的构造函数，如果没有带参数的构造函数则返回null
     */
    private Constructor<?> findBestConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        if (constructors.length == 0) {
            return null;
        }

        Constructor<?> best = null;
        int maxParams = -1;

        for (Constructor<?> constructor : constructors) {
            int paramCount = constructor.getParameterCount();
            if (paramCount > maxParams) {
                maxParams = paramCount;
                best = constructor;
            }
        }

        // 如果所有构造函数都是无参的，返回null
        return maxParams > 0 ? best : null;
    }
}

