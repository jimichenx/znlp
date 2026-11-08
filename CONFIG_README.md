# znlp 系统配置说明

## 概述

本目录包含 znlp 系统的配置管理模块，提供灵活的配置加载和管理机制。配置系统支持多种配置方式，优先级从高到低为：

1. **JVM 系统属性** (最高优先级)
2. **配置文件** (`znlp.properties`)
3. **代码中设置** (通过 Builder)

## 配置项说明

### 可用的配置项

| 配置项                      | 类型 | 默认值 | 说明 |
|--------------------------|------|--------|------|
| `data-dir`               | String | `data` | 数据目录，用于存储模型数据和资源文件 |
| `cache-dir`              | String | `cache` | 缓存目录，用于存储临时文件和缓存数据 |
| `auto-download-resource` | Boolean | `true` | 是否自动下载资源，当资源不存在时是否从网络自动下载 |

## 配置方式

### 1. 通过配置文件配置

在 classpath 根目录下创建或编辑 `znlp.properties` 文件：

```properties
# 数据目录配置
data-dir=/path/to/data
# 缓存目录配置
cache-dir=/path/to/cache
# 是否自动下载资源
auto-download-resource=true
```

**注意事项：**

- 配置项名称使用 kebab-case（短横线命名）格式
- 空值会被解析为 `null`
- 布尔值使用 `true`/`false`（不区分大小写）

### 2. 通过 JVM 系统属性配置

JVM 系统属性会覆盖配置文件中的值，适合运行时动态指定配置。

#### 启动参数格式

```bash
java -Dznlp.data-dir=/custom/data/path \
     -Dznlp.cache-dir=/custom/cache/path \
     -Dznlp.auto-download-resource=false \
     -jar your-application.jar
```

#### JVM 属性命名规则

JVM 属性名称格式为：`znlp.{配置项名称}`

- Java 字段名 `dataDir` → JVM 属性 `znlp.data-dir`
- Java 字段名 `cacheDir` → JVM 属性 `znlp.cache-dir`
- Java 字段名 `autoDownloadResource` → JVM 属性 `znlp.auto-download-resource`

**转换规则：**

- camelCase → kebab-case
- 前缀统一为 `znlp.`

#### 代码中设置 JVM 属性

```java
// 在应用启动早期设置
System.setProperty("znlp.data-dir","/custom/data/path");
System.

setProperty("znlp.cache-dir","/custom/cache/path");
System.

setProperty("znlp.auto-download-resource","false");

// 注意：必须在 SystemDefaultConfig 类加载之前设置
// 因为配置会在静态初始化块中加载
```

### 3. 通过代码配置

使用 `ZnlpSystemConfigBuilder` 构建自定义配置：

```java
// 创建自定义配置
ZnlpSystemConfig config = ZnlpSystemConfigBuilder.create()
                .dataDir("/custom/data/path")
                .cacheDir("/custom/cache/path")
                .autoDownloadResource(false)
                .build();

// 或者使用默认值填充未设置的项
ZnlpSystemConfig config = ZnlpSystemConfigBuilder.create()
        .dataDir("/custom/data/path")
        .buildWithDefaults(); // 未设置的项会使用系统默认配置
```

## 配置加载流程

### 系统默认配置加载

1. JVM 启动时，`SystemDefaultConfig` 类的静态初始化块自动执行
2. 通过 `ConfigLoader` 从 classpath 加载 `znlp.properties` 配置文件
3. 使用 JVM 系统属性覆盖配置文件中的值
4. 创建全局单例配置实例

```
加载流程：
znlp.properties → 基础配置
       ↓
JVM 系统属性 → 覆盖配置
       ↓
SystemDefaultConfig.instance → 全局单例
```

### 获取系统默认配置

```java
// 获取全局默认配置
ZnlpSystemConfig config = SystemDefaultConfig.getInstance();

// 访问配置项
String dataDir = config.getDataDir();
String cacheDir = config.getCacheDir();
Boolean autoDownload = config.getAutoDownloadResource();
```

### 配置优先级示例

假设同时存在以下配置：

**znlp.properties:**

```properties
data-dir=data
cache-dir=cache
auto-download-resource=true
```

**JVM 参数:**

```bash
-Dznlp.data-dir=/opt/znlp/data
```

**最终生效的配置：**

- `data-dir`: `/opt/znlp/data` (JVM 属性覆盖)
- `cache-dir`: `cache` (配置文件)
- `auto-download-resource`: `true` (配置文件)

## 核心类说明

