pipeline {
    agent any
    stages {
        stage('Build') {
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
