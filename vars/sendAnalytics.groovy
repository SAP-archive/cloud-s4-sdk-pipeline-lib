import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.piper.ConfigurationLoader
import hudson.model.Result

def call(Map parameters = [:]) {
    try {
        def script = parameters.script
        if (Analytics.instance.analyticsEnabled) {
            def telemetryData = parameters.telemetryData
            if (!telemetryData) {
                telemetryData = [:]
                telemetryData << Analytics.instance.getSystemInfo()
                telemetryData << Analytics.instance.getJobConfiguration()

                telemetryData.event_type = 'pipeline'
                telemetryData.custom3 = 'stage_name'
                telemetryData.e_3 = 'postAction'

                telemetryData.custom4 = 'pipeline_result'
                telemetryData.e_4 = Result.fromString(currentBuild.currentResult)

                telemetryData.custom5 = 'start_time'
                telemetryData.e_5 = currentBuild.startTimeInMillis

                telemetryData.custom6 = 'pipeline_duration'
                telemetryData.e_6 = currentBuild.duration
            }
            telemetryData << Analytics.instance.getTelemetryData()

            Map generalConfiguration = ConfigurationLoader.generalConfiguration(script)
            if (generalConfiguration.idsite) {
                telemetryData.idsite = generalConfiguration.idsite
            }

            def options = []
            options.push("--get")
            options.push("--verbose \"${telemetryData.swaUrl}\"")
            telemetryData.remove('swaUrl')
            telemetryData.each { configName, configValue -> options.push("--data-urlencode \"${configName}=${configValue}\"") }
            options.push("--connect-timeout 5")
            options.push("--max-time 20")

            echo "Sending telemetry data: ${telemetryData}"

            sh(returnStatus: true, script: "#!/bin/sh +x\ncurl ${options.join(' ')} > /dev/null 2>&1 ")
        } else {
            echo "Sending telemetry data is disabled."
        }
    } catch (ignore) {
    }
}
