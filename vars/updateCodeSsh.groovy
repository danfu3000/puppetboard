def call() {
    pipeline {
        agent any
        environment {
            PROJECT_DIR = "\${HOME}/backdemo/${PROJECT_NAME}"
            CONFIG_DIR = "\${HOME}/backdemo/${CONFIG_NAME}"
            GIT_SSH_COMMAND = "ssh -i \${HOME}/.ssh/${PROJECT_NAME}-key.rsa"
            GIT_SSH_CONFIG = "ssh -i \${HOME}/.ssh/${CONFIG_NAME}-key.rsa"
        }

        stages {
            stage('Pull') {
                steps {
                    script {
                        def cmd="""export BUILDDIR=${PROJECT_DIR};
                                if [ ! -d \${BUILDDIR} ];then 
                                    mkdir -p \${BUILDDIR}; 
                                    cd \${BUILDDIR}; 
                                    git init; git remote add origin ssh://git@github.com/megaease/${PROJECT_NAME}.git;
                                fi; 
                                cd \${BUILDDIR}; 
                                GIT_SSH_COMMAND="${GIT_SSH_COMMAND}" git pull;git checkout ${GIT_BRANCH}"""
                        sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: "${cmd}", execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                    }
                }
            }
            stage('PullConfig') {
                when {
                    not {environment name:'CONFIG_NAME', value:''}
                }
                steps {
                    script {
                        def cmd="""export CONFIGDIR=${CONFIG_DIR};export BUILDDIR=${PROJECT_DIR};
                                if [ ! -d \${CONFIGDIR} ];then 
                                    mkdir -p \${CONFIGDIR}; 
                                    cd \${CONFIGDIR}; 
                                    git init; git remote add origin ssh://git@github.com/megaease/${CONFIG_NAME}.git;
                                fi; 
                                cd \${CONFIGDIR}; 
                                GIT_SSH_COMMAND="${GIT_SSH_CONFIG}" git pull;
                                git checkout ${CONFIG_BRANCH};
                                cp \${CONFIGDIR}/*.env \${BUILDDIR}/scripts/;
                                """
                        sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: "${cmd}", execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                    }
                }
            }
            stage('Buildtest') {
                when {
                    not {environment name:'BUILDTEST_CMD', value:''}
                }
                steps {
                    script {
                        def cmd="""export BUILDDIR=${PROJECT_DIR};
                                cd \${BUILDDIR}; 
                                source \${HOME}/.profile;
                                ${BUILDTEST_CMD};"""
                        sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: "${cmd}", execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                    }
                }
            }
            stage('Build') {
                steps {
                    script {
                        def cmd="""export BUILDDIR=${PROJECT_DIR};
                                cd \${BUILDDIR}; 
                                source \${HOME}/.profile;
                                ${BUILD_CMD};
                                export PRODLABDIR=\${HOME}/backdemo/prodlab/bin;
                                if [ ! -d \${PRODLABDIR} ];then 
                                    mkdir -p \${PRODLABDIR};
                                fi;
                                cp \${BUILDDIR}/bin/easegateway-server \${PRODLABDIR}/easegateway-server.${BUILD_ID};
                                cp \${BUILDDIR}/bin/egctl \${PRODLABDIR}/egctl.${BUILD_ID}"""
                        sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: "${cmd}", execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                    }
                }
            }
             stage('Deploy') {
                steps {
                    script {
                        def cmd="""export BUILDDIR=${PROJECT_DIR};
                                export BUILD_ID=${BUILD_ID}
                                \${BUILDDIR}/scripts/${DEPLOY_CMD};"""
                        sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: "${cmd}", execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                    }
                }
            }
        }
    }
    post {
        success {
            script {
                def level = 'good'
                def subject = "[TASK:${JOB_NAME} SUCCESS]Deploy megaease/${JOB_NAME} in environment:${SSH_NAME}"
                helper.notifySlack level, env, subject
            }
        }

        failure {
            script {
                def level = 'danger'
                def subject = "[TASK:${JOB_NAME} FAILED]Deploy megaease/${JOB_NAME} in environment:${SSH_NAME}"
                helper.notifySlack level, env, subject
            }
        }
    }
}
