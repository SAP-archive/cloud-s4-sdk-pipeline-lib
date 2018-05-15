package com.sap.cloud.sdk.s4hana.pipeline

import com.sap.piper.ConfigurationHelper

class NeoDeployCommandHelper implements Serializable {
    private List mandatoryParameters = [
        'host',
        'account',
        'application'
    ]


    private final String neoToolDirectory = "/sdk/tools"
    private final String neoTool = "$neoToolDirectory/neo.sh"


    private Map deploymentDescriptor
    private String username
    private String password
    private String source

    NeoDeployCommandHelper(Map deploymentDescriptor, String username, String password, String source) {
        this.deploymentDescriptor = deploymentDescriptor
        this.username = username
        this.password = password
        this.source = source
    }

    void assertMandatoryParameters() {
        for (int i = 0; i < mandatoryParameters.size(); i++) {
            String parameterName = mandatoryParameters[i]
            if (!new ConfigurationHelper(deploymentDescriptor).isPropertyDefined(parameterName)) {
                error("Please define the parameter ${parameterName} in your deployment configuration")
            }
        }
    }


    String getNeoToolDirectory() {
        return neoToolDirectory
    }

    String cloudCockpitLink() {
        return "https://account.${deploymentDescriptor.host}/cockpit#" +
            "/acc/${deploymentDescriptor.account}/app/${deploymentDescriptor.application}/dashboard"
    }

    String resourceLock() {
        return "${deploymentDescriptor.host}/${deploymentDescriptor.account}/${deploymentDescriptor.application}"
    }

    String statusCommand() {
        return "${neoTool} status ${mainArgs()}"
    }

    String rollingUpdateCommand() {
        return "${neoTool} rolling-update ${mainArgs()} -s ${source} ${additionalCommonArgs()}"
    }

    String deployCommand() {
        String command = "${neoTool} deploy ${mainArgs()} -s ${source} ${additionalCommonArgs()}"

        return command
    }

    String restartCommand() {
        return "${neoTool} restart --synchronous ${mainArgs()}"
    }

    private String mainArgs() {
        return "-h ${deploymentDescriptor.host} -a ${deploymentDescriptor.account} -b ${deploymentDescriptor.application} -u ${username} -p ${password}"
    }

    private String additionalCommonArgs() {
        String args = ""

        if (deploymentDescriptor.containsKey('ev')) {
            def value = deploymentDescriptor.ev
            if (value instanceof List) {
                for (String singleValue : value) {
                    args += " --ev ${singleValue}"
                }
            } else {
                args += " --ev ${value}"
            }
        }

        if (deploymentDescriptor.containsKey('runtime')) {
            args += " --runtime ${deploymentDescriptor.runtime}"
        }

        if (deploymentDescriptor.containsKey('runtimeVersion')) {
            args += " --runtime-version ${deploymentDescriptor.runtimeVersion}"
        }

        if (deploymentDescriptor.containsKey('vmArguments')) {
            args += " --vm-arguments \"${deploymentDescriptor.vmArguments}\""
        }

        if (deploymentDescriptor.containsKey('size')) {
            args += " --size ${deploymentDescriptor.size}"
        }

        return args
    }
}
