def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeWhitesourceScanMaven', stepParameters: parameters) {
        final script = parameters.script
        dir('application') {
            try {
                executeMaven(
                        script: script,
                        globalSettingsFile: "../${parameters.globalSettingsFile}",
                        m2Path: s4SdkGlobals.m2Directory,
                        goals: 'org.whitesource:whitesource-maven-plugin:update',
                        flags: "--batch-mode -Dorg.whitesource.orgToken=${parameters.orgToken} -Dorg.whitesource.product=\"${parameters.product}\" -Dorg.whitesource.checkPolicies=true"
                )
            } finally {
                archiveArtifacts artifacts: 'target/site/whitesource/**', allowEmptyArchive: true
                publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true,
                             reportDir   : 'target/site/whitesource',
                             reportFiles : 'index.html', reportName: 'Whitesource Policy Check (Maven)'])
            }
        }
    }
}