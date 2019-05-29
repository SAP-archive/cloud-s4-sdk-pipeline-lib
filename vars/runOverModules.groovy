import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

def call(Map parameters = [:], body) {
    Script script = parameters.script

    def moduleType = parameters.moduleType

    List moduleTypeFilter

    if(moduleType instanceof String){
        moduleTypeFilter = [moduleType]
    }
    else if (moduleType instanceof List){
        moduleTypeFilter = moduleType
    }
    else {
        error("Parameter moduleType has to be either a String or a List.")
    }

    List pathOfModules = BuildToolEnvironment.instance.getModulesPathOfType(moduleTypeFilter)

    pathOfModules.each{ basePath ->
        body(basePath)
    }

}
