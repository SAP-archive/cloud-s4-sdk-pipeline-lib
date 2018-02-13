import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkPmd', stepParameters: parameters) {
        def script = parameters.script

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'checkPmd')

        List parameterKeys = ['dockerImage', 'excludes']
        List stepConfigurationKeys = parameterKeys
        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys)

        def excludeOption
        def excludes = configuration.excludes

        if (excludes == null) {
            excludeOption = ''
        } else if (excludes.class == List) {
            excludeOption = "-Dpmd.excludes=${excludes.join(',')}"
        } else {
            excludeOption = "-Dpmd.excludes=${excludes}"
        }

        try {
            executeMaven (
                    script: script,
                    flags: '-U -B',
                    m2Path: s4SdkGlobals.m2Directory,
                    goals: "com.sap.cloud.s4hana.quality:pmd-plugin:RELEASE:pmd ${excludeOption}",
                    defines: '-Dpmd.rulesets=rulesets/s4hana-qualities.xml',
                    dockerImage: configuration.dockerImage
                    )
        }
        catch (Exception ex){
            echo ex.getMessage()
        }

        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'PMD', errorMessage: "Please examine the PMD reports.") {
            pmd(failedTotalHigh: '0', failedTotalNormal: '10', pattern: '**/target/pmd.xml')
        }
    }
}
