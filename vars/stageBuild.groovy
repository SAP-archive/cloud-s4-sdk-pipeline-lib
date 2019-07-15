import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

import static com.sap.cloud.sdk.s4hana.pipeline.MavenUtils.installMavenArtifacts

def call(Map parameters = [:]) {
    Script script = parameters.script
    build(script)

    if (BuildToolEnvironment.instance.isNpm()) {
        packageJsApp(script)
    }
}

private build(Script script) {
    def stageName = 'build'
    runAsStage(stageName: stageName, script: script) {
        if (BuildToolEnvironment.instance.isMta()) {

            withEnv(['MAVEN_OPTS=-Dmaven.repo.local=../s4hana_pipeline/maven_local_repo']) {
                mtaBuild(script: script)
            }

            runOverModules(script: this, moduleType: "java") { String basePath ->

                String pathToPom = BuildToolEnvironment.instance.getApplicationPomXmlPath(basePath)
                def pom

                if (fileExists(pathToPom)) {
                    pom = readMavenPom file: pathToPom
                } else {
                    error("File ${pathToPom} was expected, but does not exist.")
                }
                installMavenArtifacts(script, pom, basePath, pathToPom)
            }

            // mta-builder executes 'npm install --production', therefore we need 'npm ci/install' to install the dev-dependencies
            runOverModules(script: script, moduleType: 'html5') { basePath ->
                dir(basePath) {
                    installAndBuildNpm(script: script)
                }
            }
            // The MTA builder executes the maven command only inside the java module directories and not on the root directory.
            // Hence we need install root pom to local repository after the project is built using the mta builder
            def pomFile = "pom.xml"
            if (fileExists(pomFile)) {
                def pom = readMavenPom file: pomFile
                mavenExecute(script: script,
                    goals: 'install:install-file',
                    m2Path: 's4hana_pipeline/maven_local_repo',
                    defines: ["-Dfile=${pomFile}",
                              "-DpomFile=${pomFile}",
                              "-DgroupId=${pom.groupId}",
                              "-DartifactId=${pom.artifactId}",
                              "-Dversion=${pom.version}",
                              "-Dpackaging=pom"].join(" "))
            }

        } else if (BuildToolEnvironment.instance.isNpm()) {
            installAndBuildNpm script: script, customScripts: ['ci-build']
        } else {
            mavenCleanInstall script: script
            if (fileExists('package.json')) {
                installAndBuildNpm script: script
            }
        }
    }
}

private packageJsApp(script) {
    String stageName = 'package'
    runAsStage(stageName: stageName, script: script) {
        executeNpm(script: script, dockerOptions: []) {
            sh "npm run ci-package"
        }
    }
}
