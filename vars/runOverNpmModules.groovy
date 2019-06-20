import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

def call(Map parameters = [:], Closure body) {

    def npmScripts = parameters.npmScripts
    List npmModules = []

    List scriptFilter

    if (npmScripts) {
        if (npmScripts instanceof String) {
            scriptFilter = [npmScripts]
        } else if (npmScripts instanceof List) {
            scriptFilter = npmScripts
        }
        npmModules = BuildToolEnvironment.instance.getNpmModulesWithScripts(scriptFilter)
    } else {
        npmModules = BuildToolEnvironment.instance.getNpmModules()
    }

    npmModules.each { module ->
        body(module.basePath)
    }
}
