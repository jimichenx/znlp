# znlp Internal Logging

`dev.znlp.base.logging` 是 znlp 内部使用的日志抽象层。它的设计目标是使 znlp 核心库保持“零依赖”特性，同时能够自动适配用户环境中的主流日志框架。

## 主要特性

*   **零强制依赖**：znlp 可以在没有任何第三方日志库的情况下运行（自动回退到 JDK Logging）。
*   **自动适配**：启动时会按照优先级自动检测并绑定可用的日志框架。
*   **优先级顺序**：
    1.  **SLF4J** (推荐，如 Logback, Log4j2-SLF4J-Impl)
    2.  **Log4j2** (直接使用 Log4j2 API)
    3.  **JDK Logging** (java.util.logging，默认兜底方案)

## 使用示例

在 znlp 内部开发时，请使用 `InternalLoggerFactory` 获取日志记录器：

```java
import logging.base.dev.znlp.InternalLogger;
import logging.base.dev.znlp.InternalLoggerFactory;

public class MyComponent {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MyComponent.class);

    public void doSomething() {
        if (logger.isDebugEnabled()) {
            logger.debug("Processing data: {}", "sample");
        }

        try {
            // 业务逻辑
            logger.info("Operation completed successfully.");
        } catch (Exception e) {
            logger.error("Operation failed", e);
        }
    }
}
```

## 手动配置

虽然通常不需要手动干预，但你可以在 znlp 初始化之前强制指定日志工厂。例如，强制使用 JDK Logging：

```java
// 在其他 znlp 类加载之前调用
InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
```

## 常见问题

**Q: 为什么不直接使用 SLF4J？**
A: 虽然 SLF4J 是标准，但为了让 znlp 在极简环境下（无任何第三方 jar 包）也能开箱即用并输出日志，我们维护了这个轻量级的抽象层。

**Q: 如何开启 DEBUG 日志？**
A: 这取决于底层的日志实现。
*   如果是 SLF4J/Log4j2：请在对应的 `logback.xml` 或 `log4j2.xml` 中配置。
*   如果是 JDK Logging：请配置 `logging.properties`。
