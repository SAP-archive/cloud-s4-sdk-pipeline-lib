// keep because it is used by legacy consumers

def call(Map parameters){
    Script script = parameters.script
    List customScripts = parameters.customScripts ?: []
    npmExecuteScripts script: script, runScripts: customScripts, install: true
}

