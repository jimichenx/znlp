---
title: 安装
description: znlp 的运行环境要求与三种引入方式，包括 jar 依赖、Gradle 复合构建与源码子项目。
---

import { Tabs, TabItem } from '@astrojs/starlight/components';

## 运行环境

| 项 | 要求 |
| :--- | :--- |
| JDK | **17 或更高**（`sourceCompatibility` / `targetCompatibility` 均为 `JavaVersion.VERSION_17`） |
| 构建工具 | Gradle 9.2.1（仓库自带 wrapper，无需本地安装 Gradle） |
| 运行时第三方依赖 | **无强制依赖** |

znlp 主库在运行时不引入任何强制的第三方库。`slf4j-api` 与 `log4j-api` 都是 `compileOnly`：如果宿主应用提供了 SLF4J 或 Log4j2，znlp 会自动接入；如果都没有，内部的 `base/logging` 会退回到 JDK 自带的 `java.util.logging`。因此你不必为了用 znlp 而额外引入日志框架。

## 模块坐标

| 模块 | 坐标 | 说明 |
| :--- | :--- | :--- |
| 主库 | `dev.znlp:znlp:0.1.0` | 全部 Java 代码 |
| 资源 | `dev.znlp:znlp-resources:1.0.0` | 词典与模型文件，纯资源 jar，无 Java 源码 |

:::caution[资源 jar 必须在运行时 classpath 上]
主库通过 `runtimeOnly(project(":znlp-resources"))` 依赖资源模块。这意味着**资源不会随主库传递过来**，你在自己的项目里必须显式把 `znlp-resources` 加到运行时 classpath，否则所有词典和模型都加载不到，会抛 `ResourceNotFoundException`。
:::

:::note[尚未发布到 Maven 仓库]
仓库当前**没有配置任何 `maven-publish` / `publishing` / `signing`**，所以 `dev.znlp:znlp` 无法从 Maven Central 或华为云镜像直接拉取。下面三种引入方式都不依赖远程仓库。如果你希望支持标准坐标依赖，需要先给项目补上 publishing 配置。
:::

## 方式一：构建 jar 后直接依赖（推荐）

最稳妥的方式，不依赖任何发布机制。

```bash
git clone git@codeup.aliyun.com:zaprt/znlp.git
cd znlp
./gradlew :znlp:jar :znlp-resources:jar
```

产物位置：

```
znlp/build/libs/znlp-0.1.0.jar
znlp-resources/build/libs/znlp-resources-1.0.0.jar
```

把这两个 jar 复制到你的项目（例如 `libs/` 目录），然后声明文件依赖：

<Tabs>
<TabItem label="Gradle (Kotlin DSL)">

```kotlin
dependencies {
    implementation(files(
        "libs/znlp-0.1.0.jar",
        "libs/znlp-resources-1.0.0.jar"
    ))
}
```

</TabItem>
<TabItem label="Gradle (Groovy)">

```groovy
dependencies {
    implementation files(
        'libs/znlp-0.1.0.jar',
        'libs/znlp-resources-1.0.0.jar'
    )
}
```

</TabItem>
<TabItem label="Maven">

Maven 没有原生的文件依赖，需要先安装到本地仓库：

```bash
mvn install:install-file \
  -Dfile=znlp/build/libs/znlp-0.1.0.jar \
  -DgroupId=dev.znlp -DartifactId=znlp \
  -Dversion=0.1.0 -Dpackaging=jar

mvn install:install-file \
  -Dfile=znlp-resources/build/libs/znlp-resources-1.0.0.jar \
  -DgroupId=dev.znlp -DartifactId=znlp-resources \
  -Dversion=1.0.0 -Dpackaging=jar
```

然后在 `pom.xml` 中声明，注意资源包的 scope 是 `runtime`：

```xml
<dependency>
  <groupId>dev.znlp</groupId>
  <artifactId>znlp</artifactId>
  <version>0.1.0</version>
</dependency>
<dependency>
  <groupId>dev.znlp</groupId>
  <artifactId>znlp-resources</artifactId>
  <version>1.0.0</version>
  <scope>runtime</scope>
</dependency>
```

</TabItem>
</Tabs>

## 方式二：Gradle 复合构建

如果 znlp 源码和你的项目在同一个工作区，可以让 Gradle 直接把它当作依赖解析，省去手动搬 jar。

在你的 `settings.gradle.kts` 中加入：

```kotlin
includeBuild("../znlp")
```

然后正常声明坐标依赖：

```kotlin
dependencies {
    implementation("dev.znlp:znlp:0.1.0")
    runtimeOnly("dev.znlp:znlp-resources:1.0.0")
}
```

Gradle 会按 `group` + 项目名把坐标替换为源码项目。这样改 znlp 源码后不需要重新打 jar。

:::tip
复合构建的坐标替换依赖被包含项目的 `group` 和项目名与坐标一致。znlp 的 `allprojects` 中设置了 `group = "dev.znlp"`，子项目名分别是 `znlp` 和 `znlp-resources`，因此 `dev.znlp:znlp` 与 `dev.znlp:znlp-resources` 都能正确替换。
:::

## 方式三：作为源码子项目引入

把 `znlp` 和 `znlp-resources` 两个目录拷进你的工程，在 `settings.gradle.kts` 里 `include` 它们，再用 `implementation(project(":znlp"))` 依赖。适合需要深度定制或跟踪主干开发的场景，缺点是升级时要手动同步。

## 验证安装

写一个最小类确认资源加载正常：

```java
import dev.znlp.Znlp;

public class SmokeTest {
    public static void main(String[] args) {
        System.out.println(Znlp.segmentToStrings("自然语言处理"));
    }
}
```

期望输出类似 `[自然语言, 处理]`。

首次运行会明显偏慢：znlp 需要把核心词典、二元语法表等原始资源解析成缓存文件写入 `data/cache/`（其中二元语法表缓存约 40 MB）。之后的启动会直接读缓存。缓存目录可通过配置调整，详见[配置与数据目录](../deployment/configuration/)。

## 下一步

- 跑通第一个完整例子：[快速开始](quick-start/)
- 了解资源从哪来、如何离线部署：[资源下载与离线部署](../deployment/resources/)
- 直接查方法签名：[API 参考](../reference/api/)
