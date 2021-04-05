pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew publish'
                try {
                    sh './gradlew -q checkRelease'
                }
                catch (exc) {
                    echo 'This is a canary build!'
                    currentBuild.result = 'UNSTABLE'
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
