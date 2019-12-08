def call() {
    pipeline {
        agent any
        environment {
            BUILDDIR = "${HOME}/backdemo/"
            PROJECT_DIR = "${HOME}/backdemo/${PROJECT_NAME}"
            CONFIG_DIR = "${HOME}/backdemo/${CONFIG_NAME}"
            GIT_SSH_COMMAND = "ssh -i ${HOME}/repository-keys/${PROJECT_NAME}-key.rsa"
            GIT_SSH_CONFIG = "ssh -i ${HOME}/repository-keys/${CONFIG_NAME}-key.rsa"
        }

        stages {
            stage('Pull') {
                steps {
                    sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: 'export BUILDDIR=${PROJECT_DIR};if [ ! -d ${BUILDDIR} ];then mkdir -p ${BUILDDIR}; cd ${BUILDDIR}; git init; git remote add origin ssh://git@github.com/megaease/${PROJECT_NAME}.git; fi;cd ${BUILDDIR};GIT_SSH_COMMAND="${GIT_SSH_COMMAND}" git pull', execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                }
            }
            stage('Pullconfig') {
                steps {
                    sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: 'km05', transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: 'export BUILDDIR=${PROJECT_DIR};export CONFIGDIR=${CONFIG_DIR};if [ ! -d ${CONFIGDIR} ];then mkdir -p ${CONFIGDIR}; cd ${CONFIGDIR}; git init; git remote add origin ssh://git@github.com/megaease/${CONFIG_NAME}.git; fi;cd ${CONFIGDIR};GIT_SSH_COMMAND="${GIT_SSH_CONFIG" git pull origin ${CONFIG_BRANCH}; cp *.env ${BUILDDIR}/scripts/', execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                }
            }
        }
    }
}