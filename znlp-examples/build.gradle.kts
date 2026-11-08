plugins {
    application
}

dependencies {
    implementation(project(":znlp"))
    runtimeOnly(project(":znlp-resources"))

    compileOnly("org.jetbrains:annotations:26.0.2-1")
}

application {
    mainClass.set(
        providers.gradleProperty("mainClass")
            .orElse("dev.znlp.examples.CwsPerceptronExample")
    )
}

tasks.named<JavaExec>("run") {
    maxHeapSize = "2G"
    // 以仓库根目录为工作目录，示例里的 data/... 相对路径才能统一生效
    workingDir = rootProject.projectDir
    // 透传参数：-PappArgs="arg1 arg2"
    providers.gradleProperty("appArgs").orNull?.let { raw ->
        args = raw.split(" ").filter { it.isNotBlank() }
    }
}
