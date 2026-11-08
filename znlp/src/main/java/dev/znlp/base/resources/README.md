# Resources 包 - NLP 资源管理模块

## 概述

`resources` 包提供了统一的 NLP 资源管理框架，支持从多种来源（文件系统、JAR 包、类路径）加载和管理 NLP 相关的资源文件（如字典、模型等）。该模块采用工厂模式和策略模式，提供了灵活、可扩展的资源加载机制。

## 核心组件

### 1. 接口层

#### `NlpResource`
资源接口，定义了读取 NLP 资源的标准方式。

**主要方法：**
- `InputStream inputStream()` - 获取资源的输入流
- `String hash()` - 计算资源的哈希值（默认使用 MD5）

**特点：**
- 支持从不同来源（文件系统、类路径、JAR 文件等）读取资源
- 资源通常是基于文本的，一般一行一个数据
- 可以与外部系统对接，比如数据库、HDFS 等

#### `NlpResourceFactory`
资源工厂接口，用于从不同来源加载资源文件。

**主要方法：**
- `NlpResource load(String resourceName, Charset charset)` - 加载指定资源
- `NlpResource load(String resourceName)` - 使用 UTF-8 字符集加载资源（默认方法）

**实现类：**
- `FileNlpResourceFactory` - 从文件系统加载
- `JarNlpResourceFactory` - 从 JAR 文件加载
- `ClasspathNlpResourceFactory` - 从类路径加载

### 2. 实现层

#### `FileNlpResourceFactory`
从文件系统加载资源的工厂实现。

**功能：**
- 在指定的基础目录下查找资源文件
- 资源名称中的 "/" 会被替换为系统文件分隔符
- 只有当文件存在且可读时才会返回资源实例

**使用示例：**
```java
File baseDir = new File("/path/to/resources");
NlpResourceFactory factory = new FileNlpResourceFactory(baseDir);
NlpResource resource = factory.load("dict/abc.dict");
```

#### `JarNlpResourceFactory`
从 JAR 文件加载资源的工厂实现。

**功能：**
- 在指定的基础目录下查找 JAR 文件
- 从这些 JAR 文件中加载资源
- JAR 文件按照版本号排序，后加载的版本会覆盖先加载的版本
- 版本号格式：`x.y.z.jar`（例如 `1.0.9.jar`、`1.0.10.jar`）
- 使用索引缓存提升性能

**特点：**
- 支持版本号规范化排序（例如 `1.0.9` 会排在 `1.0.10` 之前）
- 使用延迟初始化构建索引
- 使用 ZIP 条目的 CRC 值作为哈希值，性能更优

#### `ClasspathNlpResourceFactory`
从类路径（Classpath）加载资源的工厂实现。

**功能：**
- 使用指定的 `ClassLoader` 从类路径中查找和加载资源
- 资源名称可以以 "/" 开头，会自动处理

**使用示例：**
```java
ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
NlpResourceFactory factory = new ClasspathNlpResourceFactory(classLoader);
NlpResource resource = factory.load("dict/abc.dict");
```

#### `URLNlpResource`
基于 URL 的 NLP 资源实现。

**功能：**
- 封装一个 URL，提供从该 URL 读取资源内容的功能
- 通常用于从类路径或网络位置加载资源
- 返回的输入流是缓冲的，以提高读取性能

### 3. 核心加载器

#### `NlpResourceLoader`
NLP 资源加载器，负责根据指定配置加载 NLP 相关的资源文件。

**核心功能：**

1. **多源资源查找**
   - 优先级：本地文件系统 → 本地 Jar 包 → Classpath
   - 按照工厂列表的优先级顺序尝试从不同源加载资源

2. **自动下载**
   - 资源未找到且允许自动下载时，从远程仓库自动下载所需资源
   - 支持从 Maven 仓库下载资源 JAR 包

3. **资源缓存**
   - 避免重复加载，提升性能
   - 使用 `ConcurrentHashMap` 实现线程安全的缓存

4. **并发安全**
   - 使用并发容器和原子变量保证线程安全
   - 支持多线程环境下的资源加载

5. **资源验证**
   - 加载前验证资源的完整性和可读性
   - 验证资源路径的合法性（防止目录遍历攻击）

6. **性能监控**
   - 记录加载统计信息，包括总数、失败数和平均耗时
   - 提供 `LoadStatistics` 类查看统计信息

