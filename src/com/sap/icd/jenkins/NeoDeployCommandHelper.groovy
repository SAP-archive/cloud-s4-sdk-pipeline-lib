package com.sap.icd.jenkins

class NeoDeployCommandHelper implements Serializable{
    private List mandatoryParameters = [
        'host',
        'account',
        'application'
    ]
    private String neoTool = "/sdk/tools/neo.sh"


    private Map deploymentDescriptor
    private String username
    private String password
    private String source

    NeoDeployCommandHelper(Map deploymentDescriptor, String username, String password, String source){
        this.deploymentDescriptor = deploymentDescriptor
        this.username = username
        this.password = password
        this.source = source
    }

    void assertMandatoryParameters(){
        for(int i=0; i<mandatoryParameters.size(); i++){
            String parameterName = mandatoryParameters[i]
            if(!new ConfigurationHelper(deploymentDescriptor).isPropertyDefined(parameterName)){
                error("Please define the parameter ${parameterName} in your deployment configuration")
            }
        }
    }

    String resourceLock(){
        return "${deploymentDescriptor.host}/${deploymentDescriptor.account}/${deploymentDescriptor.application}"
    }

    String statusCommand(){
        return "${neoTool} status ${mainArgs()}"
    }

    String rollingUpdateCommand(){
        return "${neoTool} rolling-update ${mainArgs()} -s ${source} ${additionalCommonArgs()}"
    }

    String deployCommand(){
        String command = "${neoTool} deploy ${mainArgs()} -s ${source} ${additionalCommonArgs()}"

        if(deploymentDescriptor.containsKey('runtime')){
            command += " --runtime ${deploymentDescriptor.runtime}"
        }

        return command
    }

    String restartCommand(){
        return "${neoTool} restart ${mainArgs()}"
    }

    private String mainArgs(){
        return "-h ${deploymentDescriptor.host} -a ${deploymentDescriptor.account} -b ${deploymentDescriptor.application} -u ${username} -p ${password}"
    }

    private String additionalCommonArgs(){
        String args = ""

        if(deploymentDescriptor.containsKey('ev')){
            def value = deploymentDescriptor.ev
            if(value instanceof List){
                for(String singleValue: value){
                    args += " --ev ${singleValue}"
                }
            }
            else {
                args += " --ev ${value}"
            }
        }

        if(deploymentDescriptor.containsKey('runtimeVersion')){
            args += " --runtime-version ${deploymentDescriptor.runtimeVersion}"
        }

        if(deploymentDescriptor.containsKey('vmArguments')){
            args += " --vm-arguments \"${deploymentDescriptor.vmArguments}\""
        }

        return args
    }
}
