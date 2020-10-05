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
                    loadPiper script: parameters.script
                    piperPipelineStageInit script: parameters.script, customDefaults: ['default_s4_pipeline_environment.yml'], useTechnicalStageNames: true
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
                }
            }

            stage('Remote Tests') {
                when {
                    anyOf {
                        expression { parameters.script.commonPipelineEnvironment.configuration.runStage.endToEndTests };
                        expression { parameters.script.commonPipelineEnvironment.configuration.runStage.performanceTests }
                    }
                }
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

            stage('Security') {
                when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.security } }
                steps { piperPipelineStageSecurity script: parameters.script }
            }

            stage('Compliance') {
                when { expression { parameters.script.commonPipelineEnvironment.configuration.runStage.compliance } }
                steps { piperPipelineStageCompliance script: parameters.script }
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
            /* https://jenkins.io/doc/book/pipeline/syntax/#post */
            success { buildSetResult(currentBuild) }
            aborted { buildSetResult(currentBuild, 'ABORTED') }
            failure { buildSetResult(currentBuild, 'FAILURE') }
            unstable { buildSetResult(currentBuild, 'UNSTABLE') }
            cleanup {
                piperPipelineStagePost script: parameters.script
                deleteDir()
            }
        }
    }
}
