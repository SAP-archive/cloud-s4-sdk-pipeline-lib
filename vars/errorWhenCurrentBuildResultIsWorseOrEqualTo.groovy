def call(def currentBuild, def worthEqual, def msg) {
    if (currentBuild.resultIsWorseOrEqualTo(worthEqual)) {
        currentBuild.result = 'FAILURE'
        error(msg)
    }
}