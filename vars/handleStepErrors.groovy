import com.sap.cloud.sdk.s4hana.pipeline.Debuglogger

def call(Map parameters = [:], body) {

    def stepParameters = parameters.stepParameters //mandatory
    def stepName = parameters.stepName //mandatory
    def echoParameters = parameters.get('echoParameters', true)
    try {
        if (stepParameters == null && stepName == null)
            error "step handleStepError requires following mandatory parameters: stepParameters, stepName"

        echo "--- BEGIN LIBRARY STEP: ${stepName}.groovy ---"

        body()

    } catch (Throwable err) {
        Debuglogger.instance.failedBuild.put("stage", stepName)
        Debuglogger.instance.failedBuild.put("reason", err)
        Debuglogger.instance.failedBuild.put("stack_trace", err.getStackTrace())
        def paramString = ''
        if (echoParameters)
            paramString = """FOLLOWING PARAMETERS WERE AVAILABLE TO THIS STEP:
***
${stepParameters?.toString()}
***"""
        echo """----------------------------------------------------------
--- ERROR OCCURRED IN LIBRARY STEP: ${stepName}
----------------------------------------------------------

${paramString}

ERROR WAS:
***
${err}
***

"""
        throw err
    } finally {
        echo "--- END LIBRARY STEP: ${stepName}.groovy ---"
    }
}
