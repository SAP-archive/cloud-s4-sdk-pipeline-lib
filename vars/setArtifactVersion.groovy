import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator


def call(Map parameters) {
    def script = parameters.script

    if (!isProductiveBranch(script: script)) {
        echo "Automatic versioning not performed, because this is not the productive branch"
        return
    }

    String buildTool
    if (BuildToolEnvironment.instance.isMta()) {
        buildTool = 'mta'
    } else if (BuildToolEnvironment.instance.isMaven()) {
        buildTool = 'maven'
    } else if (BuildToolEnvironment.instance.isNpm()) {
        buildTool = 'npm'
    } else {
        echo "Automatic versioning is not supported for this project's build tool"
        return
    }

    artifactPrepareVersion script: script, buildTool: buildTool
    ReportAggregator.instance.reportAutomaticVersioning()
}
