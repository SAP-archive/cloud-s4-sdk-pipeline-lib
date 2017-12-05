def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'checkGatling', stepParameters: parameters) {
        def script = parameters.script
        try{
            executeMaven script: script, flags: '-U -B', pomPath: 'performance-tests/pom.xml', m2Path: s4SdkGlobals.m2Directory, goals: 'test'
        }
        finally{
            gatlingArchive()
        }
    }
}
