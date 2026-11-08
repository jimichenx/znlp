# Service 包 - 依赖注入容器

## 概述

`dev.znlp.base.service` 包实现了一个轻量级的依赖注入（DI）容器，提供了服务注册、查找和自动依赖注入功能。该容器支持基于继承关系和接口实现关系的智能服务查找，并提供了单例和原型两种服务生命周期管理模式。

## 核心组件

### 1. ServiceLocator（服务定位器接口）

定义了从服务注册表中获取服务实例的标准方法。

**主要功能：**
- 根据服务类型查找对应的服务实例
- 支持继承关系和接口实现关系的服务查找
- 提供类型安全的服务获取

**接口方法：**
```java
<T> T getService(Class<T> clazz)
```

### 2. ServiceRegistry（服务注册表实现）

依赖注入容器的核心实现类，实现了 `ServiceLocator` 接口。

**主要功能特点：**
- ✅ 支持直接类型匹配查找
- ✅ 支持基于继承关系的最匹配查找
- ✅ 支持基于接口实现关系的服务查找
- ✅ **支持未注册服务的自动创建（带依赖注入）**
- ✅ 线程安全的并发访问
- ✅ 支持单例实例快速注册
- ✅ 支持通过类注册（自动构造函数注入）
- ✅ 支持单例和原型两种服务模式
- ✅ 提供类型安全的服务获取
- ✅ 循环依赖检测
- ✅ 查找结果缓存优化

**核心方法：**

#### 服务注册方法

```java
// 注册服务工厂
<T> void registerFactory(Class<T> clazz, ServiceFactory<T> factory)

// 注册服务工厂（指定单例模式）
<T> void registerFactory(Class<T> clazz, ServiceFactory<T> factory, boolean singleton)

// 注册单例实例
<T> void registerSingleton(Class<T> clazz, T instance)

// 自动注册服务类（默认单例模式，支持构造函数注入）
<T> void registerClass(Class<T> clazz)

// 注册单例服务类（支持构造函数注入）
<T> void registerSingletonClass(Class<T> clazz)

// 注册原型服务类（支持构造函数注入）
<T> void registerPrototypeClass(Class<T> clazz)

// 注册服务类（指定单例模式）
<T> void registerClass(Class<T> clazz, boolean singleton)
```

#### 服务查找方法

```java
// 根据服务类型获取服务实例
<T> T getService(Class<T> clazz)
```

**智能查找策略：**

1. **精确匹配**：直接查找指定类型的服务
2. **继承匹配**：查找指定类型的子类实现，选择继承距离最小的最具体类型
3. **接口匹配**：查找实现指定接口的服务
4. **自动创建**：如果未找到已注册的服务，自动创建该类的实例并注入依赖（原型模式）

### 3. ServiceFactory（服务工厂接口）

定义了创建服务实例的工厂方法接口。

**主要功能：**
- 支持单例服务的创建
- 支持原型服务的创建
- 支持服务创建时的依赖注入（通过构造函数注入）

**接口方法：**
```java
T create()
```

### 4. DependencyInjector（依赖注入器）

提供自动构造函数注入功能，通过反射分析构造函数参数，自动从 `ServiceLocator` 获取依赖并创建服务实例。

**主要功能：**
- 自动分析类的构造函数
- 从 `ServiceLocator` 获取构造函数参数对应的依赖服务
- 通过构造函数创建服务实例并注入依赖
- 检测并防止循环依赖
- 优先选择参数最多的构造函数（通常是最具体的构造函数）

**核心方法：**
```java
<T> T createInstance(Class<T> clazz)
```

### 5. ServiceNotFoundException（服务未找到异常）

当尝试从服务注册表中获取不存在的服务时抛出此异常。

**抛出场景：**
- 请求的服务类型未注册
- 请求的服务类型与已注册的服务类型无继承或实现关系
- 服务工厂创建服务实例时发生异常
- 检测到循环依赖

## 使用示例

### 基本使用

```java
// 1. 创建服务注册表
ServiceRegistry registry = new ServiceRegistry();

// 2. 注册服务（单例模式）
registry.registerSingleton(MyService.class, new MyServiceImpl());

// 3. 获取服务
MyService service = registry.getService(MyService.class);
```

### 自动构造函数注入

```java
// 定义服务接口
interface UserService {
    void saveUser(User user);
}

interface EmailService {
    void sendEmail(String to, String subject, String body);
}

// 实现服务类（通过构造函数注入依赖）
class UserServiceImpl implements UserService {
    private final EmailService emailService;
    
    public UserServiceImpl(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @Override
    public void saveUser(User user) {
        // 保存用户逻辑
        emailService.sendEmail(user.getEmail(), "Welcome", "Your account is created");
    }
}

class EmailServiceImpl implements EmailService {
    @Override
    public void sendEmail(String to, String subject, String body) {
        // 发送邮件逻辑
    }
}

// 注册服务（支持自动依赖注入）
ServiceRegistry registry = new ServiceRegistry();
registry.registerClass(EmailService.class);  // 注册 EmailService
registry.registerClass(UserService.class);   // 注册 UserService，自动注入 EmailService

// 获取服务（依赖已自动注入）
UserService userService = registry.getService(UserService.class);
```

### 单例 vs 原型模式

