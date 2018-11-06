import com.sap.cloud.sdk.s4hana.pipeline.DeploymentDescriptorType
import com.sap.piper.ConfigurationLoader
import com.sap.piper.JenkinsUtils

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkDeploymentDescriptors', stepParameters: parameters) {

        def script = parameters.script
        Map projectGeneralConfiguration = ConfigurationLoader.generalConfiguration(script)
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, "productionDeployment")

        boolean unsafeMode = projectGeneralConfiguration.unsafeMode

        if (isProductiveBranch(script: script) && (!unsafeMode)) {

            if (script.commonPipelineEnvironment.configuration.isMta) {
                def mta = readYaml file: 'mta.yaml'
                def cloudTargets = extractDeploymentdescriptorEnvironment(mta, DeploymentDescriptorType.MTA)
                verifyEnvVars(cloudTargets, DeploymentDescriptorType.MTA)
            }
            def neoTargets = stageConfiguration.neoTargets
            if (neoTargets) {
                def cloudTargets = extractDeploymentdescriptorEnvironment(neoTargets, DeploymentDescriptorType.NEOTARGETS)
                verifyEnvVars(cloudTargets, DeploymentDescriptorType.NEOTARGETS)
            }

            def cfTargets = stageConfiguration.cfTargets
            if (cfTargets) {
                for (int i = 0; i < cfTargets.size(); i++) {
                    if (cfTargets[i].manifest) {
                        def manifest = readYaml file: cfTargets[i].manifest
                        def cloudTargets = extractDeploymentdescriptorEnvironment(manifest, DeploymentDescriptorType.MANIFEST)
                        verifyEnvVars(cloudTargets, DeploymentDescriptorType.MANIFEST)
                    }
                }
            }
        }
    }
}

private checkForBadgePlugin() {
    final String PLUGIN_ID_BAGDE = 'badge'
    if (!JenkinsUtils.isPluginActive(PLUGIN_ID_BAGDE)) {
        String exception = """[ERROR] Plugin '${PLUGIN_ID_BAGDE}' is not installed or not active.
Please update the Jenkins image to the latest available version. 
For more information how to update the image please visit:
https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/doc/operations/operations-guide.md#update-image
"""
        throw new RuntimeException(exception)
    }
}

private def extractDeploymentdescriptorEnvironment(def deploymentDescriptor, DeploymentDescriptorType deploymentDescriptorType) {
    def extractedEnvironmentVariables = []
    if (deploymentDescriptor) {
        switch (deploymentDescriptorType) {
            case DeploymentDescriptorType.MANIFEST:
                for (int i = 0; i < deploymentDescriptor.applications.size(); i++) {
                    extractedEnvironmentVariables.addAll(deploymentDescriptor.applications[i].env?.keySet())
                }
                return extractedEnvironmentVariables
            case DeploymentDescriptorType.MTA:
                for (int i = 0; i < deploymentDescriptor.modules.size(); i++) {
                    extractedEnvironmentVariables.addAll(deploymentDescriptor.modules[i].properties?.keySet())
                }
                return extractedEnvironmentVariables
            case DeploymentDescriptorType.NEOTARGETS:
                for (int i = 0; i < deploymentDescriptor.size(); i++) {
                    extractedEnvironmentVariables.addAll(deploymentDescriptor[i].environment?.keySet())
                }
                return extractedEnvironmentVariables
            default:
                break
        }
    }
}

private void verifyEnvVars(List configuredEnvVars, DeploymentDescriptorType deploymentDescriptorType) {
    
    final List prohibitedEnvVars = ['ALLOW_MOCKED_AUTH_HEADER', 'USE_MOCKED_TENANT', 'USE_MOCKED_USER']

    def usedProhibitedEnvVars = configuredEnvVars.intersect(prohibitedEnvVars)
    if (usedProhibitedEnvVars) {
        String message = """[WANRING]: \nThe environment variables: \n\n\t\"$usedProhibitedEnvVars\" \n\nset in the deployment descriptor: \n\n\t\"${
            deploymentDescriptorType.descriptorFilename
        }\" \n\nshould never be used in a productive environment. \n
This is a very high security threat, please consider removing those variables. For more information on securing your application visit: \n
https://blogs.sap.com/2017/07/18/step-7-with-sap-s4hana-cloud-sdk-secure-your-application-on-sap-cloud-platform-cloudfoundry/ \n
https://blogs.sap.com/2017/07/18/step-8-with-sap-s4hana-cloud-sdk-secure-your-application-on-sap-cloud-platform-neo/ \n"""

        String html =
            """
<h2>Unsafe variables are used in a productive branch</h2>
<p>Following variables detected in \"${deploymentDescriptorType.descriptorFilename}\" should never be used in a productive environment:</p>
<p>$usedProhibitedEnvVars</p>
<p>This is a very high security threat, please consider removing those variables. For more information on securing your application visit:</p>
<p><a href="https://blogs.sap.com/2017/07/18/step-8-with-sap-s4hana-cloud-sdk-secure-your-application-on-sap-cloud-platform-neo/">Secure App on Neo</a></p>
<p><a href="https://blogs.sap.com/2017/07/18/step-7-with-sap-s4hana-cloud-sdk-secure-your-application-on-sap-cloud-platform-cloudfoundry/">Secure App on CloudFoundry</a></p>
"""

        echo message
        checkForBadgePlugin()
        addBadge(icon: "warning.gif", text: "Unsafe environment variables are used. Please have a look into the summary or log")
        createSummary(icon: "warning.gif", text: html)
    }
}
