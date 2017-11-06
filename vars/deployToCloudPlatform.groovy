import com.sap.icd.jenkins.CloudPlatform
import com.sap.icd.jenkins.DeploymentType

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'deployToCloudPlatform', stepParameters: parameters) {
        if (parameters.cfTargets) {
            deployToCfWithCli script: parameters.script, targets: parameters.cfTargets, deploymentType: parameters.isProduction ? DeploymentType.getDepolymentTypeForProduction(CloudPlatform.CLOUD_FOUNDRY) : DeploymentType.STANDARD
        } else if (parameters.neoTargets) {

            def pom = readMavenPom file: 'application/pom.xml'
            def source = "application/target/${pom.getArtifactId()}.${pom.getPackaging()}"

            deployToNeoWithCli script: parameters.script, targets: parameters.neoTargets, deploymentType: parameters.isProduction ? DeploymentType.getDepolymentTypeForProduction(CloudPlatform.NEO) : DeploymentType.STANDARD, source: source
        } else {
            currentBuild.result = 'FAILURE'
            error("Test Deployment skipped because no targets defined!")
        }
    }
}