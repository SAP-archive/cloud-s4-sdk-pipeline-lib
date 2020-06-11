import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator


def call(Map parameters) {
    def script = parameters.script

    //TODO activate automatic versioning for JS
    Map configWithDefault = loadEffectiveGeneralConfiguration script: script
    if (!BuildToolEnvironment.instance.isNpm() && isProductiveBranch(script: script) && configWithDefault.automaticVersioning) {
        boolean isMtaProject = BuildToolEnvironment.instance.isMta()
        artifactSetVersion script: script, buildTool: isMtaProject ? 'mta' : 'maven', filePath: isMtaProject ? 'mta.yaml' : 'pom.xml'
        ReportAggregator.instance.reportAutomaticVersioning()
    }
}
