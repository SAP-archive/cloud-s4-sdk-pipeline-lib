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
        def paramString = ''
        if (echoParameters)
            paramString = """FOLLOWING PARAMETERS WERE AVAILABLE TO THIS STEP:
***
${stepParameters}
***"""
        echo """----------------------------------------------------------
--- ERROR OCCURED IN LIBRARY STEP: ${stepName}
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