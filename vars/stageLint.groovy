import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

import com.sap.piper.ConfigurationLoader

void call(Map parameters = [:]) {
    def stageName = 'lint'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        List jsFiles = []
        List jsxFiles = []
        List tsFiles = []
        List tsxFiles = []

        try {
            jsFiles = findFiles(glob: '**/*.js', excludes: '**/node_modules/**')
            jsxFiles = findFiles(glob: '**/*.jsx', excludes: '**/node_modules/**')
            tsFiles = findFiles(glob: '**/*.ts', excludes: '**/node_modules/**')
            tsxFiles = findFiles(glob: '**/*.tsx', excludes: '**/node_modules/**')
        } catch (IOException ioe) {
            error "An error occurred when looking for js/ts files.\n"
        }

        if (jsFiles.size() > 0 || jsxFiles.size() > 0 || tsFiles.size() > 0 || tsxFiles.size() > 0) {
            // Might be written both upper and lowercase
            String[] ui5ComponentsUpperCase = []
            String[] ui5ComponentsLowerCase = []

            try {
                ui5ComponentsUpperCase = findFiles(glob: '**/Component.js', excludes: '**/node_modules/**')
                ui5ComponentsLowerCase = findFiles(glob: '**/component.js', excludes: '**/node_modules/**')
            } catch (IOException ioe) {
                error "An error occurred when looking for UI5 components.\n"
            }
            String[] ui5Components = ui5ComponentsUpperCase.plus(ui5ComponentsLowerCase)

            if (BuildToolEnvironment.instance.getNpmModulesWithScripts(['ci-lint'])){
                checkUserLint(script: script, configuration: stageConfiguration)
            } else if (ui5Components.size() > 0) {
                checkUi5BestPractices(script: script, configuration: stageConfiguration, ui5Components: ui5Components)
            } else {
                checkDefaultLint(script: script, configuration: stageConfiguration)
            }
        } else {
            echo "No Javascript/Typescript files found, skipping lint stage."
        }
    }
}
