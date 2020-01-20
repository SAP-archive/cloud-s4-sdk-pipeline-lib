import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call() {
    //Collect all coverage reports from backend and frontend in the cobertura format
    List coverageReports = findFiles(glob: "${s4SdkGlobals.coverageReports}/*.xml")
    if (coverageReports.size() > 0) {
        assertPluginIsActive("cobertura")
        cobertura(autoUpdateHealth: false, autoUpdateStability: false,
            coberturaReportFile: "${s4SdkGlobals.coverageReports}/*.xml",
            failNoReports: false, failUnstable: false,
            maxNumberOfBuilds: 0, onlyStable: false, zoomCoverageChart: false)
    }
}
