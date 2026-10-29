apply plugin: "java"

sourceSets.main.java.srcDirs = ["src"]

ext{
    mindustryVersion = "v160"
    isWindows = System.getProperty("os.name").toLowerCase().contains("windows")
    sdkRootVal = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
}

repositories{
    mavenCentral()
    maven { url = 'https://jitpack.io' }
    ivy{
        url = 'https://github.com/'
        patternLayout{ artifact(
            (mindustryVersion == "latest" ? '/[organisation]/[module]/releases/[revision]/download/dependencies.jar' :
            mindustryVersion == "be" ? '/[organisation]/[module]/releases/download/master/[revision].jar' :
            '/[organisation]/[module]/releases/download/[revision]/dependencies.jar'))
        }
        metadataSources{ artifact() }

        content{
            if(mindustryVersion == "be"){
                includeVersion("Anuken", "MindustryBuilds", "latest")
            }else{
                includeVersion("Anuken", "Mindustry", mindustryVersion)
            }
        }
    }
}

java{
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

dependencies{
    compileOnly mindustryVersion == "be" ? "Anuken:MindustryBuilds:latest" : "Anuken:Mindustry:$mindustryVersion"
}

// 任务注册阶段就拿到config，不要放到doLast里面
def compileCp = configurations.compileClasspath
def runtimeCp = configurations.runtimeClasspath

tasks.register('jarAndroid'){
    dependsOn "jar"
    def projectName = project.name
    def sdkRoot = project.ext.sdkRootVal
    def isWin = isWindows

    doLast{
        if(!sdkRoot || !new File(sdkRoot).exists()) throw new GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")

        def platformRoot = new File("$sdkRoot/platforms/").listFiles().sort().reverse().find{ f -> new File(f, "android.jar").exists() }

        if(!platformRoot) throw new GradleException("No android.jar found. Ensure that you have an Android platform installed.")

        def dependencies = (compileCp.asList() + runtimeCp.asList() + [new File(platformRoot, "android.jar")]).collect{ "--classpath $it.path" }.join(" ")

        def d8 = isWin ? "d8.bat" : "d8"

        "$d8 $dependencies --min-api 21 --output ${projectName}Android.jar ${projectName}Desktop.jar"
                .execute(null, new File("build/libs")).waitForProcessOutput(System.out, System.err)
    }
}

jar{
    archiveFileName = "${project.name}Desktop.jar"

    from{
        configurations.runtimeClasspath.collect{ it.isDirectory() ? it : zipTree(it) }
    }

    from(projectDir){
        include "mod.hjson"
    }

    from("assets/"){
        include "**"
    }
}

tasks.register('deploy', Jar){
    def projectName = project.name
    dependsOn jarAndroid
    dependsOn jar
    archiveFileName = "${projectName}.jar"

    from{ [zipTree("build/libs/${projectName}Desktop.jar"), zipTree("build/libs/${projectName}Android.jar")] }

    doLast{
        delete{
            delete "build/libs/${projectName}Android.jar"
        }
    }
}
