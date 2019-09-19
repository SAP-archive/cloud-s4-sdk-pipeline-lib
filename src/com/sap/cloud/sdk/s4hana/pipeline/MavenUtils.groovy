package com.sap.cloud.sdk.s4hana.pipeline

class MavenUtils implements Serializable {
    static final long serialVersionUID = 1L

    static void generateEffectivePom(script, pomFile, effectivePomFile) {
        script.mavenExecute(script: script,
            flags: '--batch-mode',
            pomPath: pomFile,
            m2Path: script.s4SdkGlobals.m2Directory,
            goals: 'help:effective-pom',
            defines: "-Doutput=${effectivePomFile}")
    }

    static void installMavenArtifacts(script, pom, String basePath, String pathToPom) {

        String pathToTargetDirectory = PathUtils.normalize(BuildToolEnvironment.instance.getApplicationPath(basePath), '/target')

        if (pom.packaging == "war") {
            List classesJars = script.findFiles(glob: "$pathToTargetDirectory/${pom.artifactId}*-classes.jar")
            if (classesJars.size() != 1) {
                script.error "Expected exactly one *-classes.jar file in $pathToTargetDirectory, but found ${classesJars?.join(', ')}"
            }
            script.mavenExecute(
                script: script,
                goals: 'install:install-file',
                m2Path: 's4hana_pipeline/maven_local_repo',
                defines: [
                    "-Dfile=${classesJars[0]}",
                    "-DpomFile=$pathToPom",
                    "-Dpackaging=jar",
                    "-Dclassifier=classes"
                ].join(" ")
            )
        }

        List packagingFiles = script.findFiles(glob: "$pathToTargetDirectory/${pom.artifactId}*.${pom.packaging}")
        packagingFiles.each { file ->
            script.mavenExecute(
                script: script,
                goals: 'install:install-file',
                m2Path: 's4hana_pipeline/maven_local_repo',
                defines: [
                    "-Dfile=${file}",
                    "-DpomFile=$pathToPom"
                ].join(" ")
            )
        }
    }

    static String getMavenDependencyTree(def script, String basePath) {
        script.mavenExecute(script: script,
            flags: "--batch-mode -DoutputFile=mvnDependencyTree.txt",
            m2Path: script.s4SdkGlobals.m2Directory,
            pomPath: PathUtils.normalize(basePath, 'pom.xml'),
            goals: "dependency:tree")

        return script.readFile(PathUtils.normalize(basePath, 'mvnDependencyTree.txt'))
    }
}
