def call() {
    pipeline {
        agent any
        parameters {
            string(name: 'PROJECT_NAME', defaultValue: "project", description: 'project repository name')
            string(name: 'PROJECT_DIR', defaultValue: "\${HOME}/backdemo/project", description: 'project pull directory')
            string(name: 'PROJECT_KEY', defaultValue: "\${HOME}/.ssh/project-key.rsa", description: 'project repository sshkey')
            string(name: 'GIT_BRANCH', defaultValue: "master", description: 'project repository branch')
            string(name: 'CONFIG_NAME', defaultValue: "", description: 'config repository name, remain blank for skip get config')
            string(name: 'CONFIG_DIR', defaultValue: "\${HOME}/backdemo/config", description: 'config pull directory. if CONFIG_NAME is blank , this is ignore ')
            string(name: 'CONFIG_KEY', defaultValue: "\${HOME}/.ssh/config-key.rsa", description: 'config repository sshkey. if CONFIG_NAME is blank , this is ignore ')
            string(name: 'CONFIG_BRANCH', defaultValue: "master", description: 'config repository branch. if CONFIG_NAME is blank , this is ignore ')
            string(name: 'BUILDTEST_CMD', defaultValue: "source \${HOME}/.profile;cd \${HOME}/backdemo/project;make test", description: 'cmd for build test ')
            string(name: 'BUILD_CMD', defaultValue: "source \${HOME}/.profile;cd \${HOME}/backdemo/project;make", description: 'cmd for build')
            string(name: 'DEPLOY_CMD', defaultValue: "\${HOME}/backdemo/project/deploy.sh", description: 'cmd for build')
        }
        environment {
            GIT_SSH_COMMAND = "ssh -i ${PROJECT_KEY}"
            GIT_SSH_CONFIG = "ssh -i ${CONFIG_KEY}"
        }
        stages{
            stage('test'){
                steps{
                script{
                    sh "echo ${params.PROJECT_DIR}"
                    sh "echo ${params.PROJECT_KEY}"
                    sh "echo ${params.PROJECT_NAME}"
                    sh "echo ${params.CONFIG_NAME}"
                    sh "echo ${params.GIT_BRANCH}"
                    sh "echo ${params.CONFIG_DIR}"
                    sh "echo ${params.CONFIG_KEY}"
                    sh "echo ${params.CONFIG_BRANCH}"
                    sh "echo ${params.BUILDTEST_CMD}"
                    sh "echo ${params.BUILD_CMD}"
                    sh "echo ${params.DEPLOY_CMD}"
                    sh "echo ${params.GIT_SSH_COMMAND}"
                }
                }
            }
        }
    }
}
