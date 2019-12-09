def call() {
    pipeline {
        agent any
        parameters {
            string(name: 'PROJECT_NAME', defaultValue: "${JOB_NAME}", description: 'project repository name')
            string(name: 'PROJECT_DIR', defaultValue: "\${HOME}/backdemo/${PROJECT_NAME}", description: 'project pull directory')
            string(name: 'PROJECT_KEY', defaultValue: "\${HOME}/.ssh/${PROJECT_NAME}-key.rsa", description: 'project repository sshkey')
            string(name: 'GIT_BRANCH', defaultValue: "master", description: 'project repository branch')
            string(name: 'CONFIG_NAME', defaultValue: "", description: 'config repository name, remain blank for skip get config')
            string(name: 'CONFIG_DIR', defaultValue: "\${HOME}/backdemo/${CONFIG_NAME}", description: 'config pull directory. if CONFIG_NAME is blank , this is ignore ')
            string(name: 'CONFIG_KEY', defaultValue: "\${HOME}/.ssh/${CONFIG_NAME}-key.rsa", description: 'config repository sshkey. if CONFIG_NAME is blank , this is ignore ')
            string(name: 'CONFIG_BRANCH', defaultValue: "master", description: 'config repository branch. if CONFIG_NAME is blank , this is ignore ')
            string(name: 'BUILDTEST_CMD', defaultValue: "source \${HOME}/.profile;cd ${PROJECT_DIR};make test", description: 'cmd for build test ')
            string(name: 'BUILD_CMD', defaultValue: "source \${HOME}/.profile;cd ${PROJECT_DIR};make", description: 'cmd for build')
            string(name: 'DEPLOY_CMD', defaultValue: "${PROJECT_DIR}/deploy.sh", description: 'cmd for build')
        }
        environment {
            GIT_SSH_COMMAND = "ssh -i ${PROJECT_KEY}"
            GIT_SSH_CONFIG = "ssh -i ${CONFIG_KEY}"
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
                        def cmd="""${BUILDTEST_CMD}"""
                        sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: "${cmd}", execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                    }
                }
            }
            stage('Build') {
                steps {
                    script {
                        def cmd="""${BUILD_CMD}"""
                        sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: "${cmd}", execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                    }
                }
            }
             stage('Deploy') {
                steps {
                    script {
                        def cmd="""${DEPLOY_CMD}"""
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
