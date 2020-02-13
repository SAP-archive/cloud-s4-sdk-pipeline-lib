package com.sap.cloud.sdk.s4hana.pipeline

class MtaUtils implements Serializable {

    static void installAllMavenModules(Script script) {
        script.runOverModules(script: script, moduleType: "java") { String basePath ->

            String pathToPom = PathUtils.normalize(basePath, 'pom.xml')

            if (!script.fileExists(pathToPom)) {
                script.error("To install the the maven artifacts to the local maven repository a pom.xml file at \"${pathToPom}\" is expexted, but no such file was found." +
                    "If there is a pom.xml in that location then please do not hesitate to open an issue at https://github.com/SAP/cloud-s4-sdk-pipeline/issues")
            }

            def pom = script.readMavenPom file: pathToPom
            MavenUtils.installMavenArtifacts(script, pom, basePath, pathToPom)
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
