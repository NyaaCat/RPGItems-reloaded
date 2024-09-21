pipeline {
    agent any
    environment {
        PROPERTIES_FILE = 'gradle.properties'
    }
    stages {
        stage('Read Properties and Update Version') {
            steps {
                script {
                    def propertiesContent = readFile(PROPERTIES_FILE)
                    def properties = [:]
                    propertiesContent.eachLine { line ->
                        def (key, value) = line.split('=')
                        properties[key.trim()] = value.trim()
                    }

                    def majorVersion = properties['MAJOR_VERSION'].toInteger()
                    def currentMinorVersion = properties['MINOR_VERSION'].toInteger()
                    def currentPatchVersion = properties['PATCH_VERSION'] ? properties['PATCH_VERSION'].toInteger() : 0
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
                    def updatedProperties = properties.collect { key, value -> "${key}=${value}" }.join('\n')
                    writeFile file: PROPERTIES_FILE, text: updatedProperties

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
