package com.sap.cloud.sdk.s4hana.pipeline

class NeoDeployCommandHelper implements Serializable {
    private List mandatoryParameters = [
        'host',
        'account',
        'application',
        'runtime',
        'runtimeVersion'
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
            if (!this.deploymentDescriptor[parameterName]) {
                def errorMessage = "Please define the parameter ${parameterName} in your deployment configuration"
                throw new RuntimeException(errorMessage)
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

        if (deploymentDescriptor.containsKey('environment')) {
            def environment = deploymentDescriptor.environment
            def keys = environment.keySet()

            for (int i = 0; i < keys.size(); i++) {
                def key = keys[i]
                def value = environment.get(keys[i])
                args += " --ev ${BashUtils.escape(key)}=${BashUtils.escape(value)}"
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
