pipeline {
    agent any
    options {
      skipStagesAfterUnstable()
    }
    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'build/libs/RPGItems-reloaded-release.jar', fingerprint: true
        }
    }
}
