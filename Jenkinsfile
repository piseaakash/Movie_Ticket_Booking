// Jenkins pipeline: build, test, Docker build, deploy to Kubernetes
// Requires: Maven, Docker, kubectl on agent. Optional: Docker registry for push.

pipeline {
    agent any
    environment {
        DOCKER_IMAGE_PREFIX = 'ticketing'
        K8S_NAMESPACE = 'ticketing'
        // Set DOCKER_REGISTRY if pushing to a registry (e.g. 'myregistry.io/myrepo')
        // DOCKER_REGISTRY = ''
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build & Test') {
            steps {
                sh 'mvn -B clean package -DskipTests=false'
            }
        }
        stage('Docker Build') {
            parallel {
                stage('auth-service') {
                    steps {
                        sh "docker build --build-arg SERVICE_MODULE=auth-service -t ${DOCKER_IMAGE_PREFIX}/auth-service:${BUILD_NUMBER} -t ${DOCKER_IMAGE_PREFIX}/auth-service:latest -f Dockerfile ."
                    }
                }
                stage('user-service') {
                    steps {
                        sh "docker build --build-arg SERVICE_MODULE=user-service -t ${DOCKER_IMAGE_PREFIX}/user-service:${BUILD_NUMBER} -t ${DOCKER_IMAGE_PREFIX}/user-service:latest -f Dockerfile ."
                    }
                }
                stage('booking-service') {
                    steps {
                        sh "docker build --build-arg SERVICE_MODULE=booking-service -t ${DOCKER_IMAGE_PREFIX}/booking-service:${BUILD_NUMBER} -t ${DOCKER_IMAGE_PREFIX}/booking-service:latest -f Dockerfile ."
                    }
                }
                stage('movie-service') {
                    steps {
                        sh "docker build --build-arg SERVICE_MODULE=movie-service -t ${DOCKER_IMAGE_PREFIX}/movie-service:${BUILD_NUMBER} -t ${DOCKER_IMAGE_PREFIX}/movie-service:latest -f Dockerfile ."
                    }
                }
                stage('payment-service') {
                    steps {
                        sh "docker build --build-arg SERVICE_MODULE=payment-service -t ${DOCKER_IMAGE_PREFIX}/payment-service:${BUILD_NUMBER} -t ${DOCKER_IMAGE_PREFIX}/payment-service:latest -f Dockerfile ."
                    }
                }
                stage('theatre-service') {
                    steps {
                        sh "docker build --build-arg SERVICE_MODULE=theatre-service -t ${DOCKER_IMAGE_PREFIX}/theatre-service:${BUILD_NUMBER} -t ${DOCKER_IMAGE_PREFIX}/theatre-service:latest -f Dockerfile ."
                    }
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def k8sDir = 'k8s'
                    sh """
                        kubectl apply -f ${k8sDir}/namespace.yaml
                        kubectl apply -f ${k8sDir}/secret-template.yaml
                        kubectl apply -f ${k8sDir}/configmap-common.yaml
                        kubectl apply -f ${k8sDir}/postgres.yaml
                        sleep 15
                        kubectl apply -f ${k8sDir}/auth-service/deployment.yaml
                        kubectl apply -f ${k8sDir}/user-service/deployment.yaml
                        kubectl apply -f ${k8sDir}/theatre-service/deployment.yaml
                        kubectl apply -f ${k8sDir}/movie-service/deployment.yaml
                        kubectl apply -f ${k8sDir}/payment-service/deployment.yaml
                        kubectl apply -f ${k8sDir}/booking-service/deployment.yaml
                    """
                }
            }
        }
    }
    post {
        always {
            echo 'Pipeline finished. Check Kubernetes: kubectl get pods -n ticketing'
        }
    }
}
