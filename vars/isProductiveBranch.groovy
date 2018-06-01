def call(Map parameters) {
    def script = parameters.script
    Map configWithDefault = loadEffectiveGeneralConfiguration script: script
    return configWithDefault['productiveBranch'] == env.BRANCH_NAME
}
