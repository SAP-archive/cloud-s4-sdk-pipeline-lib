import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 's4SdkQualityChecks'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        executeQualityChecks(script, stageConfiguration)
    }
}

private void executeQualityChecks(def script, Map configuration) {

    if (BuildToolEnvironment.instance.isMaven() || BuildToolEnvironment.instance.isMta()) {
        checkDeploymentDescriptors script: script

        aggregateListenerLogs script: script

        checkResilience script: script
        ReportAggregator.instance.reportResilienceCheck()

        //Fixme: Disabled API checks as it does not work with sdk v3
        //checkServices script: script, nonErpDestinations: configuration.nonErpDestinations, customODataServices: configuration.customODataServices
        //ReportAggregator.instance.reportServicesCheck(configuration.nonErpDestinations, configuration.customODataServices)
    }

    checkCodeCoverage(
        script: script,
        jacocoExcludes: configuration.jacocoExcludes,
        threshold: configuration.threshold,
        codeCoverageFrontend: configuration.codeCoverageFrontend
    )
}
