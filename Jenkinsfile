pipeline {
    agent any
    stages {
        stage('Versioning') {
            steps {
                script {
                    env.VERSION = 3.11.1
                }
            }
        }
        stage('Build') {
            tools {
                jdk "jdk21"
            }
            steps {
                sh './gradlew publish'
                warnError('This is a canary build!') {
                    sh './gradlew -q checkRelease'
                }
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'build/libs/RPGItems-*.jar', fingerprint: true
            cleanWs()
        }
    }
}
