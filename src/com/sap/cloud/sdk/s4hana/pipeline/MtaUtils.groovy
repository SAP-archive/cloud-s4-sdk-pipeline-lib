package com.sap.cloud.sdk.s4hana.pipeline

class MtaUtils implements Serializable {

    static void installAllMavenModules(Script script) {
        script.runOverModules(script: script, moduleType: "java") { String basePath ->
            MavenUtils.installMavenArtifacts(script, basePath)
        }
    }

    static void installAllNpmModules(Script script) {
        script.runOverModules(script: script, moduleType: ["html5", "nodejs"]) { String basePath ->
            String pathToPackageJson = "$basePath/package.json"
            if (script.fileExists(pathToPackageJson)) {
                script.dir(basePath) {
                    script.installAndBuildNpm(script: script)
                }
            }
        }
    }
}
