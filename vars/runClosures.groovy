def call(Map closures, script) {
    handleStepErrors(stepName: 'runClosures', stepParameters: [:]) {
        if (isFeatureActive(script: script, feature: 'parallelTestExecution')) {
            parallel closures
        } else {
            def closuresToRun = closures.values().asList()
            Collections.shuffle(closuresToRun) // Shuffle the list so no one tries to rely on the order of execution
            for (int i = 0; i < closuresToRun.size(); i++) {
                (closuresToRun[i] as Closure).run()
            }
        }
    }
}
