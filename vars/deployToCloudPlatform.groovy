import com.sap.cloud.sdk.s4hana.pipeline.CloudPlatform
import com.sap.cloud.sdk.s4hana.pipeline.DeploymentType
import com.sap.piper.k8s.ContainerMap

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'deployToCloudPlatform', stepParameters: parameters) {
        def index = 1
        def deployments = [:]
        def stageName = parameters.get('stage')
        def script = parameters.get('script')

        if (parameters.cfTargets) {
            for (int i = 0; i < parameters.cfTargets.size(); i++) {
                def target = parameters.cfTargets[i]
                Closure deployment = {
                    unstashFiles script: script, stage: stageName
                    String deploymentType = DeploymentType.selectFor(
                        CloudPlatform.CLOUD_FOUNDRY,
                        parameters.isProduction.asBoolean()
                    ).toString()

                    def deployTool =
                        (script.commonPipelineEnvironment.configuration.isMta) ? 'mtaDeployPlugin' : 'cf_native'

                    cloudFoundryDeploy(
                        script: parameters.script,
                        deployType: deploymentType,
                        cloudFoundry: target,
                        mtaPath: script.commonPipelineEnvironment.mtarFilePath,
                        deployTool: deployTool
                    )

                    stashFiles script: script, stage: stageName
                }
                deployments["Deployment ${index > 1 ? index : ''}"] = {
                    if (env.POD_NAME) {
                        dockerExecuteOnKubernetes(script: script, containerMap: ContainerMap.instance.getMap().get(stageName) ?: [:]) {
                            deployment.run()
                        }
                    } else {
                        node(env.NODE_NAME) {
                            deployment.run()
                        }
                    }
                }
                index++
            }
            runClosures deployments, script
        } else if (parameters.neoTargets) {

            def pom = readMavenPom file: 'application/pom.xml'
            def source = "application/target/${pom.getArtifactId()}.${pom.getPackaging()}"
            for (int i = 0; i < parameters.neoTargets.size(); i++) {
                def target = parameters.neoTargets[i]
                Closure deployment = {
                    unstashFiles script: script, stage: stageName
                    deployToNeoWithCli(
                        script: parameters.script,
                        target: target,
                        deploymentType: DeploymentType.selectFor(CloudPlatform.NEO, parameters.isProduction.asBoolean()),
                        source: source
                    )
                    stashFiles script: script, stage: stageName
                }
                deployments["Deployment ${index > 1 ? index : ''}"] = {
                    if (env.POD_NAME) {
                        dockerExecuteOnKubernetes(script: script, containerMap: ContainerMap.instance.getMap().get(stageName) ?: [:]) {
                            deployment.run()
                        }
                    } else {
                        node(env.NODE_NAME) {
                            deployment.run()
                        }
                    }
                }
                index++
            }
            runClosures deployments, script
        } else {
            currentBuild.result = 'FAILURE'
            error("Test Deployment skipped because no targets defined!")
        }
    }
}
