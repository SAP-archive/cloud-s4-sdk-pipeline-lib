import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.BashUtils
import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils
import com.sap.cloud.sdk.s4hana.pipeline.PathUtils
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeFortifyScan', stepParameters: parameters) {
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

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'executeFortifyScan')
        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeFortifyScan')
        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        if (!configuration.dockerImage) {
            error("Error while executing fortifyScan. The value for dockerImage is empty, please provide an appropriate fortify client docker image name. For more information, please visit https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#executefortifyscan")
        }

        String pathToPom = PathUtils.normalize(basePath, 'pom.xml')

        if (!fileExists(pathToPom)) {
            error("Fortify stage expected a pom.xml file at \"${pathToPom}\", but no such file was found.")
        }

        String effectivePomFileLocation = basePath
        String effectivePomFileName = 'effectivePomFile.xml'
        String effectivePomFile = PathUtils.normalize(effectivePomFileLocation, effectivePomFileName)
        String artifactVersion = ''
        String artifactId = ''

        if (BuildToolEnvironment.instance.isMta()) {
            def mta = readYaml file: 'mta.yaml'
            artifactVersion = mta.version
            artifactId = script.commonPipelineEnvironment.configuration.artifactId
        } else {
            MavenUtils.generateEffectivePom(script, pathToPom, effectivePomFileName)

            if (!fileExists(effectivePomFile)) {
                error("Fortify stage expected an effective pom file, but no such file was generated.")
            }
            def effectivePom = readMavenPom file: effectivePomFile
            artifactId = effectivePom.artifactId
            artifactVersion = effectivePom.version
        }

        if(artifactVersion.isEmpty() || artifactId.isEmpty()){
            String errorMessage = "Invalid artifactId or artifactVersion value. Please ensure that" + (BuildToolEnvironment.instance.isMta() == true ) ? " the mta.yaml contains a valid artifactId and a version" : " the pom.xml contains a valid artifactId and a version"
            error(errorMessage)
        }
        def fortifyMavenScanOptions = [:]
        fortifyMavenScanOptions.script = script
        fortifyMavenScanOptions.pomPath = pathToPom
        fortifyMavenScanOptions.dockerImage = configuration.dockerImage
        fortifyMavenScanOptions.goals = [
            'fortify:translate',
            'fortify:scan'
        ].join(' ')

        String defaultBuildId = "${artifactId}-${artifactVersion}"

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
            updateFortifyProjectVersion(configuration, artifactVersion)
        } catch (Exception e) {
            error("Exception while updating project version in Fortify Software Security Center. \n Please ensure that the fortifyProjectName in the pipeline_config.yml matches the project name in the fortify server. This value is case sensitive. \n The projectVersionId is an integer id of the project that can be obtained by navigating to https://your-fortify-server/ssc/api/v1/projectVersions/ under currentState -> id.  ${Arrays.toString(e.getStackTrace())}")
        }

        try {
            dockerExecute(script: script, dockerImage: configuration.dockerImage) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: configuration.fortifyCredentialId, passwordVariable: 'password', usernameVariable: 'username']]) {
                    sh "fortifyclient uploadFPR -url ${configuration.sscUrl} -f ${PathUtils.normalize(basePath, 'target/result.fpr')} -application ${configuration.fortifyProjectName} -applicationVersion ${artifactVersion} -user ${username} -password ${BashUtils.escape(password)}"
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
