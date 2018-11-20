package com.sap.cloud.sdk.s4hana.pipeline

import com.cloudbees.groovy.cps.NonCPS

import java.nio.file.Paths

class PathUtils implements Serializable {
    static final long serialVersionUID = 1L

    @NonCPS
    static String normalize(String basePath, String filePath) {
        return Paths.get(basePath, filePath).normalize()
    }
}
