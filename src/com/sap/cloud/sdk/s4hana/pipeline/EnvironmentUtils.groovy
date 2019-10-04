package com.sap.cloud.sdk.s4hana.pipeline

import com.cloudbees.groovy.cps.NonCPS
import java.nio.file.Files
import java.nio.file.Paths

class EnvironmentUtils implements Serializable {
    static boolean cxServerDirectoryExists() {
        if (Files.isDirectory(Paths.get("/var/cx-server/"))) {
            return true
        }
        return false
    }

    @NonCPS
    static String getDockerFile(String serverCfgAsString) {
        String result = "not_found"
        serverCfgAsString.splitEachLine("=") { items ->
            if (items[0] == "docker_image") {
                result = items[1].replaceAll('"', '')
            }
        }
        return result
    }
}
