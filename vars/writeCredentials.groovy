import com.sap.cloud.sdk.s4hana.pipeline.Credential
import com.sap.cloud.sdk.s4hana.pipeline.CredentialCollection

def call(List credentialItems) {
    handleStepErrors(stepName: 'writeCredentials', stepParameters: credentialItems) {
        final String CREDENTIALS_FILE_NAME = 'credentials.json'

        if (credentialItems == null || credentialItems.isEmpty()) {
            echo "Not writing any credentials."
            return
        }

        String credentialJson = readCredentials(credentialItems).toCredentialJson()

        echo "Writing credential file with ${credentialItems.size()} items."
        writeFile file: CREDENTIALS_FILE_NAME, text: credentialJson
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