### ZnlpSystemConfig

系统配置类，存储配置项的值。

```java
public class ZnlpSystemConfig {
    private String dataDir;
    private String cacheDir;
    private Boolean autoDownloadResource;

    // getter 和 package-private setter 方法
}
```

**特点：**

- 提供无参构造函数
- setter 方法为 package-private，只能通过 Builder 设置
- 不可变性保护，防止外部随意修改

### ZnlpSystemConfigBuilder

配置构建器，提供链式 API 创建配置实例。

```java
ZnlpSystemConfig config = ZnlpSystemConfigBuilder.create()
        .dataDir("/path/to/data")
        .cacheDir("/path/to/cache")
        .autoDownloadResource(true)
        .build();
```

**方法说明：**

- `create()`: 创建新的构建器实例
- `dataDir(String)`: 设置数据目录
- `cacheDir(String)`: 设置缓存目录
- `autoDownloadResource(boolean)`: 设置是否自动下载资源
- `build()`: 构建配置对象
- `buildWithDefaults()`: 构建配置对象，未设置的项使用系统默认值

### ConfigLoader

配置文件加载器，负责从 properties 文件读取配置。

**功能：**

- 从 classpath 加载 `znlp.properties` 文件
- 使用反射自动映射配置项到对象属性
- 支持类型转换（String、Boolean、Integer、Long、Double、Float）
- 命名转换：kebab-case ↔ camelCase

```java
// 加载配置到对象
ZnlpSystemConfig config = new ZnlpSystemConfig();
ConfigLoader.

load(config); // 从 znlp.properties 加载

// 或指定配置文件（用于测试）
ConfigLoader.

load(config, "custom.properties");
```

### SystemDefaultConfig

系统默认配置管理类，提供全局单例。

**功能：**

- JVM 启动时自动初始化
- 从配置文件和 JVM 属性加载配置
- 提供全局访问点
- 支持重置（主要用于测试）

```java
// 获取默认配置
ZnlpSystemConfig config = SystemDefaultConfig.getInstance();

// 重置配置（测试用）
SystemDefaultConfig.

reset();
```

### CacheFileUtils

缓存文件工具类，提供缓存文件路径的获取功能。

**缓存目录优先级：**

1. `cacheDir` (配置的缓存目录)
2. `dataDir` (配置的数据目录)
3. `java.io.tmpdir` (系统临时目录)

```java
ZnlpSystemConfig config = SystemDefaultConfig.getInstance();

// 获取缓存文件
File cacheFile = CacheFileUtils.getCacheFile(config, "model.cache");

// 自动创建目录（如果不存在）
// 返回：{cacheDir}/model.cache 或 {dataDir}/model.cache 或 {tmpdir}/model.cache
```

## 使用场景示例

### 场景 1：使用默认配置

```java
// 直接使用系统默认配置
ZnlpSystemConfig config = SystemDefaultConfig.getInstance();
System.out.

println("数据目录: "+config.getDataDir());
```

### 场景 2：测试环境使用自定义配置

```java

@Test
public void testWithCustomConfig() {
    // 创建测试专用配置
    ZnlpSystemConfig testConfig = ZnlpSystemConfigBuilder.create()
            .dataDir("/tmp/test-data")
            .cacheDir("/tmp/test-cache")
            .autoDownloadResource(false)
            .build();

    // 使用测试配置
    File cacheFile = CacheFileUtils.getCacheFile(testConfig, "test.cache");
    // ... 测试逻辑
}
```

### 场景 3：Docker 环境通过环境变量配置

Dockerfile:

```dockerfile
FROM openjdk:11
ENV JAVA_OPTS="-Dznlp.data-dir=/app/data -Dznlp.cache-dir=/app/cache"
CMD java $JAVA_OPTS -jar app.jar
```

docker-compose.yml:

```yaml
services:
  app:
    image: znlp-app
    environment:
      JAVA_OPTS: >
        -Dznlp.data-dir=/app/data
        -Dznlp.cache-dir=/app/cache
        -Dznlp.auto-download-resource=true
```

### 场景 4：生产环境配置

```bash
# 启动脚本
#!/bin/bash

# 设置 JVM 参数
JAVA_OPTS="-Xmx4g -Xms2g"
JAVA_OPTS="$JAVA_OPTS -Dznlp.data-dir=/opt/znlp/data"
JAVA_OPTS="$JAVA_OPTS -Dznlp.cache-dir=/var/cache/znlp"
JAVA_OPTS="$JAVA_OPTS -Dznlp.auto-download-resource=false"

# 启动应用
java $JAVA_OPTS -jar znlp-app.jar
```

