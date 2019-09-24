import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator

def call(Map parameters) {
    def script = parameters.script

    Map scmCheckoutResult = checkout(parameters.checkoutMap ?: scm)

    initS4SdkPipelineLibrary script: script

    if (scmCheckoutResult.GIT_COMMIT) {
        ReportAggregator.instance.reportVersionControlUsed('Git')
    }

    if (scmCheckoutResult.GIT_URL) {
        script.commonPipelineEnvironment.configuration.general.gitUrl = scmCheckoutResult.GIT_URL
    }

    Analytics.instance.initAnalytics(script)

    loadGlobalExtension script: script
    convertLegacyExtensions(script: script)

    if (Boolean.valueOf(env.ON_K8S)) {
        initContainersMap script: script
    }

    stash allowEmpty: true, excludes: '', includes: '**/*', name: 'scm'
}
