import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stageBuildBackend', stepParameters: parameters) {
        def script = parameters.script
        Map configuration = ConfigurationLoader.stageConfiguration(script, 'buildBackend')

        unstashFiles script: script, stage:'buildBackend'

        executeMaven script: script, flags: '-U -B', m2Path: s4SdkGlobals.m2Directory, goals: 'clean install', defines:'-Dmaven.test.skip=true', dockerImage: configuration.dockerImage

        stashFiles script: script, stage:'buildBackend'
    }
}