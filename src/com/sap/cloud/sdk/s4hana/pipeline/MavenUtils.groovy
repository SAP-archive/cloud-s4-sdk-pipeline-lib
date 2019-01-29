package com.sap.cloud.sdk.s4hana.pipeline

class MavenUtils implements Serializable {
    static final long serialVersionUID = 1L
    static void generateEffectivePom(script, pomFile, effectivePomFile) {
        script.mavenExecute(script: script,
            flags: '--batch-mode',
            pomPath: pomFile,
            m2Path: script.s4SdkGlobals.m2Directory,
            goals: 'help:effective-pom',
            defines: "-Doutput=${effectivePomFile}")
    }
}
