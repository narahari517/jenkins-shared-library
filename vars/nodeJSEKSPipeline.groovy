def call(Map configMap) {
    pipeline {
        agent {
            label 'agent-1'
        }
        options {
            timeout(time: 30, unit: 'MINUTES')
            disableConcurrentBuilds()
            retry(1)
        }
        parameters{
            booleanParam(name: 'deploy', defaultValue: false, description: 'Select to deploy or not')
        }
        environment {
            appVersion = '' // this will become global, we can use across pipeline
            region = "us-east-1"
            account_id = "816069152585"
            project = configMap.get("project")
            environment = "dev"
            component = configMap.get("component")
        }
        stages {
            stage('Read the version') {
                steps {
                    script {
                        def packageJson = readJSON file: 'package.json'
                        appVersion = packageJson.version
                        echo "App Version : ${appVersion}"
                    }
                }
            }
            stage('Install Dependencies') {
                steps {
                    sh "npm install"
                }
            }
            // stage('SonarQube Analysis') {
            //     environment {
            //         SCANNER_HOME = tool 'sonar-6.0' // scanner config
            //     }
            //     steps {
            //         // sonar qube server injection
            //         withSonarQubeEnv('sonar-6.0') {
            //             sh '$SCANNER_HOME/bin/sonar-scanner'
            //             // this is a generic scanner, it automatically understands the language and provides the scan results
            //         }
            //     }
            // }

            // stage('SQuality Gate') {
            //     steps {
            //         timeout(time: 5, unit: 'MINUTES') {
            //             waitForQualityGate abortPipeline: true
            //         }
            //     }
            // }

            stage('Docker build') {
                
                steps {
                    withAWS(region: 'us-east-1', credentials: 'aws-creds') {
                        sh """
                        aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.us-east-1.amazonaws.com

                        docker build -t ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${appVersion} .

                        docker images

                        docker push ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${appVersion}
                        """
                    }
                }
            }

            stage('Deploy'){
                when {
                    expression{params.deploy}
                }
                steps {
                    build job: 'backend-cd', parameters: [
                        string(name: 'version', value: "$appVersion"),
                        string(name: 'ENVIRONMENT', value: "dev"),
                    ], wait: true
                }
            }
        }

        post {
            always{
                echo "This section runs always"
                deleteDir()
            }
            success{
                echo "This section runs when pipeline success"
            }
            failure{
                echo "This section runs when pipeline failure"
            }
        }
    }
}