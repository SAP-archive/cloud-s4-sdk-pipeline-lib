import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.piper.JenkinsUtils

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkDeploymentDescriptors', stepParameters: parameters) {

        def script = parameters.script

        Set keys = ['unsafeMode']
        Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
        Map projectGeneralConfiguration = ConfigurationLoader.generalConfiguration(script)
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, "productionDeployment")
        Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, "cloudFoundryDeploy")
        Map generalConfig = ConfigurationMerger.merge(projectGeneralConfiguration, keys, defaultGeneralConfiguration)
        boolean unsafeMode = generalConfig.unsafeMode
        boolean safeMode = !unsafeMode

        Set usedForbiddenEnvironmentVariables = []

        if (isProductiveBranch(script: script)) {

            if (script.commonPipelineEnvironment.configuration.isMta) {
                def mta = readYaml file: 'mta.yaml'
                usedForbiddenEnvironmentVariables.addAll(extractForbiddenEnvironmentVariablesMta(mta))
            }

            def neoTargets = stageConfiguration.neoTargets
            if (neoTargets) {
                usedForbiddenEnvironmentVariables.addAll(extractForbiddenEnvironmentVariablesNeo(neoTargets))
            }

            def cfTargets = stageConfiguration.cfTargets
            if (cfTargets) {
                Set stageLevelManifestFiles = computeStageLevelManifestFiles(cfTargets, stepConfiguration?.cloudFoundry?.manifest)

                for (int i = 0; i < stageLevelManifestFiles.size(); i++) {
                    def manifest = readYaml file: stageLevelManifestFiles[i]
                    usedForbiddenEnvironmentVariables.addAll(extractForbiddenEnvironmentVariablesCloudFoundry(manifest))
                }
            }

            if (usedForbiddenEnvironmentVariables) {
                if (safeMode) {
                    error(formatLogMessage(usedForbiddenEnvironmentVariables))
                } else {
                    logUnsafeUsage(usedForbiddenEnvironmentVariables)
                }
            }
        }
    }
}

@NonCPS
private Set extractForbiddenEnvironmentVariablesMta(mtaDescriptor) {
    Set extractedEnvironmentVariables = []
    def modules = mtaDescriptor.modules
    for (int i = 0; i < modules.size(); i++) {
        if (modules[i].properties) {
            extractedEnvironmentVariables.addAll(modules[i].properties.keySet())
        }
    }
    return extractForbiddenEnvironmentVariables(extractedEnvironmentVariables)
}

@NonCPS
private Set extractForbiddenEnvironmentVariablesNeo(neoTargets) {
    Set extractedEnvironmentVariables = []
    for (int i = 0; i < neoTargets.size(); i++) {
        def environment = neoTargets[i].environment
        if (environment) {
            extractedEnvironmentVariables.addAll(environment.keySet())
        }
    }
    return extractForbiddenEnvironmentVariables(extractedEnvironmentVariables)
}

@NonCPS
private Set computeStageLevelManifestFiles(cfTargets, defaultManifest) {
    Set stageLevelManifestFiles = []
    for (int i = 0; i < cfTargets.size(); i++) {
        if (cfTargets[i].manifest) {
            stageLevelManifestFiles.add(cfTargets[i].manifest)
        } else if (defaultManifest) {
            stageLevelManifestFiles.add(defaultManifest)
        }
    }
    return stageLevelManifestFiles
}

@NonCPS
private Set extractForbiddenEnvironmentVariablesCloudFoundry(manifest) {
    Set extractedEnvironmentVariables = []
    def applications = manifest.applications
    for (int i = 0; i < applications.size(); i++) {
        def env = applications[i].env
        if (env) {
            extractedEnvironmentVariables.addAll(env.keySet())
        }
    }
    return extractForbiddenEnvironmentVariables(extractedEnvironmentVariables)
}

@NonCPS
private Set extractForbiddenEnvironmentVariables(Set extractedEnvironmentVariables) {
    final Set forbiddenEnvironmentVariables = ['ALLOW_MOCKED_AUTH_HEADER', 'USE_MOCKED_TENANT', 'USE_MOCKED_USER']
    return extractedEnvironmentVariables.intersect(forbiddenEnvironmentVariables)
}

@NonCPS
private logUnsafeUsage(usedForbiddenEnvironmentVariables) {
    if (usedForbiddenEnvironmentVariables) {
        String message = formatLogMessage(usedForbiddenEnvironmentVariables)

        String html =
            """
<h2>Unsafe variables are used in a productive branch</h2>
<p>Following environment variables should never be used in a productive environment:</p>
<p>$usedForbiddenEnvironmentVariables</p>
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

@NonCPS
// TODO Replace this method with a generic, centralized method to ensure plugin compliance in the pipeline/library
private void checkForBadgePlugin() {
    final String PLUGIN_ID_BADGE = 'badge'
    if (!JenkinsUtils.isPluginActive(PLUGIN_ID_BADGE)) {
        String exception = """[ERROR] Plugin '${PLUGIN_ID_BADGE}' is not installed or not active.
Please update the Jenkins image to the latest available version. 
For more information how to update the image please visit:
https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/doc/operations/operations-guide.md#update-image
"""
        throw new RuntimeException(exception)
    }
}

@NonCPS
private String formatLogMessage(Set usedForbiddenEnvironmentVariables) {
    """The environment variables \n\n\t\"$usedForbiddenEnvironmentVariables\" \n\nset in the deployment descriptor should never be used in a productive environment. \n
This is a very high security threat, please consider removing those variables. For more information on securing your application visit: \n
https://blogs.sap.com/2017/07/18/step-7-with-sap-s4hana-cloud-sdk-secure-your-application-on-sap-cloud-platform-cloudfoundry/ \n
https://blogs.sap.com/2017/07/18/step-8-with-sap-s4hana-cloud-sdk-secure-your-application-on-sap-cloud-platform-neo/ \n"""
}
