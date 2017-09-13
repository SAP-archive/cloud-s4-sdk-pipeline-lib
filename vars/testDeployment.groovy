def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'testDeployment', stepParameters: parameters) {
        if (parameters.cfTargets) {
            deployToCfWithCli script: parameters.script, targets: parameters.cfTargets, deploymentType: parameters.deploymentType
        } else if (parameters.neoTargets) {

            def pom = readMavenPom file: 'application/pom.xml'
            def source = "application/target/${pom.getArtifactId()}.${pom.getPackaging()}"

            deployToNeoWithCli script: parameters.script, targets: parameters.neoTargets, deploymentType: 'rolling-update', source: source
        } else {
            currentBuild.result = 'FAILURE'
            error("Test Deployment skipped because no targets defined!")
        }
    }
}