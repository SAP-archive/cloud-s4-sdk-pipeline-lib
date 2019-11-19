import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {

    def stageName = 'sonarQubeScan'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {

        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
        def pathList = []
        runOverModules(script: script, moduleType: "java") { String basePath ->
            String path = BuildToolEnvironment.instance.getApplicationPath(basePath)
            pathList.add(path + '/target/classes')
        }
        executeSonarQubeChecks(script, pathList, stageConfiguration)
    }
}

private void executeSonarQubeChecks(def script, List pathList, Map configuration) {

    String path = pathList.join(',')
    String coverageExclusion = '**.js,unit-tests/**,integration-tests/**,performance-tests/**,**.xml,**/target/**'
    List sonarProperties = ['sonar.java.binaries=' + path, 'sonar.coverage.exclusions=' + coverageExclusion]
    String projectKey = 'sonar.projectKey=unnamed'

    if (configuration?.projectKey != null && !configuration?.projectKey.toString().isEmpty()) {
        projectKey = "sonar.projectKey=${configuration.projectKey}"
    } else {
        echo "Please provide projectKey in configuration for SonarQube"
    }
    if (configuration?.sonarProperties != null) {
        sonarProperties += configuration.sonarProperties
    }
    sonarProperties.add(projectKey)
    sonarExecuteScan([
        script     : script,
        dockerImage: 'ppiper/node-browsers',
        instance   : configuration.instance,
        options    : sonarProperties
    ])


}
