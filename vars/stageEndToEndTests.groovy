import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stageEndToEndTests', stepParameters: parameters) {
        final script = parameters.script

        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'endToEndTests')

        unstashFiles script: script, stage:'endToEndTests'
        if (stageConfiguration?.cfTarget) {
            deployToCfWithCli script: script, targets: [stageConfiguration.cfTarget], deploymentType: 'standard'
            executeEndToEndTest(script: script, appUrl: stageConfiguration.appUrl)
        }else if(stageConfiguration?.neoTarget){
            def pom = readMavenPom file:'application/pom.xml'
            def source = "application/target/${pom.getArtifactId()}.${pom.getPackaging()}"

            deployToNeoWithCli script: script,targets: [stageConfiguration.neoTarget], deploymentType: 'standard', source:source
            executeEndToEndTest(script: script, appUrl: stageConfiguration.appUrl)
        } else {
            echo "End to end tests skipped because no targets defined!"
        }
        stashFiles script: script, stage:'endToEndTests'
    }
}