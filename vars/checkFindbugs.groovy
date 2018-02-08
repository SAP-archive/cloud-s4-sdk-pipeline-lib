import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkFindbugs', stepParameters: parameters) {
        def script = parameters.script

        final def stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'checkFindbugs')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'checkFindbugs')

        List parameterKeys = [
            'dockerImage',
            'excludeFilterFile'
        ]

        List stepConfigurationKeys = [
            'dockerImage',
            'excludeFilterFile'
        ]

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

        executeMaven script: script, flags: '-B -U', m2Path: s4SdkGlobals.m2Directory, goals: 'findbugs:findbugs', defines: filterOptions, dockerImage: configuration.dockerImage

        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Findbugs', errorMessage: "Please examine the Findbugs reports.") {
            findbugs canComputeNew: false, excludePattern: excludeFilterFile, failedTotalHigh: '0', failedTotalNormal: '10', pattern: '**/findbugsXml.xml'
        }
    }
}
