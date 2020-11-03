package com.sap.cloud.sdk.s4hana.pipeline

class MavenUtils implements Serializable {
    static final long serialVersionUID = 1L

    static void flattenPomXmls(Script script){
        script.mavenExecute(
            script: script,
            goals: ['flatten:flatten'],
            m2Path: 's4hana_pipeline/maven_local_repo',
            defines: ["-Dflatten.mode=resolveCiFriendliesOnly"]
        )
    }
    static void generateEffectivePom(Script script, String pomFile, String effectivePomFile) {
        script.mavenExecute(script: script,
            flags: ['--batch-mode'],
            pomPath: pomFile,
            m2Path: script.s4SdkGlobals.m2Directory,
            goals: ['help:effective-pom'],
            defines: ["-Doutput=${effectivePomFile}"])
    }

    static void installMavenArtifacts(Script script, String pathToMavenModule) {

        String pathToPom = "$pathToMavenModule/pom.xml"

        if (!script.fileExists(pathToPom)) {
            script.error("To install the the maven artifacts to the local maven repository a pom.xml file at \"${pathToPom}\" is expected, but no such file was found." +
                "If there is a pom.xml in that location then please do not hesitate to open an issue at https://github.com/SAP/cloud-s4-sdk-pipeline/issues")
        }

        String pathToFlattenedPom = "$pathToMavenModule/.flattened-pom.xml"
        if (script.fileExists(pathToFlattenedPom)) {
            pathToPom = pathToFlattenedPom
        }

        def pom = script.readMavenPom file: pathToPom


        if (pom.packaging == "pom") {
            installFile(script, pathToPom, pathToPom)
        } else {
            String pathToTargetDirectory = PathUtils.normalize(pathToMavenModule, '/target')

            List packagingFiles = script.findFiles(glob: "$pathToTargetDirectory/${pom.artifactId}*.${pom.packaging}")
            packagingFiles.each { def file -> installFile(script, pathToPom, file.getPath()) }

            List<String> classesJars = script.findFiles(glob: "$pathToTargetDirectory/${pom.artifactId}*-classes.jar")
            if (classesJars) {
                installFile(script, pathToPom, classesJars[0].getPath(), ["-Dpackaging=jar", "-Dclassifier=classes"])
            }
        }
    }

    static void installFile(Script script, String pathToPom, String file, List additionalDefines = []) {
        script.mavenExecute(
            script: script,
            goals: ['install:install-file'],
            m2Path: 's4hana_pipeline/maven_local_repo',
            defines: [
                "-Dfile=${file}",
                "-DpomFile=$pathToPom"
            ].plus(additionalDefines)
        )
    }

    static String getMavenDependencyTree(Script script, String basePath) {
        script.mavenExecute(script: script,
            flags: ["--batch-mode", "-DoutputFile=mvnDependencyTree.txt"],
            m2Path: script.s4SdkGlobals.m2Directory,
            pomPath: PathUtils.normalize(basePath, 'pom.xml'),
            goals: ["dependency:tree"])

        return script.readFile(PathUtils.normalize(basePath, 'mvnDependencyTree.txt'))
    }

    static List getTestModulesExcludeFlags(Script script) {
        List moduleExcludes = []
        if (script.fileExists('integration-tests/pom.xml')) {
            moduleExcludes << '-pl' << '!integration-tests'
        }
        if (script.fileExists('unit-tests/pom.xml')) {
            moduleExcludes << '-pl' << '!unit-tests'
        }
        return moduleExcludes
    }

    static void installRootPom(Script script) {
        installMavenArtifacts(script, './')
    }
}