**使用示例：**
```java
ZnlpSystemConfig config = new ZnlpSystemConfigBuilder().build();
NlpResourceLoader loader = new NlpResourceLoader(config);

// 直接加载，不存在时抛异常
NlpResource resource = loader.loadResource("dict/abc.dict");

// 尝试加载，不存在时返回 null
NlpResource optionalResource = loader.tryLoadResource("dict/abc.dict");

// 获取统计信息
LoadStatistics stats = loader.getStatistics();
System.out.println("总加载次数: " + stats.getTotalLoads());
System.out.println("失败次数: " + stats.getFailedLoads());
System.out.println("平均耗时: " + stats.getAverageLoadTime() + "ms");
```

**主要方法：**
- `NlpResource loadResource(String resourcePath)` - 加载资源（UTF-8）
- `NlpResource loadResource(String resourcePath, Charset charset)` - 使用指定字符集加载资源
- `NlpResource tryLoadResource(String resourcePath)` - 尝试加载资源，不存在时返回 null
- `boolean resourceExists(String resourcePath)` - 检查资源是否存在
- `String hashResource(String resourceName)` - 计算资源的哈希值
- `void clearCache()` - 清除所有缓存的资源对象
- `LoadStatistics getStatistics()` - 获取资源加载的统计信息

**异常处理：**
- `ResourceNotFoundException` - 资源不存在且无法下载时抛出
- `ResourceDownloadException` - 自动下载失败时抛出
- `ResourceValidationException` - 资源验证失败时抛出

### 4. 工具类

#### `UseLines`
行处理工具类，提供便捷的方法来处理输入流中的每一行。

**主要方法：**
- `void forEachLine(InputStream, boolean trim, boolean skipBlank, Consumer<String>)` - 遍历每一行
- `void forEachLine(InputStream, Consumer<String>)` - 使用默认设置遍历每一行
- `void useLines(...)` - 与 forEachLine 功能相同（保持 API 兼容性）
- `CharSourceLineReader lineReader(InputStream, Charset)` - 获取行读取器

**使用示例：**
```java
try (InputStream is = resource.inputStream()) {
    UseLines.forEachLine(is, true, true, line -> {
        // 处理每一行
        System.out.println(line);
    });
}

// 或使用行读取器
try (CharSourceLineReader reader = UseLines.lineReader(inputStream)) {
    for (String line : reader) {
        // 处理每一行
    }
}
```

## 架构设计

### 设计模式

1. **工厂模式** - `NlpResourceFactory` 及其实现类
2. **策略模式** - 不同的资源加载策略（文件、JAR、类路径）
3. **单例模式** - `NlpResourceLoader` 管理资源工厂列表

### 资源加载流程

```
用户请求资源
    ↓
NlpResourceLoader.loadResource()
    ↓
检查缓存 → 命中则返回
    ↓
遍历资源工厂列表（按优先级）
    ├─ FileNlpResourceFactory → 文件系统
    ├─ JarNlpResourceFactory → JAR 文件
    └─ ClasspathNlpResourceFactory → 类路径
    ↓
资源未找到且允许自动下载？
    ↓ 是
从远程仓库下载资源
    ↓
重新尝试加载
    ↓
验证资源完整性
    ↓
存入缓存并返回
```

### 资源优先级

1. **本地文件系统** - 从 `config.getDataDirs()` 指定的目录查找
2. **本地 JAR 包** - 从 `dataDir` 目录下的 JAR 文件内查找
3. **Classpath** - 从类路径中查找

## 使用场景

1. **字典文件加载** - 加载分词字典、词性标注字典等
2. **模型文件加载** - 加载训练好的 NLP 模型
3. **配置文件加载** - 加载 NLP 相关的配置文件
4. **多环境部署** - 支持开发、测试、生产环境的不同资源来源

## 扩展性

该模块设计具有良好的扩展性：

1. **自定义资源工厂** - 实现 `NlpResourceFactory` 接口，支持从数据库、HDFS 等来源加载资源
2. **自定义资源实现** - 实现 `NlpResource` 接口，支持加密资源、压缩资源等特殊格式
3. **自定义下载策略** - 在 `NlpResourceLoader` 中扩展下载逻辑，支持不同的下载源

## 注意事项

1. **资源路径格式** - 资源名称格式通常为路径形式，例如：`dict/abc.dict`
2. **字符集** - 默认使用 UTF-8 字符集，可通过参数指定其他字符集
3. **资源关闭** - 使用 `try-with-resources` 语句确保资源正确关闭
4. **线程安全** - `NlpResourceLoader` 是线程安全的，可以在多线程环境中使用
5. **缓存管理** - 资源会被缓存，如需重新加载可调用 `clearCache()` 方法

## 相关类

- `config.dev.znlp.ZnlpSystemConfig` - 系统配置类
- `common.base.dev.znlp.DictResources` - 资源字典
- `common.base.dev.znlp.DownloadUtils` - 下载工具类
- `common.base.dev.znlp.CharSourceLineReader` - 行读取器
