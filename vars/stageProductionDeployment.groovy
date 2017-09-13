import com.sap.icd.jenkins.ConfigurationHelper
import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    def script = parameters.script

    unstashFiles script: script, stage: 'deploy'

    Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'productionDeployment')
    Map generalConfiguration = ConfigurationLoader.generalConfiguration(script)

    def productiveBranch = new ConfigurationHelper(generalConfiguration).getConfigProperty('productiveBranch', 'master')

    if (env.BRANCH_NAME == productiveBranch) {
        if (stageConfiguration.cfTargets) {
            deployToCfWithCli script: script, targets: stageConfiguration.cfTargets, deploymentType: 'blue-green'
        } else if (stageConfiguration.neoTargets) {

            def pom = readMavenPom file: 'application/pom.xml'
            def source = "application/target/${pom.getArtifactId()}.${pom.getPackaging()}"

            deployToNeoWithCli script: script, targets: stageConfiguration.neoTargets, deploymentType: 'rolling-update', source: source
        } else {
            echo "Deployment skipped because no targets defined!"
        }
    } else {
        echo "Deployment skipped because current branch ${env.BRANCH_NAME} is not productive!"
    }
    stashFiles script: script, stage: 'deploy'
}
