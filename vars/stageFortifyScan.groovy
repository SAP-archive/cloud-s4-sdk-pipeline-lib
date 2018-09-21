import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'fortifyScan'
    def stepName = 'executeFortifyScan'
    def pathToPom = 'application/pom.xml'
    def script = parameters.script

    def stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    def executeFortifyScanConfiguration = ConfigurationLoader.stepConfiguration(script, stepName)

    runAsStage(stageName: stageName, script: script) {

        if (fileExists(pathToPom)) {
            pom = readMavenPom file: pathToPom
        } else {
            error("Fortify stage expected a pom.xml file at \"${pathToPom}\", but no such file was found.")
        }

        // clean compile scan
        mavenExecute(
            script: this,
            pomPath: pathToPom,
            dockerImage: executeFortifyScanConfiguration.dockerImage,
            goals: [
                'clean com.hpe.security.fortify.maven.plugin:sca-maven-plugin:clean',
                'compile com.hpe.security.fortify.maven.plugin:sca-maven-plugin:translate',
                'com.hpe.security.fortify.maven.plugin:sca-maven-plugin:scan'
            ].join(' '),
            projectSettingsFile: '/home/piper/.m2/settings.xml',
            defines: [
                '-Dfortify.sca.numOfWorkerThreads=8',
                '-Dfortify.sca.verbose=true',
                '-Dfortify.sca.source.version=1.7',
                '-Dfortify.sca.buildId=test',
                '-Dfortify.sca.64bit=true',
                '-Dfortify.sca.Xmx=8192M',
                '-Dfortify.sca.Xss=8M',
                "-Dfortify.sca.exclude='${workspace}/**/resources/**/*','${workspace}/**/target/**/*','${workspace}/**/unit-tests/**/*','${workspace}/**/integration-tests/**/*','${workspace}/**/src/main/webapp/**/*'",
                '-DskipNgComponents=true'
            ].join(' ')
        )

        String image = pom.artifactId
        String version = pom.version

        updateFortifyProjectVersion(stageConfiguration, version)

        withCredentials([string(credentialsId: executeFortifyScanConfiguration.fortifyCredentialId, variable: 'authToken')]) {
            dockerExecute(dockerImage: executeFortifyScanConfiguration.dockerImage) {
                sh "fortifyclient uploadFPR -url ${stageConfiguration.sscUrl} -f application/target/fortify/${image}-${version}.fpr -application ${stageConfiguration.fortifyProjectName} -applicationVersion ${version} -authtoken ${authToken}"
            }
        }

        executeFortifyAuditStatusCheck(
            fortifyProjectName: stageConfiguration.fortifyProjectName,
            fortifyProjectVersion: version,
            deltaMinutes: 20,
            spotCheckMinimum: 1,
            verbose: true,
            sscUrl: stageConfiguration.sscUrl,
            fortifyCredentialsId: stageConfiguration.fortifyApiCredentialId
        )
    }


}

def updateFortifyProjectVersion(stageConfiguration, version) {
    authHeaderValue = 'Basic '
    withCredentials([string(credentialsId: stageConfiguration.fortifyBasicAuthId, variable: 'authToken')]) {
        authHeaderValue += authToken
    }
    versionResponse = httpRequest(
        url: "${stageConfiguration.sscUrl}/api/v1/projectVersions/${stageConfiguration.projectVersionId}/",
        acceptType: 'APPLICATION_JSON',
        customHeaders: [[name: 'Authorization', value: authHeaderValue]],
        quiet: false
    )

    versionResponse = readJSON text: versionResponse.content
    if (versionResponse.data.name != version) {
        versionResponse.data.name = version
        httpRequest(
            url: "${stageConfiguration.sscUrl}/api/v1/projectVersions/${stageConfiguration.projectVersionId}/",
            acceptType: 'APPLICATION_JSON',
            customHeaders: [[name: 'Authorization', value: authHeaderValue]],
            contentType: 'APPLICATION_JSON',
            httpMode: 'PUT',
            requestBody: versionResponse.data.toString(),
            quiet: false
        )
    }
}
