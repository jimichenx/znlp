plugins {
    java
}

allprojects {
    group = "dev.znlp"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    tasks.withType<Javadoc>().configureEach {
        (options as CoreJavadocOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            encoding = "UTF-8"
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // -classfile 排除依赖 jar 中无法解析的注解警告（如 log4j-api 的 @BaselineIgnore）
        options.compilerArgs.addAll(listOf("-Xlint:all,-classfile", "-Werror"))
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
