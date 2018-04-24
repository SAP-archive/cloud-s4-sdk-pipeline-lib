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
            filterOptions += "-Dfindbugs.excludeFilterFile=${excludeFilterFile} "
        }

        def includeFilterFile = configuration.includeFilterFile
        def localIncludeFilerPath = "../s4hana_pipeline/${includeFilterFile}"
        writeFile file: localIncludeFilerPath, text: libraryResource(includeFilterFile)
        filterOptions += "-Dfindbugs.includeFilterFile=${localIncludeFilerPath}"

        executeMavenFindbugsForConfiguredModules(script, filterOptions, configuration)

        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Findbugs', errorMessage: "Please examine the Findbugs reports.") {
            findbugs canComputeNew: false, excludePattern: excludeFilterFile, failedTotalHigh: '0', failedTotalNormal: '10', pattern: '**/target/findbugsXml.xml'
        }
    }
}

def executeMavenFindbugsForConfiguredModules(script, filterOptions, Map configuration) {
    if (configuration.scanModules) {
        for (int i = 0; i < configuration.scanModules.size(); i++) {
            def scanModule = configuration.scanModules[i]
            executeMavenFindbugs(script, filterOptions, configuration, "$scanModule/pom.xml")
        }
    } else {
        executeMavenFindbugs(script, filterOptions, configuration, "pom.xml")
    }
}

def executeMavenFindbugs(script, filterOptions, Map configuration, String pomPath) {
    mavenExecute(
        script: script,
        flags: '-B -U',
        pomPath: pomPath,
        m2Path: s4SdkGlobals.m2Directory,
        goals: 'findbugs:findbugs',
        defines: filterOptions,
        dockerImage: configuration.dockerImage
    )
}
