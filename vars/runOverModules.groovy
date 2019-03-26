import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

def call(Map parameters = [:], body) {
    Script script = parameters.script
    String moduleType = parameters.moduleType

    List pathOfModules = BuildToolEnvironment.instance.getModulesPathOfType(moduleType)

    pathOfModules.each{ basePath ->
        body(basePath)
    }

}
