pipeline {
    agent any
    environment {
        PROPERTIES_FILE = 'gradle.properties'
    }
    stages {
        stage('Read Properties and Update Version') {
            steps {
                script {
                    def properties = readProperties file: PROPERTIES_FILE
                    def majorVersion = properties['MAJOR_VERSION']?.trim().toInteger()
                    def currentMinorVersion = properties['MINOR_VERSION']?.trim().toInteger()
                    def currentPatchVersion = properties['PATCH_VERSION'] ? properties['PATCH_VERSION'].trim().toInteger() : 0
                    def lastMinorVersion = currentMinorVersion

                    if (env.LAST_MINOR_VERSION) {
                        lastMinorVersion = env.LAST_MINOR_VERSION.toInteger()
                    }

                    if (currentMinorVersion == lastMinorVersion) {
                        currentPatchVersion += 1
                    } else {
                        currentPatchVersion = 0
                    }

                    def newVersion = "${majorVersion}.${currentMinorVersion}.${currentPatchVersion}"
                    echo "New version: ${newVersion}"

                    properties['PATCH_VERSION'] = currentPatchVersion.toString()
                    writeProperties file: PROPERTIES_FILE, properties: properties

                    env.LAST_MINOR_VERSION = currentMinorVersion.toString()
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
