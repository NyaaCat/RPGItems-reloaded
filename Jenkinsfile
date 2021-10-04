pipeline {
    agent any
    stages {
        stage('Build') {
            tools {
                jdk "jdk16"
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
