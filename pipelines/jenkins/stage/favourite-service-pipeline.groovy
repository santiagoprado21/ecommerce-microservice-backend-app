pipeline {
    agent any
    
    environment {
        SERVICE_NAME = 'favourite-service'
        DOCKER_IMAGE = "ecommerce/${SERVICE_NAME}"
        DOCKER_TAG = "stage-${BUILD_NUMBER}"
        KUBECONFIG = credentials('kubeconfig')
        SONAR_TOKEN = credentials('sonar-token')
        DOCKER_CREDENTIALS_ID = 'docker-credentials'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                dir(SERVICE_NAME) {
                    sh './mvnw clean package -DskipTests'
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                dir(SERVICE_NAME) {
                    sh './mvnw test'
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                dir(SERVICE_NAME) {
                    sh './mvnw verify -Pintegration-test'
                }
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                }
            }
        }
        
        stage('Code Quality') {
            steps {
                dir(SERVICE_NAME) {
                    withSonarQubeEnv('SonarQube') {
                        sh """
                            ./mvnw sonar:sonar \
                            -Dsonar.projectKey=${SERVICE_NAME} \
                            -Dsonar.projectName='${SERVICE_NAME}' \
                            -Dsonar.token=${SONAR_TOKEN}
                        """
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                dir(SERVICE_NAME) {
                    script {
                        docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")
                    }
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('', DOCKER_CREDENTIALS_ID) {
                        docker.image("${DOCKER_IMAGE}:${DOCKER_TAG}").push()
                        docker.image("${DOCKER_IMAGE}:${DOCKER_TAG}").push('latest')
                    }
                }
            }
        }
        
        stage('Deploy to Stage') {
            steps {
                script {
                    sh """
                        kubectl --kubeconfig=${KUBECONFIG} -n stage set image deployment/${SERVICE_NAME} \
                        ${SERVICE_NAME}=${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                }
            }
        }
        
        stage('E2E Tests') {
            steps {
                dir(SERVICE_NAME) {
                    sh './mvnw test -Pe2e-test'
                }
            }
            post {
                always {
                    junit '**/target/e2e-reports/*.xml'
                }
            }
        }
        
        stage('Performance Tests') {
            steps {
                dir(SERVICE_NAME) {
                    sh """
                        pip install locust
                        locust -f src/test/python/locustfile.py \
                            --headless \
                            --host http://stage.favourite-service:8080 \
                            --users 100 \
                            --spawn-rate 10 \
                            --run-time 5m \
                            --html-file target/locust-report.html
                    """
                }
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: false,
                        keepAll: true,
                        reportDir: "${SERVICE_NAME}/target",
                        reportFiles: 'locust-report.html',
                        reportName: 'Locust Performance Report'
                    ])
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            script {
                def releaseNotes = """
                    # Release Notes - ${SERVICE_NAME} - Version stage-${BUILD_NUMBER}
                    
                    ## Build Information
                    - Build Number: ${BUILD_NUMBER}
                    - Build URL: ${BUILD_URL}
                    - Git Branch: ${GIT_BRANCH}
                    
                    ## Test Results
                    - Unit Tests: Passed
                    - Integration Tests: Passed
                    - E2E Tests: Passed
                    - Performance Tests: Report available at ${BUILD_URL}Locust_Performance_Report
                    
                    ## Quality Metrics
                    - SonarQube Analysis: Passed
                    - Quality Gate: Passed
                    
                    ## Deployment Information
                    - Environment: Stage
                    - Docker Image: ${DOCKER_IMAGE}:${DOCKER_TAG}
                    - Deployment Status: Successful
                    
                    ## Changes
                    ${changeSets.collect { it.msg }.join('\n')}
                """
                
                writeFile file: "${SERVICE_NAME}/target/release-notes.md", text: releaseNotes
                archiveArtifacts artifacts: "${SERVICE_NAME}/target/release-notes.md"
            }
            
            slackSend(
                color: 'good',
                message: "Stage build successful: ${SERVICE_NAME} - ${DOCKER_TAG}"
            )
        }
        failure {
            slackSend(
                color: 'danger',
                message: "Stage build failed: ${SERVICE_NAME} - ${DOCKER_TAG}"
            )
        }
    }
} 