```java
ServiceRegistry registry = new ServiceRegistry();

// 单例模式：每次获取都返回同一个实例
registry.registerSingletonClass(MyService.class);
MyService service1 = registry.getService(MyService.class);
MyService service2 = registry.getService(MyService.class);
// service1 == service2 为 true

// 原型模式：每次获取都创建新实例
registry.registerPrototypeClass(MyService.class);
MyService service3 = registry.getService(MyService.class);
MyService service4 = registry.getService(MyService.class);
// service3 == service4 为 false
```

### 自动创建未注册服务

从 1.0.0 版本开始，`ServiceRegistry` 支持自动创建未注册的服务。

```java
ServiceRegistry registry = new ServiceRegistry();

// 注册依赖
registry.registerClass(DependencyA.class);
registry.registerClass(DependencyB.class);

// ServiceWithDependency 未注册，但会自动创建并注入依赖
ServiceWithDependency service = registry.getService(ServiceWithDependency.class);
// 自动创建成功，依赖已注入
```

**自动创建特点：**
- ✅ 无需显式注册，直接获取即可
- ✅ 自动注入构造函数所需的依赖
- ✅ 采用原型模式，每次获取都是新实例
- ✅ 支持多层依赖的自动创建
- ✅ 检测并防止循环依赖
- ⚠️ 无法创建接口或抽象类（会抛出异常）

**自动创建示例：**

```java
ServiceRegistry registry = new ServiceRegistry();

// 场景1：无参构造函数
SimpleService simple = registry.getService(SimpleService.class);
// 自动创建成功

// 场景2：带依赖的构造函数
registry.registerClass(DependencyA.class);
ServiceWithDependency service = registry.getService(ServiceWithDependency.class);
// 自动创建并注入 DependencyA

// 场景3：多层依赖自动创建
registry.registerClass(Level1Service.class);
// Level2Service 和 Level3Service 都未注册，但会自动创建
Level3Service level3 = registry.getService(Level3Service.class);
// Level3 -> Level2 -> Level1 全部自动创建并注入

// 场景4：混合场景
DependencyA registeredA = new DependencyA();
registry.registerSingleton(DependencyA.class, registeredA);
// DependencyB 和 ServiceWithDependency 未注册，会自动创建
ServiceWithDependency mixed = registry.getService(ServiceWithDependency.class);
// DependencyA 使用注册的单例，DependencyB 自动创建
```

### 基于接口的服务查找

```java
// 定义接口
interface Repository<T> {
    void save(T entity);
}

// 实现接口
class UserRepository implements Repository<User> {
    @Override
    public void save(User entity) {
        // 保存用户
    }
}

// 注册实现类
ServiceRegistry registry = new ServiceRegistry();
registry.registerClass(UserRepository.class);

// 通过接口类型查找服务
Repository<User> repository = registry.getService(Repository.class);
// 自动找到 UserRepository 实现
```

### 自定义服务工厂

```java
// 创建自定义工厂
ServiceFactory<MyService> factory = new ServiceFactory<MyService>() {
    @Override
    public MyService create() {
        // 自定义创建逻辑
        return new MyServiceImpl(config);
    }
};

// 注册工厂
ServiceRegistry registry = new ServiceRegistry();
registry.registerFactory(MyService.class, factory, true); // true 表示单例模式

// 获取服务
MyService service = registry.getService(MyService.class);
```

## 设计特点

### 1. 智能服务查找

容器支持基于继承关系和接口实现关系的智能查找：
- 当请求父类或接口类型时，自动查找最匹配的子类或实现类
- 通过计算继承距离，选择最具体的实现类型
- 使用缓存机制优化查找性能

### 2. 线程安全

- 使用 `ConcurrentHashMap` 存储服务工厂，支持并发访问
- 单例服务使用双重检查锁定模式，确保线程安全的懒加载
- 使用 `ThreadLocal` 进行循环依赖检测，避免并发问题

### 3. 循环依赖检测

- 在服务创建过程中检测循环依赖
- 使用 `ThreadLocal` 跟踪正在创建的服务类型
- 发现循环依赖时抛出 `ServiceNotFoundException`

### 6. 自动服务创建

当服务未注册时，容器会自动尝试创建：
- 通过 `DependencyInjector` 分析构造函数并创建实例
- 自动注入构造函数所需的依赖服务
- 每次获取都创建新实例（原型模式）
- 无法创建接口、抽象类等特殊类型

### 7. 灵活的注册方式

支持多种服务注册方式：
- 直接注册实例（单例）
- 注册服务类（自动构造函数注入）
- 注册服务工厂（自定义创建逻辑）
- 支持单例和原型两种生命周期模式

### 5. 类型安全

- 使用 Java 泛型提供编译时类型检查
- 服务查找和注册都基于类型，避免运行时类型错误

## 注意事项

1. **循环依赖**：容器会检测并阻止循环依赖，但应尽量避免设计上的循环依赖
2. **构造函数选择**：依赖注入器优先选择参数最多的构造函数，确保依赖能够正确注入
3. **自动创建限制**：只能自动创建具体类，无法创建接口、抽象类或需要特殊初始化的类
4. **原型模式**：自动创建的服务采用原型模式，每次获取都是新实例。如需单例，请显式注册
5. **线程安全**：容器本身是线程安全的，但服务实现类需要自行保证线程安全
6. **性能考虑**：查找结果会被缓存，但注册新服务会清空缓存，频繁注册可能影响性能

## 适用场景

- 需要依赖注入的应用程序
- 需要服务定位器模式的场景
- 需要管理服务生命周期的应用
- 需要基于接口编程的应用架构

## 版本信息

- **作者**：jimichan
- **版本**：1.0.0
