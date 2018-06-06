import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkFindbugs', stepParameters: parameters) {
        def script = parameters.script

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
        def localIncludeFilerPath = "../s4hana_pipeline/${includeFilterFile}"
        writeFile file: localIncludeFilerPath, text: libraryResource(includeFilterFile)
        filterOptions += "-Dspotbugs.includeFilterFile=${localIncludeFilerPath}"

        executeMavenSpotBugsForConfiguredModules(script, filterOptions, configuration)

        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Findbugs', errorMessage: "Please examine the FindBugs/SpotBugs reports.") {
            findbugs canComputeNew: false, excludePattern: excludeFilterFile, failedTotalHigh: '0', failedTotalNormal: '10', pattern: '**/target/spotbugsXml.xml'
        }
    }
}

def executeMavenSpotBugsForConfiguredModules(script, filterOptions, Map configuration) {
    if (configuration.scanModules) {
        for (int i = 0; i < configuration.scanModules.size(); i++) {
            def scanModule = configuration.scanModules[i]
            executeMavenSpotBugs(script, filterOptions, configuration, "$scanModule/pom.xml")
        }
    } else {
        executeMavenSpotBugs(script, filterOptions, configuration, "pom.xml")
    }
}

def executeMavenSpotBugs(script, filterOptions, Map configuration, String pomPath) {
    mavenExecute(
        script: script,
        flags: '--update-snapshots --batch-mode',
        pomPath: pomPath,
        m2Path: s4SdkGlobals.m2Directory,
        goals: 'com.github.spotbugs:spotbugs-maven-plugin:3.1.3.1:spotbugs',
        defines: filterOptions,
        dockerImage: configuration.dockerImage
    )
}
