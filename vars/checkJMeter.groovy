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

        if (!configuration.testPlan) {
            error "JMeter is enabled, but parameter testPlan (path to JMeter tests) is not set. \n" +
                "Please set testPlan in checkJMeter step of your project's .pipeline/config.yml or disable JMeter."
        }

        String defaultOptions = "-n -t ${configuration.testPlan} -l JMeter-report.jtl -e -o ${configuration.reportDirectory}"
        String command = "jmeter ${configuration.options?.trim() ?: ''} ${defaultOptions}"

        dockerExecute(script: script, dockerImage: configuration.dockerImage) {
            sh "mkdir -p ${configuration.reportDirectory}"
            sh command
        }

        performanceReport(parsers: [[$class: 'JMeterParser', glob: "JMeter-report.jtl"]],
            errorFailedThreshold: configuration.failThreshold,
            errorUnstableThreshold: configuration.unstableThreshold,
            ignoreUnstableBuild: false,
            ignoreFailedBuild: false)

        step([$class: 'ArtifactArchiver', artifacts: "**/*.jtl,${configuration.reportDirectory}", fingerprint: true])
    }
}
