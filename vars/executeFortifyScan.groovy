import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.BashUtils
import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils
import com.sap.cloud.sdk.s4hana.pipeline.PathUtils
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeFortifyScan') {
        final script = parameters.script
        final String basePath = parameters.basePath
        final Set parameterKeys = [
            'fortifyCredentialId',
            'fortifyProjectName',
            'projectVersionId',
            'sscUrl'
        ]

        final Set stepConfigurationKeys = [
            'dockerImage',
            'dockerOptions',
            'projectSettingsFile',
            'verbose',
            'sourceVersion',
            'buildId',
            'use64BitVersion',
            'maximumMemoryUsage',
            'exclude',
            'skipNgComponents',
            'additionalScanOptions'
        ]
        String pathToPom = PathUtils.normalize(basePath, 'pom.xml')

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'executeFortifyScan')
        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeFortifyScan')
        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        if (!configuration.dockerImage) {
            error("Error while executing fortifyScan. The value for dockerImage is empty, please provide an appropriate fortify client docker image name.")
        }
        if (!fileExists(pathToPom)) {
            error("Fortify stage expected a pom.xml file at \"${pathToPom}\", but no such file was found.")
        }

        String effectivePomFileLocation = basePath
        String effectivePomFileName = 'effectivePomFile.xml'
        String effectivePomFile = PathUtils.normalize(effectivePomFileLocation, effectivePomFileName)

        MavenUtils.generateEffectivePom(script, pathToPom, effectivePomFileName)
        if (!fileExists(effectivePomFile)) {
            error("Fortify stage expected an effective pom file, but no such file was generated.")
        }
        def pom = readMavenPom file: effectivePomFile

        // clean compile scan
        def fortifyMavenScanOptions = [:]
        fortifyMavenScanOptions.script = script
        fortifyMavenScanOptions.pomPath = pathToPom
        fortifyMavenScanOptions.dockerImage = configuration.dockerImage
        fortifyMavenScanOptions.goals = [
            'com.hpe.security.fortify.maven.plugin:sca-maven-plugin:translate',
            'com.hpe.security.fortify.maven.plugin:sca-maven-plugin:scan'
        ].join(' ')

        String defaultBuildId = "${pom.artifactId}-${pom.version}"

        def fortifyDefines = [
            "-Dfortify.sca.verbose=${configuration.verbose}",
            "-Dfortify.sca.source.version=${configuration.sourceVersion}",
            "-Dfortify.sca.buildId=${configuration.buildId ?: defaultBuildId}",
            "-Dfortify.sca.64bit=${configuration.use64BitVersion}",
            "-Dfortify.sca.Xmx=${configuration.maximumMemoryUsage}",
            "-Dfortify.sca.exclude=\"${configuration.exclude}\"",
            "-DskipNgComponents=${configuration.skipNgComponents}"
        ]

        if (configuration.additionalScanOptions) {
            fortifyDefines.add(configuration.additionalScanOptions)
        }

        fortifyMavenScanOptions.defines = fortifyDefines.join(' ')

        if (configuration.projectSettingsFile) {
            fortifyMavenScanOptions.projectSettingsFile = configuration.projectSettingsFile
        }

        mavenExecute(fortifyMavenScanOptions)

        try {
            updateFortifyProjectVersion(configuration, pom.version)
        } catch (Exception e) {
            error("Exception while updating project version in Fortify Software Security Center \n" + Arrays.toString(e.getStackTrace()))
        }

        String fprFilePath = PathUtils.normalize(basePath, 'target/fortify/')
        String fprFile = PathUtils.normalize(fprFilePath, "${pom.artifactId}-${pom.version}.fpr")

        try {
            dockerExecute(script: script, dockerImage: configuration.dockerImage) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: configuration.fortifyCredentialId, passwordVariable: 'password', usernameVariable: 'username']]) {
                    sh "fortifyclient uploadFPR -url ${configuration.sscUrl} -f ${fprFile} -application ${configuration.fortifyProjectName} -applicationVersion ${pom.version} -user ${username} -password ${BashUtils.escape(password)}"
                }
            }
        } catch (Exception e) {
            error("Exception while uploading scan results to Fortify SSC \n" + e.toString())
        }
    }
}

def updateFortifyProjectVersion(configuration, version) {
    def authHeaderValue = 'Basic '
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: configuration.fortifyCredentialId, passwordVariable: 'password', usernameVariable: 'username']]) {
        authHeaderValue += getEncodedAuthToken(username, password)
    }
    versionResponse = httpRequest(url: "${configuration.sscUrl}/api/v1/projectVersions/${configuration.projectVersionId}/",
        acceptType: 'APPLICATION_JSON',
        customHeaders: [[name: 'Authorization', value: authHeaderValue]],
        quiet: false)

    versionResponse = readJSON text: versionResponse.content
    if (versionResponse.data.name != version) {
        versionResponse.data.name = version
        httpRequest(url: "${configuration.sscUrl}/api/v1/projectVersions/${configuration.projectVersionId}/",
            acceptType: 'APPLICATION_JSON',
            customHeaders: [[name: 'Authorization', value: authHeaderValue]],
            contentType: 'APPLICATION_JSON',
            httpMode: 'PUT',
            requestBody: versionResponse.data.toString(),
            quiet: false)
    }
}

@NonCPS
def getEncodedAuthToken(username, password) {
    return Base64.getEncoder().encodeToString(String.format("%s:%s", username, password).getBytes())
}
