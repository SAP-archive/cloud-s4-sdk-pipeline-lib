import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.piper.analytics.Telemetry
import hudson.model.Result

def call(Map parameters) {
    def script = parameters.script

    Analytics.instance.init(script)

    if (env.JOB_URL) {
        Analytics.instance.hashBuildUrl(env.JOB_URL)
    } else {
        Analytics.instance.hashBuildUrl(env.JOB_NAME)
    }
    Analytics.instance.buildNumber(env.BUILD_NUMBER)

    Telemetry.registerListener({ steps, payload ->
        if (payload.eventType == 'library-os-stage'){
            def stageInfo = [:]
            stageInfo.event_type = 'pipeline_stage'
            stageInfo.custom3 = 'stage_name'
            stageInfo.e_3 = payload.stageName

            stageInfo.custom4 = 'stage_result'
            stageInfo.e_4 = Result.fromString(payload.buildResult)

            stageInfo.custom5 = 'start_time'
            stageInfo.e_5 = payload.stageStartTime

            stageInfo.custom6 = 'duration'
            stageInfo.e_6 = payload.stageDuration

            stageInfo.custom7 = 'project_extensions'
            stageInfo.e_7 = payload.projectExtension

            stageInfo.custom8 = 'global_extensions'
            stageInfo.e_8 = payload.globalExtension

            sendAnalytics(script: script, telemetryData: stageInfo)
        }
    })

}
