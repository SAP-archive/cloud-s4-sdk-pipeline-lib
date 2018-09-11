package com.sap.cloud.sdk.s4hana.pipeline

import com.cloudbees.groovy.cps.NonCPS

class CfTarget implements Serializable {
    static final long serialVersionUID = 1L

    def org
    def space
    def apiEndpoint
    def appName
    def manifest
    def credentialsId
    def username
    def password
    def mtar

    CfTarget(Map map) {
        loadNotNullFrom(map)
    }

    @NonCPS
    def loadNotNullFrom(Map map) {
        if (map) {
            this.setOrg(map.get('org'))
            this.setSpace(map.get('space'))
            this.setApiEndpoint(map.get('apiEndpoint'))

            this.setCredentialsId(map.get('credentialsId'))
            this.setUsername(map.get('username'))
            this.setPassword(map.get('password'))

            this.setAppName(map.appname)
            this.setManifest(map.manifest)
            this.setMtar(map.mtar)

        }
    }

    @NonCPS
    def setOrg(org) {
        if (org)
            this.org = org
    }

    @NonCPS
    def setSpace(space) {
        if (space)
            this.space = space
    }

    @NonCPS
    def setApiEndpoint(apiEndpoint) {
        if (apiEndpoint)
            this.apiEndpoint = apiEndpoint
    }

    @NonCPS
    def setAppName(appName) {
        if (appName)
            this.appName = appName
    }

    @NonCPS
    def setManifest(manifest) {
        if (manifest)
            this.manifest = manifest
    }

    @NonCPS
    def setCredentialsId(credentialsId) {
        if (credentialsId)
            this.credentialsId = credentialsId
    }

    @NonCPS
    def setUsername(username) {
        if (username)
            this.username = username
    }

    @NonCPS
    def setMtar(mtar) {
        if (mtar)
            this.mtar = mtar
    }

    @NonCPS
    def setPassword(password) {
        if (password)
            this.password = password
    }

    @NonCPS
    boolean isCredentialsIdDefined() {
        return this.credentialsId
    }

    @NonCPS
    boolean isUsernameAndPasswordDefined() {
        return this.username && this.password
    }

    @NonCPS
    def validate() {
        if (this.org == null || this.space == null || this.apiEndpoint == null) {
            String message = """All of the variables below must not be null: 
            | org : ${this.org} 
            | space : ${this.space} 
            | apiEndpoint : ${this.apiEndpoint} 
            | appName : ${this.appName} 
            | manifest : ${this.manifest} 
            | """.stripMargin().stripIndent()
            throw new RuntimeException(message)
        }
        if (this.mtar == null && (this.appName == null || this.manifest == null)) {
            String message = """For MTA deployments the mtar filepath must not be null and for java cf deployments the appName and manifest must not be null: 
            | mtar : ${this.mtar} 
            | appName : ${this.appName} 
            | manifest : ${this.manifest} 
            | """.stripMargin().stripIndent()
            throw new RuntimeException(message)
        }
    }

    @NonCPS
    Map toMap() {
        return ["org": this.org, "space": this.space, "apiEndpoint": this.apiEndpoint, "appName": this.appName, "manifest": this.manifest, "credentialsId": this.credentialsId, "username": this.username, "password": this.password]
    }
}
