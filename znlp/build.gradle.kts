plugins {
    java
}

// 显式设置制品名称（artifactId），不依赖文件夹名
base {
    archivesName.set("znlp")
}

dependencies {
    runtimeOnly(project(":znlp-resources"))

    // Annotations
    compileOnly("org.jetbrains:annotations:26.0.2-1")

    // Logging (compile-only, user provides implementation)
    compileOnly("org.slf4j:slf4j-api:2.0.16")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")


    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Test logging implementation
    testRuntimeOnly("org.apache.logging.log4j:log4j-core:2.25.3")
    testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.24.3")


}

tasks.test {
    useJUnitPlatform {
        val includeTags = (findProperty("includeTags") as String?)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        if (!includeTags.isNullOrEmpty()) {
            includeTags(*includeTags.toTypedArray())
        } else {
            excludeTags("benchmark", "train", "performance", "heavy")
        }
    }
    maxHeapSize = "2G"
    testLogging {
        showStandardStreams = true
    }
}