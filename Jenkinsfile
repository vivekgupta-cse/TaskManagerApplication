pipeline {
    agent any
    environment {
        GCP_PROJECT_ID = "task-manager-demo-489313"
        ARTIFACT_REGISTRY = "us-central1-docker.pkg.dev/${GCP_PROJECT_ID}/task-repo/task-app"
        GCP_CREDENTIALS_ID = "gcp-service-account-json" // ID of your credential in Jenkins
    }
    stages {
        stage('Compile & Test') {
            steps {
                sh './gradlew clean build'
            }
        }
        stage('Dockerize') {
            steps {
                script {
                    // Build the image locally on Jenkins
                    sh "docker build -t ${ARTIFACT_REGISTRY}:${env.BUILD_ID} ."
                }
            }
        }
        stage('Push to Artifact Registry') {
            steps {
                withCredentials([file(credentialsId: "${GCP_CREDENTIALS_ID}", variable: 'GCP_KEY')]) {
                    sh "cat $GCP_KEY | docker login -u _json_key --password-stdin https://us-central1-docker.pkg.dev"
                    sh "docker push ${ARTIFACT_REGISTRY}:${env.BUILD_ID}"
                }
            }
        }
        stage('Deploy to Cloud Run') {
            steps {
                withCredentials([file(credentialsId: "${GCP_CREDENTIALS_ID}", variable: 'GCP_KEY')]) {
                    sh "gcloud auth activate-service-account --key-file=$GCP_KEY"
                    sh "gcloud run deploy task-app --image ${ARTIFACT_REGISTRY}:${env.BUILD_ID} --platform managed --region us-central1 --allow-unauthenticated"
                }
            }
        }
    }
}