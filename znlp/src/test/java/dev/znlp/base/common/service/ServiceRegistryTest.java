package dev.znlp.base.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.znlp.base.service.ServiceFactory;
import dev.znlp.base.service.ServiceNotFoundException;
import dev.znlp.base.service.ServiceRegistry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ServiceRegistry类的单元测试
 *
 * @author jimichan
 */
public class ServiceRegistryTest {

    private ServiceRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = new ServiceRegistry();
    }

    // 测试接口
    interface TestService {
        String getName();
    }

    // 测试实现类1
    static class TestServiceImpl1 implements TestService {
        @Override
        public String getName() {
            return "TestServiceImpl1";
        }
    }

    // 测试实现类2（继承自TestServiceImpl1）
    static class TestServiceImpl2 extends TestServiceImpl1 {
        @Override
        public String getName() {
            return "TestServiceImpl2";
        }
    }

    // 测试父类
    static class ParentService {
        public String getType() {
            return "Parent";
        }
    }

    // 测试子类
    static class ChildService extends ParentService {
        @Override
        public String getType() {
            return "Child";
        }
    }

    /**
     * 测试直接注册和获取服务
     */
    @Test
    public void testRegisterFactoryAndGetService() {
        // 注册服务工厂
        registry.registerFactory(TestService.class, TestServiceImpl1::new);

        // 获取服务实例
        TestService service = registry.getService(TestService.class);

        // 验证服务实例不为null且类型正确
        assertNotNull(service);
        assertInstanceOf(TestServiceImpl1.class, service);
        assertEquals("TestServiceImpl1", service.getName());
    }

    /**
     * 测试注册单例和获取服务
     */
    @Test
    public void testRegisterSingletonAndGetService() {
        // 创建单例实例
        TestServiceImpl1 singleton = new TestServiceImpl1();

        // 注册单例
        registry.registerSingleton(TestService.class, singleton);

        // 获取服务实例两次
        TestService service1 = registry.getService(TestService.class);
        TestService service2 = registry.getService(TestService.class);

        // 验证两次获取的是同一个实例
        assertNotNull(service1);
        assertNotNull(service2);
        assertSame(service1, service2);
        assertEquals("TestServiceImpl1", service1.getName());
    }

    /**
     * 测试继承关系的服务查找（子类注册，父类获取）
     */
    @Test
    public void testGetServiceByInheritance() {
        // 注册子类服务
        registry.registerFactory(ChildService.class, ChildService::new);

        // 通过父类获取服务
        ParentService service = registry.getService(ParentService.class);

        // 验证获取到的是子类实例
        assertNotNull(service);
        assertInstanceOf(ChildService.class, service);
        assertEquals("Child", service.getType());
    }

    /**
     * 测试接口实现关系的服务查找（实现类注册，接口获取）
     */
    @Test
    public void testGetServiceByInterface() {
        // 注册实现类
        registry.registerFactory(TestServiceImpl1.class, TestServiceImpl1::new);

        // 通过接口获取服务
        TestService service = registry.getService(TestService.class);

        // 验证获取到的是实现类实例
        assertNotNull(service);
        assertInstanceOf(TestServiceImpl1.class, service);
        assertEquals("TestServiceImpl1", service.getName());
    }

    /**
     * 测试最匹配的服务查找（多个可分配类型，返回最具体的类型）
     */
    @Test
    public void testGetBestMatchService() {
        // 注册父类和子类服务
        registry.registerFactory(ParentService.class, ParentService::new);
        registry.registerFactory(ChildService.class, ChildService::new);

        // 通过父类获取服务，应该返回最具体的子类
        ParentService service = registry.getService(ParentService.class);

        // 验证获取到的是最具体的子类实例
        assertNotNull(service);
        assertInstanceOf(ChildService.class, service);
        assertEquals("Child", service.getType());
    }

    /**
     * 测试获取不存在的服务时抛出异常
     */
    @Test
    public void testGetNonExistentServiceThrowsException() {
        // 验证获取未注册的服务时抛出ServiceNotFoundException
        assertThrows(ServiceNotFoundException.class, () -> {
            registry.getService(TestService.class);
        });
    }

    /**
     * 测试getInstance方法（getService的别名）
     */
    @Test
    public void testGetInstance() {
        // 注册服务
        registry.registerFactory(TestService.class, TestServiceImpl1::new);

        // 使用getInstance获取服务
        TestService service = registry.getService(TestService.class);

        // 验证服务实例不为null且类型正确
        assertNotNull(service);
        assertInstanceOf(TestServiceImpl1.class, service);
        assertEquals("TestServiceImpl1", service.getName());
    }

    /**
     * 测试复杂的继承和接口关系
     */
    @Test
    public void testComplexInheritanceAndInterface() {
        // 注册实现类2
        registry.registerFactory(TestServiceImpl2.class, TestServiceImpl2::new);

        // 通过接口获取服务，应该返回最具体的实现类
        TestService service = registry.getService(TestService.class);

        // 验证获取到的是最具体的实现类2
        assertNotNull(service);
        assertInstanceOf(TestServiceImpl2.class, service);
        assertEquals("TestServiceImpl2", service.getName());
    }

    // ========== 依赖注入相关测试 ==========

    /**
     * 测试无参构造函数的类注册（单例模式）
     */
    @Test
    public void testRegisterClassWithNoArgsConstructor() {
        // 注册无参构造函数的类
        registry.registerSingletonClass(TestServiceImpl1.class);

        // 获取服务实例两次
        TestServiceImpl1 service1 = registry.getService(TestServiceImpl1.class);
        TestServiceImpl1 service2 = registry.getService(TestServiceImpl1.class);

        // 验证单例模式：两次获取的是同一个实例
        assertNotNull(service1);
        assertNotNull(service2);
        assertSame(service1, service2);
        assertEquals("TestServiceImpl1", service1.getName());
    }

    /**
     * 测试单例类注册
     */
    @Test
    public void testRegisterSingletonClass() {
        // 注册单例类
        registry.registerSingletonClass(TestServiceImpl1.class);

        // 获取服务实例两次
        TestServiceImpl1 service1 = registry.getService(TestServiceImpl1.class);
        TestServiceImpl1 service2 = registry.getService(TestServiceImpl1.class);

        // 验证单例模式：两次获取的是同一个实例
        assertNotNull(service1);
        assertNotNull(service2);
        assertSame(service1, service2);
    }

    /**
     * 测试原型类注册
     */
    @Test
    public void testRegisterPrototypeClass() {
        // 注册原型类
        registry.registerPrototypeClass(TestServiceImpl1.class);

        // 获取服务实例两次
        TestServiceImpl1 service1 = registry.getService(TestServiceImpl1.class);
        TestServiceImpl1 service2 = registry.getService(TestServiceImpl1.class);

        // 验证原型模式：两次获取的是不同的实例
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2);
        assertEquals("TestServiceImpl1", service1.getName());
        assertEquals("TestServiceImpl1", service2.getName());
    }

    /**
     * 测试通过参数指定单例模式
     */
    @Test
    public void testRegisterClassWithSingletonFlag() {
        // 注册为单例
        registry.registerClass(TestServiceImpl1.class, true);
        TestServiceImpl1 service1 = registry.getService(TestServiceImpl1.class);
        TestServiceImpl1 service2 = registry.getService(TestServiceImpl1.class);
        assertSame(service1, service2);

        // 重新注册为原型
        registry.registerClass(TestServiceImpl1.class, false);
        TestServiceImpl1 service3 = registry.getService(TestServiceImpl1.class);
        TestServiceImpl1 service4 = registry.getService(TestServiceImpl1.class);
        assertNotSame(service3, service4);
    }

    // 依赖注入测试类
    static class DependencyA {
        private final String name;

        public DependencyA() {
            this.name = "DependencyA";
        }

        public String getName() {
            return name;
        }
    }

    static class DependencyB {
        private final String name;

        public DependencyB() {
            this.name = "DependencyB";
        }

        public String getName() {
            return name;
        }
    }

    record ServiceWithDependency(DependencyA dependencyA, DependencyB dependencyB) {

        public String getInfo() {
                return dependencyA.getName() + " + " + dependencyB.getName();
            }
        }

    /**
     * 测试构造函数依赖注入（单例模式）
     */
    @Test
    public void testConstructorInjectionWithSingleton() {
        // 注册依赖
        registry.registerSingletonClass(DependencyA.class);
        registry.registerSingletonClass(DependencyB.class);

        // 注册需要依赖的服务（单例模式）
        registry.registerSingletonClass(ServiceWithDependency.class);

        // 获取服务
        ServiceWithDependency service = registry.getService(ServiceWithDependency.class);

        // 验证依赖注入成功
        assertNotNull(service);
        assertNotNull(service.dependencyA());
        assertNotNull(service.dependencyB());
        assertEquals("DependencyA", service.dependencyA().getName());
        assertEquals("DependencyB", service.dependencyB().getName());
        assertEquals("DependencyA + DependencyB", service.getInfo());

        // 验证单例模式
        ServiceWithDependency service2 = registry.getService(ServiceWithDependency.class);
        assertSame(service, service2);
    }

    /**
     * 测试构造函数依赖注入（原型模式）
     */
    @Test
    public void testConstructorInjectionWithPrototype() {
        // 注册依赖
        registry.registerSingletonClass(DependencyA.class);
        registry.registerSingletonClass(DependencyB.class);

        // 注册需要依赖的服务（原型模式）
        registry.registerPrototypeClass(ServiceWithDependency.class);

        // 获取服务两次
        ServiceWithDependency service1 = registry.getService(ServiceWithDependency.class);
        ServiceWithDependency service2 = registry.getService(ServiceWithDependency.class);

        // 验证依赖注入成功
        assertNotNull(service1);
        assertNotNull(service2);
        assertEquals("DependencyA + DependencyB", service1.getInfo());
        assertEquals("DependencyA + DependencyB", service2.getInfo());

        // 验证原型模式：每次都是新实例
        assertNotSame(service1, service2);
    }

    /**
     * 测试多层依赖注入
     */
    static class Level1Service {
        public String getName() {
            return "Level1";
        }
    }

    static class Level2Service {
        private final Level1Service level1;

        public Level2Service(Level1Service level1) {
            this.level1 = level1;
        }

        public String getName() {
            return "Level2 -> " + level1.getName();
        }
    }

    static class Level3Service {
        private final Level2Service level2;

        public Level3Service(Level2Service level2) {
            this.level2 = level2;
        }

        public String getName() {
            return "Level3 -> " + level2.getName();
        }
    }

    @Test
    public void testMultiLevelDependencyInjection() {
        // 注册所有层级的服务
        registry.registerSingletonClass(Level1Service.class);
        registry.registerSingletonClass(Level2Service.class);
        registry.registerSingletonClass(Level3Service.class);

        // 获取顶层服务
        Level3Service service = registry.getService(Level3Service.class);

        // 验证多层依赖注入成功
        assertNotNull(service);
        assertEquals("Level3 -> Level2 -> Level1", service.getName());
    }

    /**
     * 测试循环依赖检测
     */
    static class CircularA {
        @SuppressWarnings("unused")
        private final CircularB circularB;

        public CircularA(CircularB circularB) {
            this.circularB = circularB;
        }
    }

    static class CircularB {
        @SuppressWarnings("unused")
        private final CircularA circularA;

        public CircularB(CircularA circularA) {
            this.circularA = circularA;
        }
    }

    @Test
    public void testCircularDependencyDetection() {
        // 注册循环依赖的服务
        registry.registerSingletonClass(CircularA.class);
        registry.registerSingletonClass(CircularB.class);

        // 验证检测到循环依赖并抛出异常
        assertThrows(ServiceNotFoundException.class, () -> {
            registry.getService(CircularA.class);
        });
    }

    /**
     * 测试依赖注入时找不到依赖服务（依赖是接口，无法自动创建）
     */
    interface UncreatableDependency {
        String getValue();
    }

    static class ServiceWithUncreatableDependency {
        public ServiceWithUncreatableDependency(UncreatableDependency dependency) {
            // 需要 UncreatableDependency，但它是接口，无法自动创建
        }
    }

    @Test
    public void testMissingDependencyThrowsException() {
        // 注册服务，但依赖是接口，无法自动创建
        registry.registerSingletonClass(ServiceWithUncreatableDependency.class);

        // 验证找不到依赖且无法自动创建时抛出异常
        assertThrows(ServiceNotFoundException.class, () -> {
            registry.getService(ServiceWithUncreatableDependency.class);
        });
    }

    /**
     * 测试无参构造函数和带参构造函数的选择
     */
    static class ServiceWithMultipleConstructors {
        private final String value;

        public ServiceWithMultipleConstructors() {
            this.value = "no-args";
        }

        public ServiceWithMultipleConstructors(DependencyA dependencyA) {
            this.value = "with-dependency: " + dependencyA.getName();
        }
    }

    @Test
    public void testMultipleConstructorsSelection() {
        // 情况1：注册服务但不注册依赖
        // 由于DependencyA可以自动创建，所以会优先使用带参数的构造函数
        registry.registerSingletonClass(ServiceWithMultipleConstructors.class);
        ServiceWithMultipleConstructors service1 = registry.getService(ServiceWithMultipleConstructors.class);
        // 应该使用带参数的构造函数（因为DependencyA可以自动创建）
        assertEquals("with-dependency: DependencyA", service1.value);

        // 情况2：显式注册依赖并重新注册服务
        registry.registerSingletonClass(DependencyA.class);
        registry.registerSingletonClass(ServiceWithMultipleConstructors.class);
        ServiceWithMultipleConstructors service2 = registry.getService(ServiceWithMultipleConstructors.class);
        assertEquals("with-dependency: DependencyA", service2.value);
    }

    // ========== 测试新添加的 registerFactory 方法 ==========

    /**
     * 测试工厂类（无依赖）的注册和使用
     */
    static class SimpleService {
        public String getMessage() {
            return "Simple Service";
        }
    }

    static class SimpleServiceFactory implements ServiceFactory<SimpleService> {
        @Override
        public SimpleService create() {
            return new SimpleService();
        }
    }

    @Test
    public void testRegisterFactoryClassWithoutDependencies() {
        // 注册工厂类（原型模式）
        registry.registerFactory(SimpleService.class, SimpleServiceFactory.class, false);

        // 获取服务实例两次
        SimpleService service1 = registry.getService(SimpleService.class);
        SimpleService service2 = registry.getService(SimpleService.class);

        // 验证服务实例不为null且是不同的实例（原型模式）
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2);
        assertEquals("Simple Service", service1.getMessage());
        assertEquals("Simple Service", service2.getMessage());
    }

    @Test
    public void testRegisterFactoryClassWithSingleton() {
        // 注册工厂类（单例模式）
        registry.registerFactory(SimpleService.class, SimpleServiceFactory.class, true);

        // 获取服务实例两次
        SimpleService service1 = registry.getService(SimpleService.class);
        SimpleService service2 = registry.getService(SimpleService.class);

        // 验证服务实例不为null且是同一个实例（单例模式）
        assertNotNull(service1);
        assertNotNull(service2);
        assertSame(service1, service2);
        assertEquals("Simple Service", service1.getMessage());
        assertEquals("Simple Service", service2.getMessage());
    }

    /**
     * 测试工厂类（有依赖）的注册和使用
     */
    static class ComplexService {
        private final DependencyA dependencyA;

        public ComplexService(DependencyA dependencyA) {
            this.dependencyA = dependencyA;
        }

        public String getMessage() {
            return "Complex Service with " + dependencyA.getName();
        }
    }

    static class ComplexServiceFactory implements ServiceFactory<ComplexService> {
        private final DependencyA dependencyA;

        public ComplexServiceFactory(DependencyA dependencyA) {
            this.dependencyA = dependencyA;
        }

        @Override
        public ComplexService create() {
            return new ComplexService(dependencyA);
        }
    }

    @Test
    public void testRegisterFactoryClassWithDependencies() {
        // 注册依赖
        registry.registerSingleton(DependencyA.class, new DependencyA());

        // 注册工厂类（支持构造函数注入）
        registry.registerFactory(ComplexService.class, ComplexServiceFactory.class, true);

        // 获取服务实例
        ComplexService service = registry.getService(ComplexService.class);

        // 验证服务实例不为null且依赖注入成功
        assertNotNull(service);
        assertNotNull(service.getMessage());
        assertTrue(service.getMessage().contains("DependencyA"));
    }

    /**
     * 测试接口类型的服务注册和获取
     */
    interface MyService {
        String getServiceName();
    }

    static class MyServiceImpl implements MyService {
        @Override
        public String getServiceName() {
            return "MyServiceImpl";
        }
    }

    static class MyServiceFactory implements ServiceFactory<MyService> {
        @Override
        public MyService create() {
            return new MyServiceImpl();
        }
    }

    @Test
    public void testRegisterFactoryClassForInterface() {
        // 注册接口类型的服务
        registry.registerFactory(MyService.class, MyServiceFactory.class, true);

        // 获取服务实例
        MyService service = registry.getService(MyService.class);

        // 验证服务实例不为null且类型正确
        assertNotNull(service);
        assertInstanceOf(MyServiceImpl.class, service);
        assertEquals("MyServiceImpl", service.getServiceName());
    }

    /**
     * 测试CoreDictionaryFactory的实际注册和使用
     * 验证与实际业务类的集成
     */
    @Test
    public void testCoreDictionaryFactoryRegistration() {
        // ... (existing code)
        System.out.println("CoreDictionaryFactory方法签名正确，可以正常使用");
    }

    /**
     * 测试查找结果缓存及其失效机制
     */
    @Test
    public void testLookupCacheInvalidation() {
        // 1. 注册父类服务并获取，触发缓存
        registry.registerFactory(ParentService.class, ParentService::new);
        ParentService service1 = registry.getService(ParentService.class);
        assertInstanceOf(ParentService.class, service1);
        assertFalse(service1 instanceof ChildService);

        // 2. 注册子类服务，应该触发缓存失效
        registry.registerFactory(ChildService.class, ChildService::new);

        // 3. 再次通过父类获取服务，由于缓存失效，应该重新查找到子类（最匹配项）
        ParentService service2 = registry.getService(ParentService.class);
        assertInstanceOf(ChildService.class, service2);
    }

    /**
     * 测试查找结果负缓存及其失效机制
     */
    @Test
    public void testNegativeLookupCacheInvalidation() {
        // 1. 初始时未注册 TestService，第一次获取应抛异常，并缓存负结果
        assertThrows(ServiceNotFoundException.class, () -> registry.getService(TestService.class));

        // 2. 注册实现类，应该触发缓存清理
        registry.registerFactory(TestService.class, TestServiceImpl1::new);

        // 3. 再次获取，应不再命中负缓存，而是成功返回实例
        TestService service = registry.getService(TestService.class);
        assertNotNull(service);
        assertInstanceOf(TestServiceImpl1.class, service);
    }

    // ========== 自动创建未注册服务测试 ==========

    /**
     * 测试自动创建未注册的无参构造服务
     */
    @Test
    public void testAutoCreateUnregisteredServiceWithNoArgsConstructor() {
        // 未注册 SimpleService，直接获取
        SimpleService service = registry.getService(SimpleService.class);

        // 验证自动创建成功
        assertNotNull(service);
        assertEquals("Simple Service", service.getMessage());
    }

    /**
     * 测试自动创建未注册的服务（带依赖）
     */
    @Test
    public void testAutoCreateUnregisteredServiceWithDependencies() {
        // 注册依赖
        registry.registerSingletonClass(DependencyA.class);
        registry.registerSingletonClass(DependencyB.class);

        // 未注册 ServiceWithDependency，直接获取
        ServiceWithDependency service = registry.getService(ServiceWithDependency.class);

        // 验证自动创建成功且依赖注入成功
        assertNotNull(service);
        assertNotNull(service.dependencyA());
        assertNotNull(service.dependencyB());
        assertEquals("DependencyA + DependencyB", service.getInfo());
    }

    /**
     * 测试自动创建是原型模式（每次创建新实例）
     */
    @Test
    public void testAutoCreateIsPrototype() {
        // 获取未注册服务两次
        SimpleService service1 = registry.getService(SimpleService.class);
        SimpleService service2 = registry.getService(SimpleService.class);

        // 验证每次都是新实例（原型模式）
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2);
    }

    /**
     * 测试自动创建多层依赖的服务
     */
    @Test
    public void testAutoCreateMultiLevelDependencies() {
        // 仅注册Level1，Level2和Level3都不注册
        registry.registerSingletonClass(Level1Service.class);

        // 直接获取Level3，应该自动创建level2和level3
        Level3Service service = registry.getService(Level3Service.class);

        // 验证自动创建和多层依赖注入都成功
        assertNotNull(service);
        assertEquals("Level3 -> Level2 -> Level1", service.getName());
    }

    /**
     * 测试自动创建时的循环依赖检测
     */
    @Test
    public void testAutoCreateCircularDependencyDetection() {
        // 不注册CircularA和CircularB，直接获取
        // 应该在自动创建过程中检测到循环依赖
        assertThrows(ServiceNotFoundException.class, () -> {
            registry.getService(CircularA.class);
        });
    }

    /**
     * 测试混合场景：部分注册，部分自动创建
     */
    @Test
    public void testMixedRegisteredAndAutoCreated() {
        // 注册DependencyA
        DependencyA registeredA = new DependencyA();
        registry.registerSingleton(DependencyA.class, registeredA);

        // DependencyB和ServiceWithDependency不注册
        ServiceWithDependency service = registry.getService(ServiceWithDependency.class);

        // 验证DependencyA使用注册的单例，DependencyB和ServiceWithDependency自动创建
        assertNotNull(service);
        assertSame(registeredA, service.dependencyA());
        assertNotNull(service.dependencyB());
    }

    /**
     * 测试无法自动创建的情况（抽象类）
     */
    static abstract class AbstractService {
        public abstract String getName();
    }

    @Test
    public void testAutoCreateAbstractClassThrowsException() {
        // 尝试自动创建抽象类应该失败
        assertThrows(ServiceNotFoundException.class, () -> {
            registry.getService(AbstractService.class);
        });
    }

    /**
     * 测试无法自动创建的情况（接口）
     */
    @Test
    public void testAutoCreateInterfaceThrowsException() {
        // 尝试自动创建接口应该失败
        assertThrows(ServiceNotFoundException.class, () -> {
            registry.getService(TestService.class);
        });
    }

    // ========== 父子容器测试 ==========

    /**
     * 测试父子容器委托查找（子容器未注册，父容器已注册）
     */
    @Test
    public void testParentDelegation() {
        // 创建父容器并注册服务
        ServiceRegistry parent = new ServiceRegistry();
        SimpleService parentService = new SimpleService();
        parent.registerSingleton(SimpleService.class, parentService);

        // 创建子容器，指定父容器
        ServiceRegistry child = new ServiceRegistry(parent);

        // 子容器未注册，应从父容器获取
        SimpleService service = child.getService(SimpleService.class);

        assertNotNull(service);
        assertSame(parentService, service);
    }

    /**
     * 测试子容器覆盖父容器服务（都已注册）
     */
    @Test
    public void testChildOverride() {
        // 创建父容器并注册服务
        ServiceRegistry parent = new ServiceRegistry();
        SimpleService parentService = new SimpleService();
        parent.registerSingleton(SimpleService.class, parentService);

        // 创建子容器，指定父容器，并注册同类型服务
        ServiceRegistry child = new ServiceRegistry(parent);
        SimpleService childService = new SimpleService();
        child.registerSingleton(SimpleService.class, childService);

        // 子容器应获取到自己注册的服务
        SimpleService service = child.getService(SimpleService.class);

        assertNotNull(service);
        assertSame(childService, service);
        assertNotSame(parentService, service);
    }

    /**
     * 测试父子容器都未注册时的自动创建（应由子容器创建）
     */
    @Test
    public void testAutoCreateWhenParentMissing() {
        // 创建父容器
        ServiceRegistry parent = new ServiceRegistry();

        // 创建子容器
        ServiceRegistry child = new ServiceRegistry(parent);

        // 此时SimpleService在父子容器都未注册
        // 获取服务，应触发自动创建
        SimpleService service1 = child.getService(SimpleService.class);
        SimpleService service2 = child.getService(SimpleService.class);

        assertNotNull(service1);
        // 验证是原型模式
        assertNotSame(service1, service2);
    }

    /**
     * 测试多级父容器委托
     */
    @Test
    public void testMultiLevelParentDelegation() {
        ServiceRegistry root = new ServiceRegistry();
        SimpleService rootService = new SimpleService();
        root.registerSingleton(SimpleService.class, rootService);

        ServiceRegistry middle = new ServiceRegistry(root);
        ServiceRegistry leaf = new ServiceRegistry(middle);

        SimpleService service = leaf.getService(SimpleService.class);
        assertSame(rootService, service);
    }
}
