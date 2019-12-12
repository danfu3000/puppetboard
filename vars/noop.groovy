import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.io.File

def info(message) {
    echo "INFO: ${message}"
}

def warning(message) {
    echo "WARNING: ${message}"
}

// 发送 Slack 通知
def notifySlack(level, env, subject) {

    echo level

    // Pusher
    def commit_id = sh returnStdout: true, script: "cd ${env.PROJECT_DIR};git rev-parse HEAD;"
    commit_id = commit_id.trim()
    def author_info = sh returnStdout: true, script: "cd ${env.PROJECT_DIR};git show ${commit_id} | grep Author;"
    def author_name = author_info.split(" ")[1]

    // Commit Messages
    def changesets = ""
    withCredentials([usernamePassword(credentialsId: "${USER_CREDENTIALS_ID}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        changesets = sh returnStdout: true, script: "curl ${env.JOB_URL}${env.BUILD_NUMBER}/wfapi/changesets --user ${USERNAME}:${PASSWORD}"
        echo "${changesets}"
    }

    def jsonSlurper = new JsonSlurper()
    def object = jsonSlurper.parseText("${changesets}")

    def commit_messages = ""
    if (object) {
        def commits_info = object.get(0)
        def commits = commits_info.get("commits")
        for (commit in commits) {
            commit_messages += commit.get("message")
            commit_messages += "\n"
        }
    }
    echo "${commit_messages}"

    // Repository
    def repo_config = scm.getUserRemoteConfigs()[0]
    def repo_url = repo_config.getUrl()
    def repository = repo_url.split(":")[1].replaceAll("\\.git", ":") + "${env.GIT_BRANCH}"
    echo "${repository}"

    def fields = [
            [
                    "title": "Repository",
                    "value": "${repository}",
                    "short": true
            ],
            [
                    "title": "Pusher",
                    "value": "${author_name}",
                    "short": true
            ],
            [
                    "title": "Commit Messages",
                    "value": "${commit_messages}",
                    "short": false
            ],
    ]

    def attachments = [
            [
                    "color"   : "${level}",
                    "fallback": "Push Details",
                    "fields"  : fields
            ]
    ]

    def post_data = [
            "text"       : subject,
            "attachments": attachments
    ]
    echo "${post_data}"
    def post_json = JsonOutput.toJson(post_data)
    echo "${post_json}"

    def process = ["curl", "-X", "POST", "-H", "Content-Type: application/json", "-d", "${post_json}", "${env.SLACK_HOOK}"].execute().text
    echo "${process}"
}

def getSlackLevelAndStatus(success) {
    def level = "good"
    def status = "SUCCESS"

    if (!success) {
        level = "danger"
        status = "FAILED"
    }
    return [level, status]

}

def postBuildImage(env, success) {

    echo "${success}"

    def (level, status) = getSlackLevelAndStatus(success)
    def subject = "[TASK: ${env.STAGE_NAME} ${status}] Build image ${env.IMAGE_TAG} to registry ${env.REGISTRY_URL} in environments ${ENV_NAME}"
    notifySlack(level, env, subject)
}

def postPublishImage(env, success) {

    echo "${success}"

    def (level, status) = getSlackLevelAndStatus(success)
    def subject = "[TASK: ${env.STAGE_NAME} ${status}] Pulish image ${env.IMAGE_TAG} to registry ${env.REGISTRY_URL} in environments ${ENV_NAME}"

    notifySlack(level, env, subject)

}

def postDeploy(env, success) {
    echo "${success}"

    def (level, status) = getSlackLevelAndStatus(success)
    def subject = "[TASK:${env.STAGE_NAME}  ${status}] Update image ${env.IMAGE_TAG} to component ${env.PROJECT_NAME} of stack ${env.EASESTACK_STACK_ID}, environment: ${ENV_NAME}"

    notifySlack(level, env, subject)

}

def updateConfiguration(env) {
    def config_filenames = ['.easeops.yml', '.easeops.yaml', 'easeops.yml', 'easeops.yaml']
    for (config_filename in config_filenames) {

        def file_path = "${env.PROJECT_DIR}/${config_filename}"
        File file = new File(file_path);
        if (file.exists()) {

            def task_config = readYaml file: "${file_path}"
            echo "${task_config}"

            def tasks = task_config.tasks

            for (task in tasks) {
                def task_name = task.name
                def task_type = task.type
                def task_environments = task.environments
                def task_configs = task.configs
                echo "${task_config}"

                for (config in task_configs) {

                    def component = config.component
                    env.COMPONENT = "${component}"

                    def items = config.items
                    for (item in items) {
                        def item_file = item.file
                        def item_file_path = "${env.PROJECT_DIR}/${item_file}"
                        File itemFile = new File(item_file_path)
                        def config_datas = itemFile.text

                        item_config = [:]
                        item_config.value = config_datas
                        item_config.use_as_file = item.map_to_file
                        if (item.is_sub_path == "true") {
                            item_config.is_sub_path = true
                        } else {
                            item_config.is_sub_path = false
                        }

                        def data = ["CONFIGS": [item_config], "message": "configurations were updated "]
                        def post_json = JsonOutput.toJson(data)
                        echo "${post_json}"

                        def result = ["curl", "-X", "PUT", "-H", "Content-Type: application/json", "-d", "${post_json}", "${env.EASESTACK_ENDPOINT}/v1/stacks/${env.EASESTACK_STACK_ID}/components/${component}/configs"].execute().text
                        echo "${result}"
                    }
                }
            }
        }
    }
}

def postUpdateConfiguration(env, success) {

    def (level, status) = getSlackLevelAndStatus(success)
    def subject = "[TASK:${env.STAGE_NAME} ${status}] Update stack:${env.EASESTACK_STACK_ID} - configuration of component ${env.COMPONENT}, environments: ${ENV_NAME}"

    notifySlack(level, env, subject)

}


Object loadConfigFile(env_name) {
    def config_text = libraryResource "application-${env_name}.json"
    def jsonSlurper = new JsonSlurper()
    def config_object = jsonSlurper.parseText(config_text)
    return config_object
}

def sonarAnalyseForMaven(env) {

    // SONARQUBE_SERVER: the name config in Manage jenkins -> Configure System -> SonarQube servers.
    withSonarQubeEnv(env.SONARQUBE_SERVER){
        sh "cd ${env.PROJECT_NAME};mvn clean verify sonar:sonar -DskipTests"
    }
}

def sonarAnalyseForFront(env) {
    // SONAR_SCANNER: the name config in Manage Jenkins -> Global Tool Configuration -> SonarQube Scanner.
    def scannerHome = tool "${env.SONAR_SCANNER}"
    withSonarQubeEnv(env.SONARQUBE_SERVER){
        sh "cd ${PROJECT_DIR};${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${PROJECT_NAME} -Dsonar.projectName=${PROJECT_NAME} -Dsonar.projectVersion=1.0 -Dsonar.projectBaseDir=${PROJECT_DIR} -Dsonar.sources=src  -Dsonar.sourceEncoding=UTF-8"
    }
}

def getPullRequestComment(repo, pullid){
    withCredentials([string(credentialsId: "kmtesttoken", variable: 'TOKEN')]) {
        def result = ["curl", "-X", "GET", "-H", "Authorization: token ${TOKEN}","https://api.github.com/repos/${repo}/pulls/${pullid}/comments"].execute().text
        echo "${result}"
        changesets = sh returnStdout: true, script: """curl -H "Authorization: token ${TOKEN}" "https://api.github.com/repos/${repo}/pulls/${pullid}/comments" """
        echo "${changesets}"
    }
}

def setPullRequestComment(repo, pullid, text){
    def post_data = [
            "body"       : text
        ]
    echo "${post_data}"
    def post_json = JsonOutput.toJson(post_data)
    withCredentials([string(credentialsId: "kmtesttoken", variable: 'TOKEN')]) {
        def result = ["curl", "-X", "POST", "-H", "Authorization: token ${TOKEN}", "-d", "${post_json}", "https://api.github.com/repos/${repo}/pulls/${pullid}/comments"].execute().text
        echo "${result}"
    }
}

def setPullRequestReviews(repo, pullid, state, text){
    def status = "APPROVE"

    if (!state) {
        status = "PENDING"
    }

    def post_data = [
            "body"       : text,
            "event"      : status
        ]
    echo "${post_data}"

    def post_json = JsonOutput.toJson(post_data)
    withCredentials([string(credentialsId: "kmtesttoken", variable: 'TOKEN')]) {
        def result = ["curl", "-X", "POST", "-H", "Authorization: token ${TOKEN}", "-d", "${post_json}", "https://api.github.com/repos/${repo}/pulls/${pullid}/reviews"].execute().text
        echo "${result}"
    }
}
