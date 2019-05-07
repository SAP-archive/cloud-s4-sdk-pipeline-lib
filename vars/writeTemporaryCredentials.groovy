import com.sap.cloud.sdk.s4hana.pipeline.Credential
import com.sap.cloud.sdk.s4hana.pipeline.CredentialCollection

def call(List credentialItems, String credentialsDirectory, Closure body) {
    final String credentialsFileName = 'credentials.json'

    if (credentialsDirectory == null) {
        error("This should not happen: Directory for credentials file not specified.")
    }

    try {
        if (credentialItems != null && !credentialItems.isEmpty()) {
            writeCredentials(credentialItems, credentialsDirectory, credentialsFileName)
        }
        body()
    }
    finally {
        if (credentialItems != null && !credentialItems.isEmpty()) {
            deleteCredentials(credentialsDirectory, credentialsFileName)
        }
    }
}

private assertSystemsFileExists(String credentialsDirectory){
    dir(credentialsDirectory) {
        if (!fileExists("systems.yml") && !fileExists("systems.yaml") && !fileExists("systems.json")) {
            error("The directory ${credentialsDirectory} does not contain any of the files systems.yml, systems.yaml or systems.json." +
                "However, this file is required in order to activate the integration test credentials configured in the pipeline." +
                "Please add the file as explained in the SAP Cloud SDK documentation.")
        }
    }
}

private writeCredentials(List credentialItems, String credentialsDirectory, String credentialsFileName) {
    if (credentialItems == null || credentialItems.isEmpty()) {
        echo "Not writing any credentials."
        return
    }

    assertSystemsFileExists(credentialsDirectory)

    String credentialJson = readCredentials(credentialItems).toCredentialJson()

    echo "Writing credential file with ${credentialItems.size()} items."
    dir(credentialsDirectory) {
        writeFile file: credentialsFileName, text: credentialJson
    }
}

private readCredentials(List credentialItems) {
    CredentialCollection credentialCollection = new CredentialCollection()

    for (int i = 0; i < credentialItems.size(); i++) {
        String alias = credentialItems[i]['alias']
        String jenkinsCredentialId = credentialItems[i]['credentialId']

        withCredentials([
            [$class: 'UsernamePasswordMultiBinding', credentialsId: jenkinsCredentialId, passwordVariable: 'password', usernameVariable: 'user']
        ]) {
            credentialCollection.addCredential(new Credential(alias, env.user, env.password))
        }
    }

    return credentialCollection
}

private deleteCredentials(String credentialsDirectory, String credentialsFileName) {
    echo "Deleting credential file."
    dir(credentialsDirectory) {
        sh "rm -f ${credentialsFileName}"
    }
}
