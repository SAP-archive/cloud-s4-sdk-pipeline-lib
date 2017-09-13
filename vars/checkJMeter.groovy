import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'checkJMeter', stepParameters: parameters) {
        def script = parameters.script
        Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'checkJMeter')
        def stepConfigurationKeys = [
            'options',
            'dockerImage',
            'failThreshold',
            'unstableThreshold'
        ]
        def parameterKeys = [
            'testPlan',
            'reportDirectory'
        ]
        final Map stepDefaults = [dockerImage      : 'famiko/jmeter-base',
            testPlan         : '*',
            reportDirectory  : '/jmeter-report',
            failThreshold    : 100,
            unstableThreshold: 90]
        def configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)
        def defaultOptions = "-n -t ${configuration.testPlan} -l JMeter-report.jtl -e -o ${configuration.reportDirectory}"
        def command = "jmeter ${configuration.options?.trim() ?: ''} ${defaultOptions}"

        executeDockerNative(dockerImage: configuration.dockerImage) { sh command }
        performanceReport(parsers: [
            [$class: 'JMeterParser', glob: "JMeter-report.jtl"]
        ],
        errorFailedThreshold: configuration.failThreshold,
        errorUnstableThreshold: configuration.unstableThreshold,
        ignoreUnstableBuild: false,
        ignoreFailedBuild: false)

        step([$class: 'ArtifactArchiver', artifacts: "**/*.jtl,${configuration.reportDirectory}", fingerprint: true])
    }
}
