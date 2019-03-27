def call(Map parameters){
    mavenExecute(
        script: parameters.script,
        flags: '--update-snapshots --batch-mode',
        m2Path: s4SdkGlobals.m2Directory,
        goals: 'clean install',
        defines: '-Dmaven.test.skip=true',
    )
}
