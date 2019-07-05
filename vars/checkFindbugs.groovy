import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkFindbugs', stepParameters: parameters) {
        assertPluginIsActive('warnings-ng')

        def script = parameters.script
        String basePath = parameters.basePath

        final def stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'checkFindbugs')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'checkFindbugs')

        Set parameterKeys = [
            'scanModules',
            'dockerImage',
            'excludeFilterFile'
        ]

        Set stepConfigurationKeys = parameterKeys

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        def filterOptions = ''

        def excludeFilterFile = configuration.excludeFilterFile
        if (excludeFilterFile?.trim() && fileExists(excludeFilterFile)) {
            filterOptions += "-Dspotbugs.excludeFilterFile=${excludeFilterFile} "
        }

        def includeFilterFile = configuration.includeFilterFile
        def localIncludeFilerPath = "s4hana_pipeline/${includeFilterFile}"
        writeFile file: localIncludeFilerPath, text: libraryResource(includeFilterFile)
        filterOptions += "-Dspotbugs.includeFilterFile=${localIncludeFilerPath}"

        executeMavenSpotBugsForConfiguredModules(script, filterOptions, configuration, basePath)

        executeWithLockedCurrentBuildResult(
            script: script,
            errorStatus: 'FAILURE',
            errorHandler: script.buildFailureReason.setFailureReason,
            errorHandlerParameter: 'Findbugs',
            errorMessage: "Please examine the FindBugs/SpotBugs reports. For more information, please visit https://blogs.sap.com/2017/09/20/static-code-checks/"
        ) {
                recordIssues failedTotalHigh: 1,
                    failedTotalNormal: 10,
                    blameDisabled: true,
                    enabledForFailure: true,
                    aggregatingResults: false,
                    tool: spotBugs(pattern: '**/target/spotbugsXml.xml')
        }
    }
}

def executeMavenSpotBugsForConfiguredModules(script, filterOptions, Map configuration, String basePath = './') {
    if (configuration.scanModules && !BuildToolEnvironment.instance.isMta()) {
        for (int i = 0; i < configuration.scanModules.size(); i++) {
            def scanModule = configuration.scanModules[i]
            executeMavenSpotBugs(script, filterOptions, configuration, "$basePath/$scanModule/pom.xml")
        }
    } else {
        executeMavenSpotBugs(script, filterOptions, configuration, BuildToolEnvironment.instance.getApplicationPomXmlPath(basePath))
    }
}

def executeMavenSpotBugs(script, filterOptions, Map configuration, String pomPath) {
    mavenExecute(
        script: script,
        flags: '--batch-mode',
        pomPath: pomPath,
        m2Path: s4SdkGlobals.m2Directory,
        goals: 'com.github.spotbugs:spotbugs-maven-plugin:3.1.9:spotbugs',
        defines: filterOptions,
        dockerImage: configuration.dockerImage
    )
}
