void call(parameters) {
    pipeline {
        agent any
        options {
            timeout(time: 120, unit: 'MINUTES')
            timestamps()
            buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
            skipDefaultCheckout()
        }
        stages {
            stage('Init') {
                steps {
                    milestone 10
                    stageInitS4sdkPipeline script: parameters.script, nodeLabel: parameters.initNodeLabel
                    abortOldBuilds script: parameters.script
                }
            }

            stage('Build and Test') {
                steps {
                    milestone 20
                    piperPipelineStageBuild script: parameters.script
                }
            }

            stage('Local Tests') {
                parallel {
                    stage("Backend Integration Tests") {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.backendIntegrationTests } }
                        steps { stageBackendIntegrationTests script: parameters.script }
                    }
                    stage("Frontend Integration Tests") {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.frontendIntegrationTests } }
                        steps { stageFrontendIntegrationTests script: parameters.script }
                    }
                    stage("Additional Unit Tests") {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.additionalUnitTests } }
                        steps { piperPipelineStageAdditionalUnitTests script: parameters.script }
                    }
                    stage("NPM Dependency Audit") {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.npmAudit } }
                        steps { stageNpmAudit script: parameters.script }
                    }
                }
            }

            stage('Remote Tests') {
                when { anyOf { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.endToEndTests };
                    expression { parameters.script.commonPipelineEnvironment.configuration.runStage.performanceTests } } }
                parallel {
                    stage("End to End Tests") {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.endToEndTests } }
                        steps { piperPipelineStageAcceptance script: parameters.script }
                    }
                    stage("Performance Tests") {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.performanceTests } }
                        steps { stagePerformanceTests script: parameters.script }
                    }
                }
            }

            stage('Quality Checks') {
                steps {
                    milestone 50
                    stageS4SdkQualityChecks script: parameters.script
                }
            }

            stage('Third-party Checks') {
                when { anyOf { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.checkmarxScan };
                    expression { parameters.script.commonPipelineEnvironment.configuration.runStage.whitesourceScan };
                    expression { parameters.script.commonPipelineEnvironment.configuration.runStage.fortifyScan };
                    expression { parameters.script.commonPipelineEnvironment.configuration.runStage.detectScan };
                    expression { parameters.script.commonPipelineEnvironment.configuration.runStage.additionalTools };
                    expression { parameters.script.commonPipelineEnvironment.configuration.runStage.compliance }
                } }
                parallel {
                    stage("Checkmarx Scan") {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.checkmarxScan } }
                        steps { stageCheckmarxScan script: parameters.script }
                    }
                    stage("WhiteSource Scan") {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.whitesourceScan } }
                        steps { stageWhitesourceScan script: parameters.script }
                    }
                    stage("Fortify Scan") {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.fortifyScan } }
                        steps { stageFortifyScan script: parameters.script }
                    }
                    stage("Detect Scan"){
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.detectScan } }
                        steps { stageDetect script: parameters.script }
                    }
                    stage("Additional Tools") {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.additionalTools } }
                        steps { stageAdditionalTools script: parameters.script }
                    }
                    stage('SonarQube Scan') {
                        when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.compliance } }
                        steps { piperPipelineStageCompliance script: parameters.script }
                    }
                }
            }

            stage('Artifact Deployment') {
                when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.artifactDeployment } }
                steps {
                    milestone 70
                    piperPipelineStageArtifactDeployment script: parameters.script
                }
            }

            stage('Production Deployment') {
                when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.productionDeployment } }
                // "ordinal 80" is configured for stage "productionDeployment" in pipeline defaults
                steps { piperPipelineStageRelease script: parameters.script }
            }

        }
        post {
            always {
                script {
                    debugReportArchive script: parameters.script
                    postActionSendNotification script: parameters.script
                    postActionCleanupStashesLocks script: parameters.script
                    sendAnalytics script: parameters.script

                    if (parameters.script.commonPipelineEnvironment?.configuration?.runStage?.postPipelineHook) {
                        stage('Post Pipeline Hook') {
                            stagePostPipelineHook script: parameters.script
                        }
                    }
                }
            }
            success {
                script {
                    if (parameters.script.commonPipelineEnvironment?.configuration?.runStage?.archiveReport) {
                        postActionArchiveReport script: parameters.script
                    }
                }
            }
            failure {
                deleteDir()
            }
        }
    }
}
