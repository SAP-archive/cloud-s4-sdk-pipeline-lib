import com.sap.cloud.sdk.s4hana.pipeline.Debuglogger
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:], body) {
    def stepParameters = parameters.stepParameters //mandatory
    def stepName = parameters.stepName //mandatory
    Script script = stepParameters.script //mandatory


    if (stepParameters == null || stepName == null || script == null) {
        String message = "The parameter "+ stepParameters ? (stepName ? "script" : "stepName") : "stepParameters" + " is null"
        message += "step handleStepError requires following mandatory parameters: stepParameters, stepName and script"
        error message
    }

    def echoParameters = parameters.get('echoParameters', true)
    def commonPipelineEnvironment = script.commonPipelineEnvironment
    List mandatoryStages = ConfigurationLoader.generalConfiguration(script)?.get('mandatoryStages') ?: []

    try {
        echo "--- BEGIN LIBRARY STEP: ${stepName}.groovy ---"
        body()
    } catch (Throwable err) {
        Debuglogger.instance.failedBuild.put("stage", stepName)
        Debuglogger.instance.failedBuild.put("reason", err)
        Debuglogger.instance.failedBuild.put("stack_trace", err.getStackTrace())
        if (stepParameters.isResilient && !mandatoryStages.contains(stepName)) {
            Debuglogger.instance.failedBuild.put("isResilient", "true")
            try {
                //use new unstable feature if available: see https://jenkins.io/blog/2019/07/05/jenkins-pipeline-stage-result-visualization-improvements/
                unstable(err.toString())
            } catch (java.lang.NoSuchMethodError nmEx) {
                script.currentBuild.result = 'UNSTABLE'
            }
            List unstableSteps = commonPipelineEnvironment?.getValue('unstableSteps') ?: []
            if(!unstableSteps) {
                unstableSteps = []
            }

            // add information about unstable steps to pipeline environment
            // this helps to bring this information to users in a consolidated manner inside a pipeline
            unstableSteps.add(stepName)
            commonPipelineEnvironment?.setValue('unstableSteps', unstableSteps)
            displayErrorMessage(stepName, stepParameters, err, echoParameters)

        } else {
            if (stepParameters.isResilient && mandatoryStages.contains(stepName)) {
                echo "The failure of stage ${stepName} cannot be set to UNSTABLE with isResilient flag"
            }
            displayErrorMessage(stepName, stepParameters, err, echoParameters)

            throw err
        }
    } finally {
        echo "--- END LIBRARY STEP: ${stepName}.groovy ---"
    }
}

private displayErrorMessage(stepName, stepParameters, error, echoParameters) {
    def errorMessage= """
----------------------------------------------------------
    ERROR OCCURRED IN LIBRARY STEP: ${stepName}           
----------------------------------------------------------

Error Details:
**********************************************************
    ${error}
**********************************************************
"""
    echo errorMessage
    if(echoParameters) {
        echo "Parameters available to this step are: ${stepParameters?.toString()}"
    }
}
