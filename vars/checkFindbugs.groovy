import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkFindbugs', stepParameters: parameters) {
        assertPluginIsActive('warnings-ng')

        Script script = parameters.script

        Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'checkFindbugs')

        Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'checkFindbugs')

        Set parameterKeys = [
            'scanModules',
            'dockerImage',
            'excludeFilterFile'
        ]

        Set stepConfigurationKeys = parameterKeys

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        List filterOptions = []

        String excludeFilterFile = configuration.excludeFilterFile
        if (excludeFilterFile?.trim() && fileExists(excludeFilterFile)) {
            filterOptions.add("-Dspotbugs.excludeFilterFile=${excludeFilterFile}")
        }

        String includeFilterFile = configuration.includeFilterFile
        String localIncludeFilerPath = "s4hana_pipeline/${includeFilterFile}"
        writeFile file: localIncludeFilerPath, text: libraryResource(includeFilterFile)
        filterOptions.add("-Dspotbugs.includeFilterFile=${localIncludeFilerPath}")

        filterOptions.addAll(MavenUtils.getTestModulesExcludeFlags(script))

        mavenExecute(
            script: script,
            flags: '--batch-mode',
            m2Path: s4SdkGlobals.m2Directory,
            goals: 'com.github.spotbugs:spotbugs-maven-plugin:3.1.9:spotbugs',
            defines: filterOptions.join(' '),
            dockerImage: configuration.dockerImage
        )

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
