pipeline {
    agent any

    environment {
        // 🔧 À adapter avec vos valeurs
        DOCKER_HUB_USER    = 'votre-dockerhub-username'
        IMAGE_NAME         = 'nom-de-votre-microservice'
        DOCKER_CREDENTIALS = 'dockerhub-credentials'   // ID dans Jenkins Credentials
        SONAR_TOKEN        = credentials('sonar-token') // ID dans Jenkins Credentials
        SONAR_HOST_URL = 'http://sonarqube:9000'
    }

    tools {
        maven 'Maven-3.9'   // Nom configuré dans Jenkins Global Tools
        jdk   'JDK-17'      // Nom configuré dans Jenkins Global Tools
    }

    stages {

        stage('📥 Checkout') {
            steps {
                checkout scm
                echo "✅ Code récupéré depuis : ${env.GIT_URL}"
            }
        }

        stage('🔨 Build & Unit Tests') {
            steps {
                sh 'mvn clean package -B'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('🔍 SonarQube Analysis') {
            steps {
                sh """
                    mvn sonar:sonar \
                        -Dsonar.projectKey=${IMAGE_NAME} \
                        -Dsonar.projectName=${IMAGE_NAME} \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.token=${SONAR_TOKEN}
                """
            }
        }

        stage('🐳 Docker Build') {
            steps {
                sh "docker build -t ${DOCKER_HUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER} ."
                sh "docker tag ${DOCKER_HUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER} ${DOCKER_HUB_USER}/${IMAGE_NAME}:latest"
            }
        }

        stage('📤 Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "${DOCKER_CREDENTIALS}",
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                    sh "echo $PASS | docker login -u $USER --password-stdin"
                    sh "docker push ${DOCKER_HUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER}"
                    sh "docker push ${DOCKER_HUB_USER}/${IMAGE_NAME}:latest"
                }
            }
        }

        stage('🧹 Cleanup') {
            steps {
                sh "docker rmi ${DOCKER_HUB_USER}/${IMAGE_NAME}:${BUILD_NUMBER} || true"
                sh "docker rmi ${DOCKER_HUB_USER}/${IMAGE_NAME}:latest || true"
            }
        }
    }

    post {
        success {
            echo '🎉 Pipeline terminé avec succès !'
        }
        failure {
            echo '❌ Pipeline échoué. Vérifiez les logs.'
        }
    }
}