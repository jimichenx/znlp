base {
    archivesName.set("znlp-benchmarks")
}

val jmh by sourceSets.creating

configurations.named(jmh.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}
configurations.named(jmh.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.runtimeOnly.get())
}
configurations.named(jmh.annotationProcessorConfigurationName) {
    extendsFrom(configurations.annotationProcessor.get())
}

dependencies {
    add(jmh.implementationConfigurationName, project(":znlp"))
    add(jmh.runtimeOnlyConfigurationName, project(":znlp-resources"))
    add(jmh.implementationConfigurationName, "org.openjdk.jmh:jmh-core:1.37")
    add(jmh.annotationProcessorConfigurationName, "org.openjdk.jmh:jmh-generator-annprocess:1.37")
    add(jmh.runtimeOnlyConfigurationName, "org.apache.logging.log4j:log4j-core:2.25.3")
    add(jmh.runtimeOnlyConfigurationName, "org.apache.logging.log4j:log4j-slf4j2-impl:2.24.3")
}

tasks.named<Test>("test") {
    enabled = false
}

tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Run JMH segmentation benchmarks. Pass extra args with -PjmhArgs='...'"
    dependsOn(tasks.named(jmh.classesTaskName))
    classpath = jmh.runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    maxHeapSize = "2G"
    jvmArgs("-Dfile.encoding=UTF-8")

    val resultFile = layout.buildDirectory.file("results/jmh.json")
    val extra = (findProperty("jmhArgs") as String?).orEmpty()
    doFirst {
        resultFile.get().asFile.parentFile.mkdirs()
    }
    args(buildList {
        add("-rf")
        add("json")
        add("-rff")
        add(resultFile.get().asFile.absolutePath)
        add("-foe")
        add("true")
        if (extra.isBlank()) {
            add("dev.znlp.benchmark.segment")
        } else {
            extra.split(" ").filter { it.isNotBlank() }.forEach(::add)
        }
    })
}
