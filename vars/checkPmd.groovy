import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkPmd', stepParameters: parameters) {
        assertPluginIsActive('warnings-ng')

        def script = parameters.script
        String basePath = parameters.basePath

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
        } else if (excludes instanceof List) {
            excludeOption = "-Dpmd.excludes=${excludes.join(',')}"
        } else {
            excludeOption = "-Dpmd.excludes=${excludes}"
        }

        def ruleSetsOption = "-Dpmd.rulesets=rulesets/s4hana-qualities.xml"

        def options = "$excludeOption $ruleSetsOption"

        executeMavenPMDForConfiguredModules(script, options, configuration, basePath)

        executeWithLockedCurrentBuildResult(
            script: script,
            errorStatus: 'FAILURE',
            errorHandler: script.buildFailureReason.setFailureReason,
            errorHandlerParameter: 'PMD',
            errorMessage: "Please examine the PMD reports. For more information, please visit https://blogs.sap.com/2017/09/20/static-code-checks/"
        )
            {
            recordIssues failedTotalHigh: 1,
                    failedTotalNormal: 10,
                    blameDisabled: true,
                    enabledForFailure: true,
                    aggregatingResults: false,
                    tool: pmdParser(pattern: '**/target/pmd.xml')

        }
    }
}

def executeMavenPMDForConfiguredModules(script, options, Map configuration, String basePath = './') {
    basePath = basePath ?: './'
    if (configuration.scanModules && !BuildToolEnvironment.instance.isMta()) {
        for (int i = 0; i < configuration.scanModules.size(); i++) {
            def scanModule = configuration.scanModules[i]
            executeMavenPMD(script, options, configuration, "$basePath/$scanModule/pom.xml")
        }
    } else {
        executeMavenPMD(script, options, configuration, BuildToolEnvironment.instance.getApplicationPomXmlPath(basePath))
    }
}

def executeMavenPMD(script, options, Map configuration, String pomPath) {
    mavenExecute(
        script: script,
        flags: '--batch-mode',
        pomPath: pomPath,
        m2Path: s4SdkGlobals.m2Directory,
        goals: "com.sap.cloud.sdk.quality:pmd-plugin:RELEASE:pmd",
        defines: options,
        dockerImage: configuration.dockerImage
    )
}
