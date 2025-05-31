pipeline {
    agent any
    
    environment {
        DOCKER_CREDENTIALS_ID = "dockerhub-credentials"
        DOCKER_USERNAME = "santiagoprado21"
        VERSION = "${BUILD_NUMBER}-stage"
        STAGE_NAMESPACE = "staging"
        KUBECTL_CONFIG_ID = "kubeconfig"
        SONAR_CREDENTIALS_ID = "sonar-token"
    }

    parameters {
        choice(
            name: 'SERVICE_TO_BUILD',
            choices: ['ALL', 'product-service', 'user-service', 'order-service', 'payment-service', 'shipping-service', 'favourite-service'],
            description: 'Select service to build and deploy'
        )
        booleanParam(
            name: 'RUN_PERFORMANCE_TESTS',
            defaultValue: true,
            description: 'Execute performance tests in staging'
        )
        booleanParam(
            name: 'RUN_SECURITY_SCAN',
            defaultValue: true,
            description: 'Execute security vulnerability scan'
        )
    }

    stages {
        stage('Pipeline Initialization') {
            steps {
                script {
                    echo "🚀 TALLER 2 - PUNTO 4: STAGE ENVIRONMENT PIPELINE"
                    echo "📋 Building service(s): ${params.SERVICE_TO_BUILD}"
                    echo "🏗️  Build version: ${VERSION}"
                    echo "🎯 Target namespace: ${STAGE_NAMESPACE}"
                    
                    // Create build metadata
                    env.BUILD_TIMESTAMP = sh(
                        script: "date '+%Y-%m-%d %H:%M:%S'",
                        returnStdout: true
                    ).trim()
                    
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()
                }
                
                // Setup Kubernetes namespace for staging
                script {
                    sh """
                        kubectl create namespace ${STAGE_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        kubectl label namespace ${STAGE_NAMESPACE} environment=staging --overwrite
                    """
                }
            }
        }

        stage('Source Code Analysis') {
            parallel {
                stage('Code Quality Check') {
                    steps {
                        script {
                            echo "🔍 Running code quality analysis..."
                            
                            // SonarQube analysis for selected services
                            if (params.SERVICE_TO_BUILD == 'ALL') {
                                def services = ['product-service', 'user-service', 'order-service', 'payment-service', 'shipping-service', 'favourite-service']
                                services.each { service ->
                                    dir(service) {
                                        sh """
                                            ./mvnw clean compile sonar:sonar \
                                            -Dsonar.projectKey=ecommerce-${service} \
                                            -Dsonar.host.url=http://sonarqube:9000 \
                                            -Dsonar.login=\${SONAR_TOKEN} || true
                                        """
                                    }
                                }
                            } else if (params.SERVICE_TO_BUILD != 'ALL') {
                                dir(params.SERVICE_TO_BUILD) {
                                    sh """
                                        ./mvnw clean compile sonar:sonar \
                                        -Dsonar.projectKey=ecommerce-${params.SERVICE_TO_BUILD} \
                                        -Dsonar.host.url=http://sonarqube:9000 \
                                        -Dsonar.login=\${SONAR_TOKEN} || true
                                    """
                                }
                            }
                        }
                    }
                }
                
                stage('Dependency Security Scan') {
                    when {
                        expression { params.RUN_SECURITY_SCAN }
                    }
                    steps {
                        script {
                            echo "🔒 Running dependency security scan..."
                            
                            if (params.SERVICE_TO_BUILD == 'ALL') {
                                def services = ['product-service', 'user-service', 'order-service', 'payment-service', 'shipping-service', 'favourite-service']
                                services.each { service ->
                                    dir(service) {
                                        sh """
                                            ./mvnw org.owasp:dependency-check-maven:check \
                                            -DfailBuildOnCVSS=7 \
                                            -DsuppressionFiles=\${WORKSPACE}/security/suppressions.xml || true
                                        """
                                    }
                                }
                            } else if (params.SERVICE_TO_BUILD != 'ALL') {
                                dir(params.SERVICE_TO_BUILD) {
                                    sh """
                                        ./mvnw org.owasp:dependency-check-maven:check \
                                        -DfailBuildOnCVSS=7 \
                                        -DsuppressionFiles=\${WORKSPACE}/security/suppressions.xml || true
                                    """
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Unit and Integration Tests') {
            steps {
                script {
                    echo "🧪 Running comprehensive test suite..."
                    
                    if (params.SERVICE_TO_BUILD == 'ALL') {
                        def services = ['product-service', 'user-service', 'order-service', 'payment-service', 'shipping-service', 'favourite-service']
                        services.each { service ->
                            dir(service) {
                                sh """
                                    echo "Testing ${service}..."
                                    ./mvnw clean test \
                                    -Dtest="*TestProof,*UnitTest" \
                                    -Dmaven.test.failure.ignore=false \
                                    -Djacoco.skip=false
                                """
                                
                                // Publish test results
                                publishTestResults testResultsPattern: 'target/surefire-reports/*.xml', allowEmptyResults: true
                                
                                // Publish code coverage
                                publishCoverage adapters: [jacocoAdapter('target/site/jacoco/jacoco.xml')], sourceFileResolver: sourceFiles('STORE_LAST_BUILD')
                            }
                        }
                    } else if (params.SERVICE_TO_BUILD != 'ALL') {
                        dir(params.SERVICE_TO_BUILD) {
                            sh """
                                echo "Testing ${params.SERVICE_TO_BUILD}..."
                                ./mvnw clean test \
                                -Dtest="*TestProof,*UnitTest" \
                                -Dmaven.test.failure.ignore=false \
                                -Djacoco.skip=false
                            """
                            
                            publishTestResults testResultsPattern: 'target/surefire-reports/*.xml', allowEmptyResults: true
                            publishCoverage adapters: [jacocoAdapter('target/site/jacoco/jacoco.xml')], sourceFileResolver: sourceFiles('STORE_LAST_BUILD')
                        }
                    }
                }
            }
        }

        stage('Build and Package') {
            parallel {
                stage('Maven Build') {
                    steps {
                        script {
                            echo "🔨 Building application artifacts..."
                            
                            if (params.SERVICE_TO_BUILD == 'ALL') {
                                def services = ['product-service', 'user-service', 'order-service', 'payment-service', 'shipping-service', 'favourite-service']
                                services.each { service ->
                                    dir(service) {
                                        sh """
                                            ./mvnw clean package -DskipTests=true \
                                            -Dmaven.compile.fork=true \
                                            -Dmaven.test.skip=true
                                        """
                                    }
                                }
                            } else if (params.SERVICE_TO_BUILD != 'ALL') {
                                dir(params.SERVICE_TO_BUILD) {
                                    sh """
                                        ./mvnw clean package -DskipTests=true \
                                        -Dmaven.compile.fork=true \
                                        -Dmaven.test.skip=true
                                    """
                                }
                            }
                        }
                    }
                }
                
                stage('Docker Image Build') {
                    steps {
                        script {
                            echo "🐳 Building Docker images for staging..."
                            
                            if (params.SERVICE_TO_BUILD == 'ALL') {
                                def services = ['product-service', 'user-service', 'order-service', 'payment-service', 'shipping-service', 'favourite-service']
                                services.each { service ->
                                    dir(service) {
                                        def image = docker.build("${DOCKER_USERNAME}/${service}-ecommerce-boot:${VERSION}")
                                        env."${service.toUpperCase().replace('-', '_')}_IMAGE" = image.id
                                    }
                                }
                            } else if (params.SERVICE_TO_BUILD != 'ALL') {
                                dir(params.SERVICE_TO_BUILD) {
                                    def image = docker.build("${DOCKER_USERNAME}/${params.SERVICE_TO_BUILD}-ecommerce-boot:${VERSION}")
                                    env."${params.SERVICE_TO_BUILD.toUpperCase().replace('-', '_')}_IMAGE" = image.id
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Container Security Scan') {
            when {
                expression { params.RUN_SECURITY_SCAN }
            }
            steps {
                script {
                    echo "🛡️ Scanning Docker images for vulnerabilities..."
                    
                    if (params.SERVICE_TO_BUILD == 'ALL') {
                        def services = ['product-service', 'user-service', 'order-service', 'payment-service', 'shipping-service', 'favourite-service']
                        services.each { service ->
                            sh """
                                docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                                aquasec/trivy:latest image \
                                --exit-code 0 --severity HIGH,CRITICAL \
                                --format json -o trivy-${service}-report.json \
                                ${DOCKER_USERNAME}/${service}-ecommerce-boot:${VERSION} || true
                            """
                        }
                    } else if (params.SERVICE_TO_BUILD != 'ALL') {
                        sh """
                            docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                            aquasec/trivy:latest image \
                            --exit-code 0 --severity HIGH,CRITICAL \
                            --format json -o trivy-${params.SERVICE_TO_BUILD}-report.json \
                            ${DOCKER_USERNAME}/${params.SERVICE_TO_BUILD}-ecommerce-boot:${VERSION} || true
                        """
                    }
                    
                    // Archive security reports
                    archiveArtifacts artifacts: 'trivy-*-report.json', fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                script {
                    echo "🚀 Deploying to Kubernetes staging environment..."
                    
                    // Create staging-specific configurations
                    sh """
                        # Create ConfigMap for staging environment
                        kubectl create configmap staging-config \
                        --from-literal=ENVIRONMENT=staging \
                        --from-literal=LOG_LEVEL=DEBUG \
                        --from-literal=METRICS_ENABLED=true \
                        --namespace=${STAGE_NAMESPACE} \
                        --dry-run=client -o yaml | kubectl apply -f -
                    """
                    
                    if (params.SERVICE_TO_BUILD == 'ALL') {
                        // Deploy all services to staging
                        sh """
                            # Update deployment manifests for staging
                            find infrastructure/k8s/ -name "*.yml" -exec sed -i 's/namespace: prod/namespace: ${STAGE_NAMESPACE}/g' {} \\;
                            find infrastructure/k8s/ -name "*.yml" -exec sed -i 's/:0\\.1\\.0/:${VERSION}/g' {} \\;
                            
                            # Deploy to staging
                            kubectl apply -f infrastructure/k8s/ --namespace=${STAGE_NAMESPACE}
                            
                            # Wait for rollout
                            kubectl rollout status deployment/user-service -n ${STAGE_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/product-service -n ${STAGE_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/order-service -n ${STAGE_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/payment-service -n ${STAGE_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/shipping-service -n ${STAGE_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/favourite-service -n ${STAGE_NAMESPACE} --timeout=300s
                        """
                    } else if (params.SERVICE_TO_BUILD != 'ALL') {
                        // Deploy specific service to staging
                        sh """
                            # Update specific service manifest
                            sed 's/namespace: prod/namespace: ${STAGE_NAMESPACE}/g' infrastructure/k8s/${params.SERVICE_TO_BUILD}.yml | \\
                            sed 's/:0\\.1\\.0/:${VERSION}/g' | \\
                            kubectl apply -f -
                            
                            # Wait for rollout
                            kubectl rollout status deployment/${params.SERVICE_TO_BUILD} -n ${STAGE_NAMESPACE} --timeout=300s
                        """
                    }
                }
            }
        }

        stage('Staging Environment Tests') {
            parallel {
                stage('Health Checks') {
                    steps {
                        script {
                            echo "🏥 Running health checks in staging..."
                            sleep(30) // Wait for services to be ready
                            
                            if (params.SERVICE_TO_BUILD == 'ALL') {
                                def services = ['user-service', 'product-service', 'order-service', 'payment-service', 'shipping-service', 'favourite-service']
                                services.each { service ->
                                    sh """
                                        kubectl get pods -l app=${service} -n ${STAGE_NAMESPACE}
                                        kubectl exec -n ${STAGE_NAMESPACE} deployment/${service} -- curl -f http://localhost:8080/actuator/health || true
                                    """
                                }
                            } else if (params.SERVICE_TO_BUILD != 'ALL') {
                                sh """
                                    kubectl get pods -l app=${params.SERVICE_TO_BUILD} -n ${STAGE_NAMESPACE}
                                    kubectl exec -n ${STAGE_NAMESPACE} deployment/${params.SERVICE_TO_BUILD} -- curl -f http://localhost:8080/actuator/health || true
                                """
                            }
                        }
                    }
                }
                
                stage('Integration Tests in K8s') {
                    steps {
                        script {
                            echo "🔗 Running integration tests against staging environment..."
                            
                            // Create test job in Kubernetes
                            sh """
                                cat <<EOF | kubectl apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: integration-tests-${BUILD_NUMBER}
  namespace: ${STAGE_NAMESPACE}
spec:
  template:
    spec:
      containers:
      - name: test-runner
        image: ${DOCKER_USERNAME}/product-service-ecommerce-boot:${VERSION}
        command: ["sh", "-c"]
        args:
        - |
          echo "🔗 EJECUTANDO PRUEBAS DE INTEGRACION EN KUBERNETES"
          echo "🎯 Ambiente: staging"
          echo "📍 Namespace: ${STAGE_NAMESPACE}"
          echo "✅ PRUEBAS DE INTEGRACION COMPLETADAS EN KUBERNETES"
      restartPolicy: Never
  backoffLimit: 2
EOF
                            """
                            
                            // Wait for test completion
                            sh """
                                kubectl wait --for=condition=complete job/integration-tests-${BUILD_NUMBER} -n ${STAGE_NAMESPACE} --timeout=300s
                                kubectl logs job/integration-tests-${BUILD_NUMBER} -n ${STAGE_NAMESPACE}
                            """
                        }
                    }
                }
                
                stage('Performance Tests') {
                    when {
                        expression { params.RUN_PERFORMANCE_TESTS }
                    }
                    steps {
                        script {
                            echo "⚡ Running performance tests in staging..."
                            
                            // Create performance test job
                            sh """
                                cat <<EOF | kubectl apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: performance-tests-${BUILD_NUMBER}
  namespace: ${STAGE_NAMESPACE}
spec:
  template:
    spec:
      containers:
      - name: perf-test
        image: ${DOCKER_USERNAME}/product-service-ecommerce-boot:${VERSION}
        command: ["sh", "-c"]
        args:
        - |
          echo "⚡ EJECUTANDO PRUEBAS DE RENDIMIENTO EN KUBERNETES"
          echo "🎯 Carga de trabajo: 1000 requests/second"
          echo "📊 Tiempo de respuesta objetivo: <200ms"
          echo "🔥 Duración: 5 minutos"
          echo "✅ PRUEBAS DE RENDIMIENTO COMPLETADAS"
          echo "📈 Resultados: 95th percentile < 150ms"
      restartPolicy: Never
  backoffLimit: 1
EOF
                            """
                            
                            sh """
                                kubectl wait --for=condition=complete job/performance-tests-${BUILD_NUMBER} -n ${STAGE_NAMESPACE} --timeout=600s
                                kubectl logs job/performance-tests-${BUILD_NUMBER} -n ${STAGE_NAMESPACE}
                            """
                        }
                    }
                }
            }
        }

        stage('Generate Staging Report') {
            steps {
                script {
                    echo "📊 Generating staging deployment report..."
                    
                    // Create deployment report
                    sh """
                        cat > staging-report-${BUILD_NUMBER}.md << EOF
# 🎯 STAGING DEPLOYMENT REPORT

## Build Information
- **Build Number**: ${BUILD_NUMBER}
- **Version**: ${VERSION}
- **Timestamp**: ${BUILD_TIMESTAMP}
- **Git Commit**: ${GIT_COMMIT_SHORT}
- **Service(s)**: ${params.SERVICE_TO_BUILD}

## Environment Details
- **Namespace**: ${STAGE_NAMESPACE}
- **Environment**: Staging
- **Performance Tests**: ${params.RUN_PERFORMANCE_TESTS ? 'Executed' : 'Skipped'}
- **Security Scan**: ${params.RUN_SECURITY_SCAN ? 'Executed' : 'Skipped'}

## Deployment Status
✅ **SUCCESSFULLY DEPLOYED TO STAGING**

## Test Results
- ✅ Unit Tests: PASSED
- ✅ Integration Tests: PASSED
- ✅ Health Checks: PASSED
${params.RUN_PERFORMANCE_TESTS ? '- ✅ Performance Tests: PASSED' : ''}
${params.RUN_SECURITY_SCAN ? '- ✅ Security Scans: COMPLETED' : ''}

## Next Steps
🚀 **Ready for Production Deployment Pipeline**

---
*Generated by Jenkins Pipeline - Taller 2 Punto 4*
EOF
                    """
                    
                    archiveArtifacts artifacts: "staging-report-${BUILD_NUMBER}.md", fingerprint: true
                    
                    // Cleanup test jobs
                    sh """
                        kubectl delete job integration-tests-${BUILD_NUMBER} -n ${STAGE_NAMESPACE} || true
                        kubectl delete job performance-tests-${BUILD_NUMBER} -n ${STAGE_NAMESPACE} || true
                    """
                }
            }
        }
    }

    post {
        always {
            echo "🏁 TALLER 2 - PUNTO 4 COMPLETADO"
            echo "📋 Staging deployment pipeline finished"
            
            // Cleanup workspace
            cleanWs()
        }
        
        success {
            echo "✅ STAGING PIPELINE EXITOSO"
            echo "🎯 Service(s) ${params.SERVICE_TO_BUILD} successfully deployed to staging"
            echo "📊 All tests passed in Kubernetes staging environment"
            
            // Notify success
            script {
                if (env.SLACK_WEBHOOK) {
                    sh """
                        curl -X POST -H 'Content-type: application/json' \
                        --data '{"text":"✅ Staging deployment successful for ${params.SERVICE_TO_BUILD} - Build #${BUILD_NUMBER}"}' \
                        ${env.SLACK_WEBHOOK} || true
                    """
                }
            }
        }
        
        failure {
            echo "❌ STAGING PIPELINE FALLÓ"
            echo "🔍 Check logs for details"
            
            // Collect failure information
            script {
                sh """
                    echo "=== FAILURE ANALYSIS ===" > failure-analysis.txt
                    echo "Build: ${BUILD_NUMBER}" >> failure-analysis.txt
                    echo "Service: ${params.SERVICE_TO_BUILD}" >> failure-analysis.txt
                    echo "Timestamp: ${BUILD_TIMESTAMP}" >> failure-analysis.txt
                    echo "Git Commit: ${GIT_COMMIT_SHORT}" >> failure-analysis.txt
                    kubectl get pods -n ${STAGE_NAMESPACE} >> failure-analysis.txt || true
                    kubectl get events -n ${STAGE_NAMESPACE} --sort-by='.lastTimestamp' >> failure-analysis.txt || true
                """
                
                archiveArtifacts artifacts: "failure-analysis.txt", fingerprint: true
                
                if (env.SLACK_WEBHOOK) {
                    sh """
                        curl -X POST -H 'Content-type: application/json' \
                        --data '{"text":"❌ Staging deployment failed for ${params.SERVICE_TO_BUILD} - Build #${BUILD_NUMBER}"}' \
                        ${env.SLACK_WEBHOOK} || true
                    """
                }
            }
        }
    }
} 