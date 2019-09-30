import com.cloudbees.groovy.cps.NonCPS
import org.jenkins.plugins.lockableresources.LockableResourcesManager

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'postActionCleanupStashesLocks', stepParameters: parameters) {
        def script = parameters.script

        lock("lockableResourcesManager") {
            removeStashLock(script.commonPipelineEnvironment.configuration.stashFiles)
        }
    }
}

@NonCPS
private removeStashLock(lockPrefix) {
    def manager = LockableResourcesManager.get()
    def resources = manager.getResources().findAll { it.name.startsWith(lockPrefix) }
    resources.each{ manager.getResources().remove(it) }
    manager.save()
}
