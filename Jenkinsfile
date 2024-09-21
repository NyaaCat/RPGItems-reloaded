pipeline {
    agent any
    stages {
        stage('Versioning') {
            steps {
                script {
                    def version = env.VERSION.split('\\.')
                    def majorVersion = version[0].toInteger()
                    def minorVersion = version[1].toInteger()
                    def patchVersion = version[2].toInteger()

                    def updateMinorVersion = true

                    if (updateMinorVersion) {
                        minorVersion += 0
                        patchVersion = 1
                    }

                    def newVersion = "${majorVersion}.${minorVersion}.${patchVersion}"
                    echo "新版本号: ${newVersion}"
                    env.VERSION = newVersion
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
