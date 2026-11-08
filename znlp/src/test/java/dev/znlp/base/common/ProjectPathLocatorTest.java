package dev.znlp.base.common;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectPathLocator 测试类
 *
 * @author jimichan
 */
class ProjectPathLocatorTest {

    @Test
    void testLocateDataTrainNer() {
        // 测试定位 znlp-data/data_train/ner 目录
        File nerDir = ProjectPathLocator.locate("znlp-data/data_train/ner");
        assertNotNull(nerDir);
        assertTrue(nerDir.exists());
        assertTrue(nerDir.isDirectory());
        System.out.println("找到 NER 训练数据目录: " + nerDir.getAbsolutePath());
    }

    @Test
    void testLocateDataTrainNerTrain() {
        // 测试定位 znlp-data/data_train/ner/train 子目录
        File trainDir = ProjectPathLocator.locate("znlp-data/data_train/ner/train");
        assertNotNull(trainDir);
        assertTrue(trainDir.exists());
        assertTrue(trainDir.isDirectory());
        System.out.println("找到 NER 训练数据子目录: " + trainDir.getAbsolutePath());
    }

    @Test
    void testLocateDataCorpus() {
        // 测试定位 znlp-data/data_corpus 目录
        File corpusDir = ProjectPathLocator.locate("znlp-data/data_corpus");
        assertNotNull(corpusDir);
        assertTrue(corpusDir.exists());
        assertTrue(corpusDir.isDirectory());
        System.out.println("找到语料数据目录: " + corpusDir.getAbsolutePath());
    }

    @Test
    void testTryLocateNonExistent() {
        // 测试定位不存在的路径（使用 tryLocate）
        File nonExistent = ProjectPathLocator.tryLocate("znlp-data/non-existent-path");
        assertNull(nonExistent);
        System.out.println("不存在的路径正确返回 null");
    }

    @Test
    void testLocateNonExistentThrowsException() {
        // 测试定位不存在的路径（使用 locate，应该抛出异常）
        assertThrows(IllegalStateException.class, () -> {
            ProjectPathLocator.locate("znlp-data/non-existent-path");
        });
        System.out.println("不存在的路径正确抛出异常");
    }

    @Test
    void testExists() {
        // 测试 exists 方法
        assertTrue(ProjectPathLocator.exists("znlp-data/data_train/ner"));
        assertFalse(ProjectPathLocator.exists("znlp-data/non-existent"));
        System.out.println("exists 方法工作正常");
    }

    @Test
    void testLocateWithCustomDepth() {
        // 测试自定义查找深度
        File nerDir = ProjectPathLocator.locate("znlp-data/data_train/ner", 5);
        assertNotNull(nerDir);
        assertTrue(nerDir.exists());
        System.out.println("自定义深度查找成功: " + nerDir.getAbsolutePath());
    }

    @Test
    void testLocateProjectRoot() {
        // 测试定位项目根目录
        File projectRoot = ProjectPathLocator.locateProjectRoot();
        assertNotNull(projectRoot);
        assertTrue(projectRoot.exists());
        
        // 验证项目根目录包含标识文件（settings.gradle.kts 或 build.gradle.kts）
        File settingsGradle = new File(projectRoot, "settings.gradle.kts");
        File buildGradle = new File(projectRoot, "build.gradle.kts");
        assertTrue(settingsGradle.exists() || buildGradle.exists(), 
                "项目根目录应包含 settings.gradle.kts 或 build.gradle.kts");
        
        System.out.println("找到项目根目录: " + projectRoot.getAbsolutePath());
    }

    @Test
    void testLocateProjectRootWithCustomMarker() {
        // 测试使用自定义标识文件定位项目根目录
        File projectRoot = ProjectPathLocator.locateProjectRoot("settings.gradle.kts");
        assertNotNull(projectRoot);
        assertTrue(projectRoot.exists());
        System.out.println("使用自定义标识文件找到项目根目录: " + projectRoot.getAbsolutePath());
    }

