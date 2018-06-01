import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkPmd', stepParameters: parameters) {
        def script = parameters.script

        final def stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'checkPmd')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'checkPmd')

        Set parameterKeys = [
            'scanModules',
            'dockerImage',
            'excludes'
        ]

        Set stepConfigurationKeys = parameterKeys

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        def excludeOption
        def excludes = configuration.excludes

        if (excludes == null) {
            excludeOption = ''
        } else if (excludes.class == List) {
            excludeOption = "-Dpmd.excludes=${excludes.join(',')}"
        } else {
            excludeOption = "-Dpmd.excludes=${excludes}"
        }

        def ruleSetsOption = "-Dpmd.rulesets=rulesets/s4hana-qualities.xml"

        def options = "$excludeOption $ruleSetsOption"

        executeMavenPMDForConfiguredModules(script, options, configuration)

        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'PMD', errorMessage: "Please examine the PMD reports.") {
            pmd(failedTotalHigh: '0', failedTotalNormal: '10', pattern: '**/target/pmd.xml')
        }
    }
}

def executeMavenPMDForConfiguredModules(script, options, Map configuration) {
    if (configuration.scanModules) {
        for (int i = 0; i < configuration.scanModules.size(); i++) {
            def scanModule = configuration.scanModules[i]
            executeMavenPMD(script, options, configuration, "$scanModule/pom.xml")
        }
    } else {
        executeMavenPMD(script, options, configuration, "pom.xml")
    }
}

def executeMavenPMD(script, options, Map configuration, String pomPath) {
    mavenExecute(
        script: script,
        flags: '--update-snapshots --batch-mode',
        pomPath: pomPath,
        m2Path: s4SdkGlobals.m2Directory,
        goals: "com.sap.cloud.s4hana.quality:pmd-plugin:RELEASE:pmd",
        defines: options,
        dockerImage: configuration.dockerImage
    )
}
