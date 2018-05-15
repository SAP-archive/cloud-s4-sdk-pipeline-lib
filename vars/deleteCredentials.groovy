def call() {
    final CREDENTIALS_FILE_NAME = 'credentials.json'
    echo "Deleting credential file."
    sh "rm -f ${CREDENTIALS_FILE_NAME}"
}
