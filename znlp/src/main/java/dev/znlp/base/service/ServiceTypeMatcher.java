package dev.znlp.base.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 服务类型匹配器
 *
 * <p>提供基于继承关系和接口实现关系的服务类型查找逻辑，
 * 供 {@link ServiceRegistry} 使用。</p>
 *
 * <p>距离计算规则：</p>
 * <ul>
 *   <li>相同类：距离为0</li>
 *   <li>直接继承：距离为1</li>
 *   <li>多级继承：每级加1</li>
 *   <li>接口实现：距离为1（无论直接或间接实现）</li>
 *   <li>无继承关系：距离为{@link Integer#MAX_VALUE}</li>
 * </ul>
 *
 * @author jimichan
 * @since 1.0.0
 */
final class ServiceTypeMatcher {

    private ServiceTypeMatcher() {
    }

    /**
     * 在已注册类型中查找与请求类型最匹配的类型
     *
     * @param registeredClasses 已注册的服务类型集合
     * @param requestedClass    请求的服务类型
     * @return 最匹配的已注册类型，未找到则返回 null
     */
    static Class<?> findBestMatch(Set<Class<?>> registeredClasses, Class<?> requestedClass) {
        Class<?> bestMatch = null;
        int bestDistance = -1;

        for (Class<?> registeredClass : registeredClasses) {
            if (requestedClass.isAssignableFrom(registeredClass)) {
                int distance = calculateClassDistance(requestedClass, registeredClass);
                if (distance > bestDistance) {
                    bestDistance = distance;
                    bestMatch = registeredClass;
                }
            }
        }
        return bestMatch;
    }

    /**
     * 计算两个类之间的继承距离
     *
     * @param superClass 父类或接口类型
     * @param subClass   子类或实现类类型
     * @return 两个类之间的继承距离
     */
    static int calculateClassDistance(Class<?> superClass, Class<?> subClass) {
        if (superClass == subClass) {
            return 0;
        }

        int distance = 0;
        Class<?> currentClass = subClass;

        while (currentClass != null && currentClass != superClass) {
            distance++;
            currentClass = currentClass.getSuperclass();
        }

        if (currentClass == superClass) {
            return distance;
        }

        Set<Class<?>> interfaces = getAllInterfaces(subClass);
        if (interfaces.contains(superClass)) {
            return 1;
        }

        return Integer.MAX_VALUE;
    }

    /**
     * 获取类的所有接口（包括继承的接口）
     *
     * @param clazz 要获取接口的类对象
     * @return 包含所有接口的集合，不会返回 null
     */
    static Set<Class<?>> getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaces = new HashSet<>();
        Class<?> currentClass = clazz;

        while (currentClass != null) {
            Collections.addAll(interfaces, currentClass.getInterfaces());
            currentClass = currentClass.getSuperclass();
        }

        Set<Class<?>> allInterfaces = new HashSet<>();
        Set<Class<?>> toProcess = new HashSet<>(interfaces);

        while (!toProcess.isEmpty()) {
            Class<?> interfaceClass = toProcess.iterator().next();
            toProcess.remove(interfaceClass);

            if (allInterfaces.add(interfaceClass)) {
                for (Class<?> parentInterface : interfaceClass.getInterfaces()) {
                    if (!allInterfaces.contains(parentInterface)) {
                        toProcess.add(parentInterface);
                    }
                }
            }
        }

        return allInterfaces;
    }
}