    @Test
    void testInvalidArguments() {
        // 测试无效参数
        assertThrows(IllegalArgumentException.class, () -> {
            ProjectPathLocator.locate(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            ProjectPathLocator.locate("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            ProjectPathLocator.locate("znlp-data", 0);
        });
        
        System.out.println("无效参数检查正常");
    }

    @Test
    void testCompareWithTestHelper() {
        // 比较新工具类与现有 TestHelper 的行为
        // ProjectPathLocator 可以找到 znlp-data 外部目录
        File nerDir = ProjectPathLocator.locate("znlp-data/data_train/ner");
        
        // 也可以找到项目内部目录
        File projectRoot = ProjectPathLocator.locateProjectRoot();
        File dataDir = new File(projectRoot, "data/dat");
        
        System.out.println("外部数据目录: " + nerDir.getAbsolutePath());
        System.out.println("项目根目录: " + projectRoot.getAbsolutePath());
        System.out.println("项目内部数据目录: " + dataDir.getAbsolutePath());
        
        // 验证路径正确
        assertNotNull(nerDir);
        assertNotNull(projectRoot);
    }

    @Test
    void testMultipleDataPaths() {
        // 测试定位多个不同的数据路径
        String[] paths = {
            "znlp-data/data_train/ner",
            "znlp-data/data_train/name-nr",
            "znlp-data/data_train/pinyin-split",
            "znlp-data/data_corpus/pku"
        };

        System.out.println("\n测试定位多个数据路径:");
        for (String path : paths) {
            File dir = ProjectPathLocator.tryLocate(path);
            if (dir != null) {
                System.out.println("✓ " + path + " -> " + dir.getAbsolutePath());
                assertTrue(dir.exists());
            } else {
                System.out.println("✗ " + path + " (不存在)");
            }
        }
    }

    @Test
    void testFindGradleProjectRoot() {
        // 测试查找 Gradle 项目根目录
        File gradleRoot = ProjectPathLocator.findGradleProjectRoot();
        assertNotNull(gradleRoot);
        assertTrue(gradleRoot.exists());
        
        // 验证包含 gradle/wrapper/gradle-wrapper.jar
        File wrapperJar = new File(gradleRoot, "gradle/wrapper/gradle-wrapper.jar");
        assertTrue(wrapperJar.exists(), "Gradle 项目根目录应包含 gradle/wrapper/gradle-wrapper.jar");
        
        System.out.println("找到 Gradle 项目根目录: " + gradleRoot.getAbsolutePath());
    }

    @Test
    void testTryFindGradleProjectRoot() {
        // 测试尝试查找 Gradle 项目根目录
        File gradleRoot = ProjectPathLocator.tryFindGradleProjectRoot();
        assertNotNull(gradleRoot);
        assertTrue(gradleRoot.exists());
        System.out.println("尝试查找 Gradle 项目根目录成功: " + gradleRoot.getAbsolutePath());
    }

    @Test
    void testEnsureProjectPath() {
        // 测试确保目录存在
        String testDirPath = "build/test-output/test-ensure-dir";
        File testDir = ProjectPathLocator.ensureProjectPath(testDirPath);
        
        assertNotNull(testDir);
        assertTrue(testDir.exists(), "目录应该被创建");
        assertTrue(testDir.isDirectory(), "应该是目录");
        
        System.out.println("确保目录存在: " + testDir.getAbsolutePath());
        
        // 再次调用应该不会报错
        File testDir2 = ProjectPathLocator.ensureProjectPath(testDirPath);
        assertEquals(testDir.getAbsolutePath(), testDir2.getAbsolutePath());
    }

    @Test
    void testEnsureProjectPathNested() {
        // 测试创建多层嵌套目录
        String nestedPath = "build/test-output/nested/deep/directory";
        File nestedDir = ProjectPathLocator.ensureProjectPath(nestedPath);
        
        assertNotNull(nestedDir);
        assertTrue(nestedDir.exists());
        assertTrue(nestedDir.isDirectory());
        
        System.out.println("创建嵌套目录成功: " + nestedDir.getAbsolutePath());
    }

    @Test
    void testGetProjectPath() {
        // 测试获取项目路径（不创建）
        String relativePath = "build/test-output/non-existent-yet";
        File path = ProjectPathLocator.getProjectPath(relativePath);
        
        assertNotNull(path);
        // 这个目录可能不存在，因为 getProjectPath 不创建目录
        
        // 验证路径是相对于项目根目录的
        File gradleRoot = ProjectPathLocator.findGradleProjectRoot();
        String expectedPath = new File(gradleRoot, relativePath).getAbsolutePath();
        assertEquals(expectedPath, path.getAbsolutePath());
        
        System.out.println("获取项目路径: " + path.getAbsolutePath());
    }

    @Test
    void testEnsureProjectPathInvalidPath() {
        // 测试无效路径
        assertThrows(IllegalArgumentException.class, () -> {
            ProjectPathLocator.ensureProjectPath(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            ProjectPathLocator.ensureProjectPath("");
        });
        
        System.out.println("无效路径检查正常");
    }

    @Test
    void testGradleRootVsOtherMarkers() {
        // 比较 Gradle wrapper 方式和其他标识文件方式
        File gradleRoot = ProjectPathLocator.findGradleProjectRoot();
        File otherRoot = ProjectPathLocator.locateProjectRoot();
        
        System.out.println("\n比较两种查找方式:");
        System.out.println("Gradle Wrapper 方式: " + gradleRoot.getAbsolutePath());
        System.out.println("标识文件方式: " + otherRoot.getAbsolutePath());
        
        // 在正常项目结构中，两者应该找到同一个根目录或相近的根目录
        assertNotNull(gradleRoot);
        assertNotNull(otherRoot);
    }
}
