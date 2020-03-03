import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.BashUtils
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.PathUtils
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.piper.Utils

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeFortifyScan', stepParameters: parameters) {
        final script = parameters.script
        final String basePath = parameters.basePath ?: '.'
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

        String artifactVersion
        String artifactId

        if (BuildToolEnvironment.instance.isMta()) {
            def mta = readYaml file: 'mta.yaml'
            if(!mta.version || !mta.ID){
                error("ID (${mta.ID}) or version (${mta.version}) are not configured in mta.yaml. Please specify these values.")
            }
            artifactVersion = mta.version
            artifactId = mta.ID
        } else {
            artifactVersion = Utils.evaluateFromMavenPom(script, pathToPom, 'project.version')
            artifactId = Utils.evaluateFromMavenPom(script, pathToPom, 'project.artifactId')
        }

        def fortifyMavenScanOptions = [:]
        fortifyMavenScanOptions.script = script
        fortifyMavenScanOptions.pomPath = pathToPom
        fortifyMavenScanOptions.dockerImage = configuration.dockerImage
        fortifyMavenScanOptions.m2Path = s4SdkGlobals.m2Directory
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
            echo "[Error] failed with exception: ${e.getMessage()}"
            error("Exception while updating project version in Fortify Software Security Center. \n Please ensure that the fortifyProjectName in the .pipeline/config.yml matches the project name in the fortify server. This value is case sensitive. \n The projectVersionId is an integer id of the project that can be obtained by navigating to https://your-fortify-server/ssc/api/v1/projectVersions/ under currentState -> id")
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
