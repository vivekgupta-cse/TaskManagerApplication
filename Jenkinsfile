pipeline {
    agent any

    // NOTE: Removed the 'tools { jdk 'jdk-25' }' block to make the Jenkinsfile portable.
    // If you prefer to use a named JDK installation, configure it in "Manage Jenkins -> Global Tool Configuration"
    // and add a tools { jdk 'your-jdk-name' } block back.

    environment {
        GCP_PROJECT_ID     = "task-manager-demo-489313"
        REGION             = "us-central1"
        REPO               = "task-repo"
        IMAGE_NAME         = "task-manager"
        CLOUD_RUN_SERVICE  = "task-manager"
        ARTIFACT_REGISTRY  = "${REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/${REPO}/${IMAGE_NAME}"
        GCP_CREDENTIALS_ID = "gcp-service-account-json"
        GITHUB_REPO        = "https://github.com/vivekgupta-cse/TaskManagerApplication.git"
        GITHUB_BRANCH      = "main"
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: "${GITHUB_BRANCH}",
                    url: "${GITHUB_REPO}"
            }
        }

        stage('Build Jar') {
            steps {
                // Print Java info to help debugging environments that don't have a named JDK tool configured
                sh 'echo "=== java info ===" && java -version || true && which java || true'
                sh 'chmod +x ./gradlew || true'
                sh './gradlew clean bootJar -x test --no-daemon --console=plain'
            }
        }

        stage('Test') {
            steps {
                withCredentials([
                    string(credentialsId: 'test-db-url',      variable: 'TEST_DB_URL'),
                    string(credentialsId: 'test-db-username', variable: 'TEST_DB_USERNAME'),
                    string(credentialsId: 'test-db-password', variable: 'TEST_DB_PASSWORD')
                ]) {
                    sh '''
                        export SPRING_DATASOURCE_URL=$TEST_DB_URL
                        export SPRING_DATASOURCE_USERNAME=$TEST_DB_USERNAME
                        export SPRING_DATASOURCE_PASSWORD=$TEST_DB_PASSWORD
                        ./gradlew test --no-daemon --console=plain
                    '''
                }
            }
            post {
                always {
                    junit '**/build/test-results/test/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                // Use Dockerfile in docker_scripts/ but keep project root as context so build/libs/*.jar is reachable
                sh "docker build -f docker_scripts/Dockerfile -t ${ARTIFACT_REGISTRY}:${env.BUILD_ID} -t ${ARTIFACT_REGISTRY}:latest ."
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
                    file(credentialsId: "${GCP_CREDENTIALS_ID}", variable: 'GCP_KEY'),
                    string(credentialsId: 'db-url',              variable: 'DB_URL'),
                    string(credentialsId: 'db-username',         variable: 'DB_USERNAME'),
                    string(credentialsId: 'db-password',         variable: 'DB_PASSWORD')
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

        stage('Prune Old Images') {
            steps {
                withCredentials([file(credentialsId: "${GCP_CREDENTIALS_ID}", variable: 'GCP_KEY')]) {
                    sh """
                        echo "--- Cleaning up old images in ${ARTIFACT_REGISTRY} ---"
                        gcloud auth activate-service-account --key-file=\$GCP_KEY --quiet

                        # List all images with their digests, sort by creation time (newest first), skip the latest 5, and construct full image references
                        IMAGES_TO_DELETE=\$(gcloud artifacts docker images list ${ARTIFACT_REGISTRY} \\
                            --sort-by=~CREATE_TIME \\
                            --format="table(IMAGE,DIGEST)" \\
                            --limit=unlimited | \\
                            tail -n +2 | \\
                            awk '{print \$1"@"\$2}' | \\
                            tail -n +6)

                        if [ -z "\$IMAGES_TO_DELETE" ]; then
                            echo "No old images to delete. Found fewer than 5 images."
                        else
                            echo "The following images will be deleted:"
                            echo "\$IMAGES_TO_DELETE"
                            # Delete each image one by one
                            echo "\$IMAGES_TO_DELETE" | while read IMAGE; do
                                echo "Deleting \$IMAGE"
                                gcloud artifacts docker images delete "\$IMAGE" --delete-tags --quiet || echo "Failed to delete \$IMAGE"
                            done
                            echo "--- Cleanup complete ---"
                        fi
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