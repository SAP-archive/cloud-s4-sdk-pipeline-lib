import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkDiskSpace', stepParameters: parameters) {
        try {
            if (lessThanOneGigabytePlusBufferAvailable()) {
                String dockerInformation = getDockerInformation()
                sendOutOfDiskSpaceMail(parameters)
                addBadge(icon: 'error.gif', text: 'Jenkins is running out of disk space. Please have a look into the pipeline summary.')
                createSummary(icon: 'error.gif', text: createDiskCleanupRecommendationsHtml(dockerInformation))
                error(createDiskCleanupRecommendations(dockerInformation))
            }
        } catch (NumberFormatException | NullPointerException | ArrayIndexOutOfBoundsException e) {
            echo "Jenkins was not able to determine the remaining disk space. The check for available disk space will be skipped."
        }
    }
}

private boolean lessThanOneGigabytePlusBufferAvailable() throws NumberFormatException, NullPointerException, ArrayIndexOutOfBoundsException {
    String freeDiskSpace = sh (returnStdout: true, script: "df -m ${env.JENKINS_HOME} | awk \'FNR > 1 {print \$4}\'") // ignore line 1 (Header of table) and print only column 4 (available disk space)
    int freeDiskSpaceInMb = parseInt(freeDiskSpace)

    String lastBuildDirectoryPath = getLastBuildDirectoryPath()
    String lastBuildSizeInMb = sh(returnStdout: true, script: "du -md 0 ${lastBuildDirectoryPath} | awk \'{print \$1}\'")

    int lastBuildBuffer = parseInt(lastBuildSizeInMb) * 3

    return freeDiskSpaceInMb < 1024 + lastBuildBuffer
}

private String getLastBuildDirectoryPath() throws ArrayIndexOutOfBoundsException {
    String currentBuildNumber = env.BUILD_NUMBER
    int lastBuildNumber = parseInt(currentBuildNumber) - 1
    def (jobName, branch) = env.JOB_NAME.split('/')

    return "${env.JENKINS_HOME}/jobs/${jobName}/branches/${branch}/builds/${lastBuildNumber}"
}

@NonCPS
private double parseInt(String s) throws NumberFormatException, NullPointerException {
    return Integer.parseInt(s.trim())
}

private String createDiskCleanupRecommendations(String dockerInformation) {
    return """Jenkins is running out of disk space
            |Please free up some space that Jenkins can work properly
            |This is the disk-usage of docker (command: docker system df):
            |$dockerInformation
            |Please consider removing unused images, containers or volumes:
            |Images: \"docker rmi Imagename\"
            |Volumes: \"docker volume rm Volumename\"
            |Containers: \"docker rm Containername\"
            |Or use the prune command (be really careful with that command): \"docker container/volume/image prune\"
            |For more information please visit https://docs.docker.com
            |You can also login to the Jenkins docker container with \"docker exec -it s4sdk-jenkins-master bash\",
            |cd to /var/jenkins_home/jobs and execute \"for dir in \$(find . -maxdepth 1 -type d); do du -sch \$dir; done\" to list all jobs and their disk usage.
            """.stripMargin().stripIndent()
}

private String createDiskCleanupRecommendationsHtml(String dockerInformation) {
    return """<h2>Jenkins is running out of disk space</h2>
            <p>Please free up some space that Jenkins can work properly</p>
            <h3>This is the disk-usage of docker (command: docker system df):</h2>
            <pre>$dockerInformation</pre>
            <p>Please consider removing unused images, containers or volumes:</p>
            <li>Images: <code>docker rmi Imagename</code></li>
            <li>Volumes: <code>docker volume rm Volumename</code></li>
            <li>Containers: <code>docker rm Containername</code></li>
            <li>Or use the prune command (be really careful with that command): <code>docker container/volume/image prune</code></li>
            <p>For more information please visit <a href="https://docs.docker.com">docker documentation</a></p>
            <p>You can also login to the Jenkins docker container with <code>docker exec -it s4sdk-jenkins-master bash</code>,
            cd to /var/jenkins_home/jobs and execute <code>for dir in \$(find . -maxdepth 1 -type d); do du -sch \$dir; done</code> to list all jobs and their disk usage.</p>
            """.stripMargin().stripIndent()
}

private String getDockerInformation() {
    String dockerInformation
    lock('docker-system-df-command') {
        dockerInformation = sh (returnStdout: true, script: 'docker system df')
    }
    return dockerInformation
}

private void sendOutOfDiskSpaceMail(Map parameters) {
    String body = "Jenkins is running out of disk space. Please visit ${env.BUILD_URL} and have a look into the pipeline summary for recommendations how to clean up the instance."
    String subject = 'Jenkins running out of disk space'
    String recipientsAsString = getRecipients(parameters)

    if (recipientsAsString) {
        emailext(recipientProviders: [[$class: 'CulpritsRecipientProvider']],
            to: recipientsAsString,
            subject: subject,
            body: body
        )
    }
}

private String getRecipients(Map parameters) {
    def script = parameters.script

    Map postActionConfiguration = ConfigurationLoader.postActionConfiguration(script, 'sendNotification')
    Set postActionConfigurationKeys = ['recipients']
    Set parameterKeys = []
    Map defaults = [recipients: ['']]
    Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, postActionConfiguration, postActionConfigurationKeys, defaults)

    return configuration.recipients.join(", ")
}