## 配置最佳实践

### 1. 开发环境

使用默认配置文件，方便快速启动：

```properties
# znlp.properties (开发环境)
data-dir=./dev-data
cache-dir=./dev-cache
auto-download-resource=true
```

### 2. 测试环境

使用 JVM 属性或代码配置，隔离测试数据：

```java

@BeforeAll
public static void setup() {
    System.setProperty("znlp.data-dir", "./test-data");
    System.setProperty("znlp.cache-dir", "./test-cache");
    System.setProperty("znlp.auto-download-resource", "false");
    SystemDefaultConfig.reset(); // 重新加载配置
}
```

### 3. 生产环境

使用 JVM 系统属性，避免修改配置文件：

```bash
java -Dznlp.data-dir=/opt/app/data \
     -Dznlp.cache-dir=/var/cache/app \
     -Dznlp.auto-download-resource=false \
     -jar app.jar
```

### 4. 云原生环境（Kubernetes）

通过 ConfigMap 和环境变量管理：

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: znlp-config
data:
  JAVA_OPTS: >
    -Dznlp.data-dir=/mnt/data
    -Dznlp.cache-dir=/mnt/cache
    -Dznlp.auto-download-resource=true
---
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: app
      env:
        - name: JAVA_OPTS
          valueFrom:
            configMapKeyRef:
              name: znlp-config
              key: JAVA_OPTS
```

## 常见问题

### Q1: JVM 属性何时设置才能生效？

**A:** JVM 属性必须在 `SystemDefaultConfig` 类加载之前设置。最佳实践：

- 使用启动参数 `-D` 设置（推荐）
- 在 `main` 方法最开始调用 `System.setProperty()`
- 在访问任何 znlp 类之前设置

### Q2: 配置文件找不到会怎样？

**A:** 如果 `znlp.properties` 不在 classpath 中：

- 系统会输出警告信息
- 所有配置项为 `null`
- 可以通过 JVM 属性或代码配置补充

### Q3: 如何验证当前生效的配置？

**A:** 打印配置对象：

```java
ZnlpSystemConfig config = SystemDefaultConfig.getInstance();
System.out.

println(config); // 输出所有配置项
```

### Q4: 缓存目录的选择逻辑是什么？

**A:** `CacheFileUtils` 按优先级选择：

1. 优先使用 `cacheDir`
2. 如果 `cacheDir` 为空，使用 `dataDir`
3. 如果 `dataDir` 也为空，使用系统临时目录 `java.io.tmpdir`

### Q5: 配置项为空字符串和 null 有区别吗？

**A:** 是的：

- 配置文件中的空值（空字符串）会被转换为 `null`
- JVM 属性中的空字符串会保留为空字符串
- 建议使用 `null` 表示未配置

## 扩展配置项

如需添加新的配置项，按以下步骤操作：

### 1. 在 ZnlpSystemConfig 中添加字段

```java
public class ZnlpSystemConfig {
    private String newConfigItem; // 新增配置项

    public String getNewConfigItem() {
        return newConfigItem;
    }

    void setNewConfigItem(String newConfigItem) {
        this.newConfigItem = newConfigItem;
    }
}
```

### 2. 在 Builder 中添加方法

```java
public class ZnlpSystemConfigBuilder {
    public ZnlpSystemConfigBuilder newConfigItem(String value) {
        config.setNewConfigItem(value);
        return this;
    }

    public ZnlpSystemConfig buildWithDefaults() {
        // 添加默认值处理
        if (config.getNewConfigItem() == null) {
            config.setNewConfigItem(defaultConfig.getNewConfigItem());
        }
        // ...
    }
}
```

### 3. 在配置文件中添加配置项

```properties
# znlp.properties
new-config-item=default-value
```

### 4. JVM 属性自动支持

```bash
java -Dznlp.new-config-item=custom-value -jar app.jar
```

命名会自动转换：`newConfigItem` → `znlp.new-config-item`

## 版本历史

- **v1.0.0**: 初始版本，支持基本配置管理
    - 配置文件加载
    - JVM 属性覆盖
    - Builder 模式
    - 缓存文件工具

## 相关文档

- [znlp 系统架构](../../README.md)
- [开发指南](../../docs/DEVELOPMENT.md)
- [部署指南](../../docs/DEPLOYMENT.md)

---

**维护者**: znlp 团队  
**最后更新**: 2026-01-06
