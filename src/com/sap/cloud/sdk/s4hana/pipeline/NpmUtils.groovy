package com.sap.cloud.sdk.s4hana.pipeline

class NpmUtils implements Serializable {
    static void renameNpmScript(Script script, String packageJsonPath, String oldName, String newName) {
        Map packageJson = script.readJSON file: packageJsonPath
        if (packageJson?.scripts && packageJson.scripts[oldName]) {
            packageJson.scripts[newName] = packageJson.scripts[oldName]
            packageJson.scripts.remove(oldName)
            script.writeJSON json: packageJson, file: packageJsonPath
            script.archiveArtifacts artifacts: 'package.json'
            script.echo "[WARNING]: You are using a legacy configuration parameter which might not be supported in the future in `package.json`. "
            "The npm-command $oldName` is deprecated. Please rename it to `$newName` as shown in the `package.json` file in the build artifacts."
        }
    }
}
