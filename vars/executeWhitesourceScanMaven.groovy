import com.sap.cloud.sdk.s4hana.pipeline.BashUtils
import java.nio.file.Paths

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeWhitesourceScanMaven', stepParameters: parameters) {
        final script = parameters.script

        try {
            withCredentials([string(credentialsId: parameters.credentialsId, variable: 'orgToken')]) {
                List defines = [
                    "-Dorg.whitesource.orgToken=${BashUtils.escape(orgToken)}",
                    "-Dorg.whitesource.product=${BashUtils.escape(parameters.product)}",
                    '-Dorg.whitesource.checkPolicies=true',
                    '-Dorg.whitesource.failOnError=true'
                ]

                if (parameters.projectName) {
                    defines.add("-Dorg.whitesource.aggregateProjectName=${BashUtils.escape(parameters.projectName)}")
                    defines.add('-Dorg.whitesource.aggregateModules=true')
                }

                if (parameters.whitesourceUserTokenCredentialsId) {
                    withCredentials([string(credentialsId: parameters.whitesourceUserTokenCredentialsId, variable: 'userKey')]) {
                        defines.add("-Dorg.whitesource.userKey=${BashUtils.escape(userKey)}")
                    }
                }

                mavenExecute(
                    script: script,
                    m2Path: s4SdkGlobals.m2Directory,
                    pomPath: Paths.get(parameters.basePath, "application", "pom.xml").toString(),
                    goals: 'org.whitesource:whitesource-maven-plugin:update',
                    defines: defines.join(' ')
                )
            }
        } finally {
            String whiteSourceTarget = Paths.get(parameters.basePath, "application", "target", "site", "whitesource").toString()

            publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true,
                         reportDir   : whiteSourceTarget,
                         reportFiles : 'index.html', reportName: "Whitesource Policy Check (${parameters.basePath})"])
        }
    }
}
