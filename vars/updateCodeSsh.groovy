def call() {
    pipeline {
        agent any
        parameters {
            string(name: 'SSH_NAME', defaultValue: params.SSH_NAME?:"km05", description: 'sshPublisher hostname')

            string(name: 'PROJECT_BASEDIR', defaultValue: params.PROJECT_BASEDIR?:"/home/ubuntu/backdemo", description: 'project pull directory')
            string(name: 'PROJECT_KEYDIR', defaultValue: params.PROJECT_KEYDIR?:"/home/ubuntu/.ssh", description: 'project pull directory')
            string(name: 'PROJECT_NAME', defaultValue: params.PROJECT_NAME?:"project", description: 'project repository name')
            string(name: 'PROJECT_KEY', defaultValue: params.PROJECT_KEY?:"project-key.rsa", description: 'project repository sshkey')
            string(name: 'GIT_BRANCH', defaultValue: params.GIT_BRANCH?:"master", description: 'project repository branch')
            
            string(name: 'CONFIG_NAME', defaultValue: params.CONFIG_NAME?:"", description: 'config repository name, remain blank for skip get config')
            string(name: 'CONFIG_DIR', defaultValue: params.CONFIG_DIR?:"config", description: 'config pull directory. if CONFIG_NAME is blank , this is ignore ')
            string(name: 'CONFIG_KEY', defaultValue: params.CONFIG_KEY?:"config-key.rsa", description: 'config repository sshkey. if CONFIG_NAME is blank , this is ignore ')
            string(name: 'CONFIG_BRANCH', defaultValue: params.CONFIG_BRANCH?:"master", description: 'config repository branch. if CONFIG_NAME is blank , this is ignore ')
            string(name: 'ENV_FILE', defaultValue: params.ENV_FILE?:"*.env", description: 'config repository branch. if CONFIG_NAME is blank , this is ignore ')
            
            string(name: 'SCRIPT_DIR', defaultValue: params.SCRIPT_DIR?:"script", description: 'config repository branch. if CONFIG_NAME is blank , this is ignore ')
            string(name: 'BUILDTEST_CMD', defaultValue: params.BUILDTEST_CMD?:"make test", description: 'cmd for build test ')
            string(name: 'BUILD_CMD', defaultValue: params.BUILD_CMD?:"make", description: 'cmd for build')
            string(name: 'DEPLOY_CMD', defaultValue: params.DEPLOY_CMD?:"deploy.sh", description: 'cmd for deploy')
        }
        environment {
            GIT_SSH_COMMAND = "ssh -i ${PROJECT_KEYDIR}/${PROJECT_KEY}"
            GIT_SSH_CONFIG = "ssh -i ${PROJECT_KEYDIR}/${CONFIG_KEY}"
        }
        stages{
            stage('Pull') {
                steps {
                    script {
                        def cmd="""export BUILDDIR=${PROJECT_BASEDIR}/${PROJECT_NAME};
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
                        def cmd="""export CONFIGDIR=${PROJECT_BASEDIR}/${CONFIG_NAME};export BUILDDIR=${PROJECT_BASEDIR}/${PROJECT_NAME};
                                if [ ! -d \${CONFIGDIR} ];then 
                                    mkdir -p \${CONFIGDIR}; 
                                    cd \${CONFIGDIR}; 
                                    git init; git remote add origin ssh://git@github.com/megaease/${CONFIG_NAME}.git;
                                fi; 
                                cd \${CONFIGDIR}; 
                                GIT_SSH_COMMAND="${GIT_SSH_CONFIG}" git pull;
                                git checkout ${CONFIG_BRANCH};
                                cp \${CONFIGDIR}/${ENV_FILE} \${BUILDDIR}/${SCRIPT_DIR}/;
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
                        def cmd="""source \${HOME}/.profile;cd ${PROJECT_BASEDIR}/${PROJECT_NAME};${BUILDTEST_CMD}"""
                        sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: "${cmd}", execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                    }
                }
            }
            stage('Build') {
                steps {
                    script {
                        def cmd="""source \${HOME}/.profile;cd ${PROJECT_BASEDIR}/${PROJECT_NAME};${BUILD_CMD}"""
                        sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: "${cmd}", execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                    }
                }
            }
             stage('Deploy') {
                steps {
                    script {
                        def cmd="""source \${HOME}/.profile;cd ${PROJECT_BASEDIR}/${PROJECT_NAME}/${SCRIPT_DIR};${DEPLOY_CMD}"""
                        sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: "${cmd}", execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
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
}
