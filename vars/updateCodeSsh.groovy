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
        stages{
            stage('test'){
                script{
                    sh "echo ${PROJECT_DIR}"
                    sh "echo ${PROJECT_KEY}"
                    sh "echo ${PROJECT_NAME}"
                    sh "echo ${CONFIG_NAME}"
                    sh "echo ${GIT_BRANCH}"
                    sh "echo ${CONFIG_DIR}"
                    sh "echo ${CONFIG_KEY}"
                    sh "echo ${CONFIG_BRANCH}"
                    sh "echo ${BUILDTEST_CMD}"
                    sh "echo ${BUILD_CMD}"
                    sh "echo ${DEPLOY_CMD}"
                    sh "echo ${GIT_SSH_COMMAND}"
                }
            }
        }
    }
}
