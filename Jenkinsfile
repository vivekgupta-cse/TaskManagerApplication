pipeline {
    agent any

    environment {
        GCP_PROJECT_ID    = "task-manager-demo-489313"
        REGION            = "us-central1"
        REPO              = "task-repo"
        IMAGE_NAME        = "task-manager"
        CLOUD_RUN_SERVICE = "task-manager"
        ARTIFACT_REGISTRY = "${REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/${REPO}/${IMAGE_NAME}"
        GCP_CREDENTIALS_ID = "gcp-service-account-json"
        GITHUB_REPO       = "https://github.com/vivekgupta-cse/TaskManagerApplication.git"
        GITHUB_BRANCH     = "main"
    }

    stages {

        stage('Checkout') {
            steps {
                // Always pull fresh from GitHub — never use local developer workspace
                git branch: "${GITHUB_BRANCH}",
                    url: "${GITHUB_REPO}"
            }
        }

        stage('Build Jar') {
            steps {
                // Build the fat jar, skip tests here — tests run in the next stage
                sh './gradlew clean bootJar -x test'
            }
        }

        stage('Test') {
            steps {
                // Tests require a running Postgres — ensure docker-compose-postgres-test.yml
                // is started on the Jenkins agent before this stage runs.
                sh './gradlew test'
            }
            post {
                always {
                    junit '**/build/test-results/test/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${ARTIFACT_REGISTRY}:${env.BUILD_ID} -t ${ARTIFACT_REGISTRY}:latest ."
            }
        }

        stage('Push to Artifact Registry') {
            steps {
                withCredentials([file(credentialsId: "${GCP_CREDENTIALS_ID}", variable: 'GCP_KEY')]) {
                    sh """
                        gcloud auth activate-service-account --key-file=\$GCP_KEY
                        gcloud auth configure-docker ${REGION}-docker.pkg.dev --quiet
                        docker push ${ARTIFACT_REGISTRY}:${env.BUILD_ID}
                        docker push ${ARTIFACT_REGISTRY}:latest
                    """
                }
            }
        }

        stage('Deploy to Cloud Run') {
            steps {
                withCredentials([
                    file(credentialsId: "${GCP_CREDENTIALS_ID}",  variable: 'GCP_KEY'),
                    string(credentialsId: 'db-url',               variable: 'DB_URL'),
                    string(credentialsId: 'db-username',          variable: 'DB_USERNAME'),
                    string(credentialsId: 'db-password',          variable: 'DB_PASSWORD')
                ]) {
                    sh """
                        gcloud auth activate-service-account --key-file=\$GCP_KEY
                        gcloud run deploy ${CLOUD_RUN_SERVICE} \
                            --image ${ARTIFACT_REGISTRY}:${env.BUILD_ID} \
                            --region ${REGION} \
                            --project ${GCP_PROJECT_ID} \
                            --port 9090 \
                            --platform managed \
                            --allow-unauthenticated \
                            --set-env-vars="SPRING_DATASOURCE_URL=\${DB_URL},SPRING_DATASOURCE_USERNAME=\${DB_USERNAME},SPRING_DATASOURCE_PASSWORD=\${DB_PASSWORD}"
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Deployed successfully from branch ${GITHUB_BRANCH} (build #${env.BUILD_ID})"
        }
        failure {
            echo "Pipeline failed — check logs above."
        }
    }
}