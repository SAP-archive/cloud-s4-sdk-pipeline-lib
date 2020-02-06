import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.CloudPlatform
import com.sap.cloud.sdk.s4hana.pipeline.DeploymentType
import com.sap.piper.Utils
import com.sap.piper.k8s.ContainerMap

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'deployToCloudPlatform', stepParameters: parameters) {
        def index = 1
        def deployments = [:]
        def stageName = parameters.stage
        def script = parameters.script
        def enableZeroDowntimeDeployment = parameters.enableZeroDowntimeDeployment

        if (parameters.cfTargets) {

            String appName = parameters.cfTargets.appName.toString()
            boolean isValidCfAppName = appName.matches("^[a-zA-Z0-9]*\$")

            if (appName.contains("_")) {
                error("Your application name contains non-alphanumeric character i.e 'underscore'. Please rename $appName that it does not contain any non-alphanumeric characters, as they are not supported by CloudFoundry.. \n" +
                    "For more details please visit https://docs.cloudfoundry.org/devguide/deploy-apps/deploy-app.html#basic-settings")
            } else if (!isValidCfAppName) {
                echo "Your application name contains non-alphanumeric characters that may lead to errors in the future, as they are not supported by CloudFoundry. \n" +
                    "For more details please visit https://docs.cloudfoundry.org/devguide/deploy-apps/deploy-app.html#basic-settings"

                addBadge(icon: "warning.gif", text: "Your application name contains non-alphanumeric characters that may lead to errors in the future, as they are not supported by CloudFoundry. \n" +
                    "For more details please visit https://docs.cloudfoundry.org/devguide/deploy-apps/deploy-app.html#basic-settings")
            }

            for (int i = 0; i < parameters.cfTargets.size(); i++) {
                def target = parameters.cfTargets[i]
                Closure deployment = {
                    Utils utils = new Utils()
                    utils.unstashStageFiles(script, stageName)

                    String deploymentType
                    if (enableZeroDowntimeDeployment) {
                        deploymentType = DeploymentType.CF_BLUE_GREEN.toString()
                    } else {
                        deploymentType = DeploymentType.selectFor(
                            CloudPlatform.CLOUD_FOUNDRY,
                            parameters.isProduction.asBoolean()
                        ).toString()
                    }

                    Map cloudFoundryDeploymentParameters = [script      : parameters.script,
                                                            deployType  : deploymentType,
                                                            cloudFoundry: target,
                                                            mtaPath     : script.commonPipelineEnvironment.mtarFilePath]

                    if (BuildToolEnvironment.instance.isMta()) {
                        cloudFoundryDeploymentParameters.deployTool = 'mtaDeployPlugin'
                        if (target.mtaExtensionDescriptor) {
                            if (!fileExists(target.mtaExtensionDescriptor)) {
                                error "The mta descriptor has defined an extension file ${target.mtaExtensionDescriptor}. But the file is not available."
                            }
                            cloudFoundryDeploymentParameters.mtaExtensionDescriptor = target.mtaExtensionDescriptor
                            if (target.mtaExtensionCredentials) {
                                echo "Modifying ${cloudFoundryDeploymentParameters.mtaExtensionDescriptor}. Adding credential values from Jenkins."
                                sh "cp ${cloudFoundryDeploymentParameters.mtaExtensionDescriptor} ${cloudFoundryDeploymentParameters.mtaExtensionDescriptor}.original"

                                Map matExtensionCredentials = target.mtaExtensionCredentials

                                String fileContent = ''
                                Map binding = [:]

                                try {
                                    fileContent = readFile target.mtaExtensionDescriptor
                                } catch (Exception e) {
                                    error("Unable to read mta extension file ${target.mtaExtensionDescriptor}. If this should not happen, please open an issue at https://github.com/sap/cloud-s4-sdk-pipeline/issues and describe your project setup.")
                                }

                                matExtensionCredentials.each { key, credentialsId ->
                                    withCredentials([string(credentialsId: credentialsId, variable: 'mtaExtensionCredential')]) {
                                        fileContent = fileContent.replaceFirst('<%= ' + key.toString() + ' %>', mtaExtensionCredential.toString())
                                    }
                                }

                                try {
                                    writeFile file: target.mtaExtensionDescriptor, text: fileContent
                                } catch (Exception e) {
                                    error("Unable to write credentials values to the mta extension file ${target.mtaExtensionDescriptor}\n. \n Please refer to the manual at https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#productiondeployment. \nIf this should not happen, please open an issue at https://github.com/sap/cloud-s4-sdk-pipeline/issues and describe your project setup.")
                                }
                            }
                        }
                    } else {
                        cloudFoundryDeploymentParameters.deployTool = 'cf_native'
                    }
                    try {
                        cloudFoundryDeploy(cloudFoundryDeploymentParameters)
                    } finally {
                        if (target.mtaExtensionCredentials && cloudFoundryDeploymentParameters.mtaExtensionDescriptor && fileExists(cloudFoundryDeploymentParameters.mtaExtensionDescriptor)) {
                            sh "mv --force ${cloudFoundryDeploymentParameters.mtaExtensionDescriptor}.original ${cloudFoundryDeploymentParameters.mtaExtensionDescriptor} || echo 'The file ${cloudFoundryDeploymentParameters.mtaExtensionDescriptor}.original couldnot be renamed. \n" + " Kindly refer to the manual at https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#productiondeployment. \nIf this should not happen, please create an issue at https://github.com/SAP/cloud-s4-sdk-pipeline/issues'"
                        }
                    }

                    utils.stashStageFiles(script, stageName)
                }
                deployments["Deployment ${index > 1 ? index : ''}"] = {
                    if (env.POD_NAME) {
                        dockerExecuteOnKubernetes(script: script, containerMap: ContainerMap.instance.getMap().get(stageName) ?: [:]) {
                            deployment.call()
                        }
                    } else {
                        node(env.NODE_NAME) {
                            deployment.call()
                        }
                    }
                }
                index++
            }
            runClosures deployments, script
        } else if (parameters.neoTargets) {

            if (BuildToolEnvironment.instance.isMta()) {
                error("MTA projects can be deployed only to the Cloud Foundry environment.")
            }

            def pom = readMavenPom file: 'application/pom.xml'
            def source = "application/target/${pom.getArtifactId()}.${pom.getPackaging()}"
            for (int i = 0; i < parameters.neoTargets.size(); i++) {
                def target = parameters.neoTargets[i]

                Closure deployment = {
                    Utils utils = new Utils()
                    utils.unstashStageFiles(script, stageName)

                    DeploymentType deploymentType
                    if (enableZeroDowntimeDeployment) {
                        deploymentType = DeploymentType.NEO_ROLLING_UPDATE
                    } else {
                        deploymentType = DeploymentType.selectFor(CloudPlatform.NEO, parameters.isProduction.asBoolean())
                    }

                    neoDeploy(
                        script: parameters.script,
                        warAction: deploymentType.toString(),
                        source: source,
                        neo: target
                    )

                    utils.stashStageFiles(script, stageName)
                }
                deployments["Deployment ${index > 1 ? index : ''}"] = {
                    if (env.POD_NAME) {
                        dockerExecuteOnKubernetes(script: script, containerMap: ContainerMap.instance.getMap().get(stageName) ?: [:]) {
                            deployment.call()
                        }
                    } else {
                        node(env.NODE_NAME) {
                            deployment.call()
                        }
                    }
                }
                index++
            }
            runClosures deployments, script
        } else {
            currentBuild.result = 'FAILURE'
            error("Deployment skipped because no targets defined!")
            if (stageName == "productionDeployment") {
                echo "For more information, please refer to https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#productiondeployment"
            } else if (stageName == "performanceTests") {
                echo "For more information, please refer to https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#performancetests"
            } else {
                echo "For more information, please refer to https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#endtoendtests"
            }
        }
    }
}
