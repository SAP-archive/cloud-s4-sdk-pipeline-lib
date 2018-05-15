import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'checkJMeter', stepParameters: parameters) {
        def script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'checkJMeter')
        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'checkJMeter')

        Set stepConfigurationKeys = [
            'options',
            'testPlan',
            'dockerImage',
            'failThreshold',
            'unstableThreshold'
        ]

        Set parameterKeys = [
            'testPlan',
            'reportDirectory'
        ]

        def configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)
        def defaultOptions = "-n -t ${configuration.testPlan} -l JMeter-report.jtl -e -o ${configuration.reportDirectory}"
        def command = "jmeter ${configuration.options?.trim() ?: ''} ${defaultOptions}"

        dockerExecute(dockerImage: configuration.dockerImage) { sh command }

        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Check JMeter', errorMessage: "Please examine Performance Test results.") {
            performanceReport(parsers: [[$class: 'JMeterParser', glob: "JMeter-report.jtl"]],
                errorFailedThreshold: configuration.failThreshold,
                errorUnstableThreshold: configuration.unstableThreshold,
                ignoreUnstableBuild: false,
                ignoreFailedBuild: false)

            step([$class: 'ArtifactArchiver', artifacts: "**/*.jtl,${configuration.reportDirectory}", fingerprint: true])
        }
    }
}
