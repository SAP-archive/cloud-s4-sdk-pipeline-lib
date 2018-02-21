import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.BashUtils

def call(Map parameters = [:], body) {
    def dockerImage = parameters.get('dockerImage', '')
    Map dockerEnvVars = parameters.get('dockerEnvVars', [:])
    def dockerOptions = parameters.get('dockerOptions', '')
    Map dockerVolumeBind = parameters.get('dockerVolumeBind', [:])

    def image = docker.image(dockerImage)
    image.pull()
    image.inside(getDockerOptions(dockerEnvVars, dockerVolumeBind, dockerOptions)) { body() }
}

/**
 * Returns a string with docker options containing
 * environment variables (if set).
 * Possible to extend with further options.
 * @param dockerEnvVars Map with environment variables
 */
@NonCPS
private getDockerOptions(Map dockerEnvVars, Map dockerVolumeBind, def dockerOptions) {
    def specialEnvironments = [
        'http_proxy',
        'https_proxy',
        'no_proxy',
        'HTTP_PROXY',
        'HTTPS_PROXY',
        'NO_PROXY'
    ]
    def options = []
    if (dockerEnvVars) {
        for (String k : dockerEnvVars.keySet()) {
            options.add("--env ${k}=${BashUtils.escape(dockerEnvVars[k].toString())}")
        }
    }

    for (String envVar : specialEnvironments) {
        if (dockerEnvVars == null || !dockerEnvVars.containsKey(envVar)) {
            options.add("--env ${envVar}")
        }
    }

    if (dockerVolumeBind) {
        for (String k : dockerVolumeBind.keySet()) {
            options.add("--volume ${k}:${dockerVolumeBind[k].toString()}")
        }
    }

    if (dockerOptions instanceof CharSequence) {
        options.add(dockerOptions.toString())
    } else if (dockerOptions instanceof List) {
        for (String option : dockerOptions) {
            options.add "${option}"
        }
    } else {
        throw new IllegalArgumentException("Unexpected type for dockerOptions. Expected was either a list or a string. Actual type was: '${dockerOptions.getClass()}'")
    }

    return options.join(' ')
}
