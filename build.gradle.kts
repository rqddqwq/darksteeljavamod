val jarAndroid = tasks.register("jarAndroid"){
    dependsOn(tasks.jar)

    doLast{
        val sdk = sdkRoot?.let{ File(it) }
        if(sdk == null || !sdk.exists()){
            throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")
        }

        val platformRoot = File(sdk, "platforms").listFiles()
            ?.sortedDescending()
            ?.firstOrNull{ File(it, "android.jar").exists() }
            ?: throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")

        val classpath = (configurations.compileClasspath.get().files +
                configurations.runtimeClasspath.get().files +
                File(platformRoot, "android.jar"))

        val d8 = if(isWindows) "d8.bat" else "d8"

        val command = buildList{
            add(d8)
            classpath.forEach{ addAll(listOf("--classpath", it.path)) }
            addAll(listOf("--min-api", "21"))
            addAll(listOf("--output", "${projectName}Android.jar"))
            add("${projectName}Desktop.jar")
        }

        val libDir = layout.buildDirectory.dir("libs").get().asFile
        val desktopJarFile = File(libDir, "${projectName}Desktop.jar")
        if(!desktopJarFile.exists()){
            throw GradleException("Missing desktop jar: ${desktopJarFile.path}")
        }

        println("=====================================")
        println("D8 command: ${command.joinToString(" ")}")
        println("Working dir: ${libDir.path}")
        println("=====================================")

        val proc = ProcessBuilder(command)
            .directory(libDir)
            .redirectErrorStream(true)
            .start()

        proc.inputStream.reader().use { reader ->
            var line: String?
            while(true){
                line = reader.readLine()
                if(line == null) break
                println(line)
            }
        }
        val exitCode = proc.waitFor()

        if(exitCode != 0) throw GradleException("d8 failed with exit code $exitCode")
    }
}
