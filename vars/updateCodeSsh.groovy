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
                    sshPublisher failOnError: true, publishers: [sshPublisherDesc(configName: "${SSH_NAME}", transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: 'export BUILDDIR=${PROJECT_DIR};if [ ! -d ${BUILDDIR} ];then mkdir -p ${BUILDDIR}; cd ${BUILDDIR}; ls -l', execTimeout: 600000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '${HOME}', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)]
                }
            }
        }
    }
}