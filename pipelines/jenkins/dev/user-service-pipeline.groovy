pipeline {
    agent any
    
    environment {
        SERVICE_NAME = 'user-service'
        DOCKER_IMAGE = "ecommerce/${SERVICE_NAME}"
        DOCKER_TAG = "dev-${BUILD_NUMBER}"
        KUBECONFIG = credentials('kubeconfig')
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
        
        stage('Code Quality') {
            steps {
                dir(SERVICE_NAME) {
                    withSonarQubeEnv('SonarQube') {
                        sh './mvnw sonar:sonar'
                    }
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
                    docker.withRegistry('', 'docker-credentials') {
                        docker.image("${DOCKER_IMAGE}:${DOCKER_TAG}").push()
                    }
                }
            }
        }
        
        stage('Deploy to Dev') {
            steps {
                script {
                    sh """
                        kubectl --kubeconfig=${KUBECONFIG} -n dev set image deployment/${SERVICE_NAME} \
                        ${SERVICE_NAME}=${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            slackSend(
                color: 'good',
                message: "Dev build successful: ${SERVICE_NAME} - ${DOCKER_TAG}"
            )
        }
        failure {
            slackSend(
                color: 'danger',
                message: "Dev build failed: ${SERVICE_NAME} - ${DOCKER_TAG}"
            )
        }
    }
} 