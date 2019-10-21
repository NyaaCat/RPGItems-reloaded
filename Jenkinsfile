pipeline {
    agent any
    options {
      skipStagesAfterUnstable()
    }
    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
                sh './gradlew publish'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'build/libs/RPGItems-*.jar', fingerprint: true
        }
    }
}
