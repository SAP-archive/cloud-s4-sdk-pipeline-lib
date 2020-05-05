import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils

import java.nio.file.Paths

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeWhitesourceScanMaven', stepParameters: parameters) {
        final Script script = parameters.script
        String pomPath = 'pom.xml'

        try {
            withCredentials([string(credentialsId: parameters.credentialsId, variable: 'orgToken')]) {
                loadUserKey(parameters.whitesourceUserTokenCredentialsId) { userKey ->
                    List defines = [
                        "-Dorg.whitesource.orgToken=${orgToken}",
                        "-Dorg.whitesource.product=${parameters.product}",
                        '-Dorg.whitesource.checkPolicies=true',
                        '-Dorg.whitesource.failOnError=true'
                    ]

                    if (parameters.projectName) {
                        defines.add("-Dorg.whitesource.aggregateProjectName=${parameters.projectName}")
                        defines.add('-Dorg.whitesource.aggregateModules=true')
                    }

                    if (userKey) {
                        defines.add("-Dorg.whitesource.userKey=${userKey}")
                    }

                    if (parameters.productVersion) {
                        String productVersion = (parameters.productVersion == true) ? 'current' : parameters.productVersion
                        defines.add("-Dorg.whitesource.productVersion=$productVersion")
                    }

                    defines.addAll(MavenUtils.getTestModulesExcludeFlags(script))

                    mavenExecute(
                        script: script,
                        m2Path: s4SdkGlobals.m2Directory,
                        pomPath: pomPath,
                        goals: ['org.whitesource:whitesource-maven-plugin:19.5.1:update'],
                        defines: defines
                    )
                }
            }
        } finally {
            String whiteSourceTarget = Paths.get(parameters.basePath, "application", "target", "site", "whitesource").toString()

            publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true,
                         reportDir   : whiteSourceTarget,
                         reportFiles : 'index.html', reportName: "Whitesource Policy Check (${parameters.basePath})"])
        }
    }
}

private loadUserKey(whitesourceUserTokenCredentialsId, body) {
    if (whitesourceUserTokenCredentialsId) {
        withCredentials([string(credentialsId: whitesourceUserTokenCredentialsId, variable: 'userKey')]) {
            body(userKey)
        }
    } else {
        body(null)
    }
}
