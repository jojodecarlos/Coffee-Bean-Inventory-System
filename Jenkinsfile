pipeline {
    agent any

    parameters {
        string(
            name: 'APP_VERSION',
            defaultValue: '1.0.0',
            description: 'Application version tag for the Docker image'
        )
    }

    environment {
        DOCKER_IMAGE_NAME = 'jojodecarlos/coffee-bean-inventory'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test (Maven)') {
            steps {
                bat 'mvn -B clean package'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build, Push & Deploy') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'dockerhub-pat',   // Secret Text credential (your Docker Hub PAT)
                        variable: 'DOCKERHUB_TOKEN'
                    )
                ]) {
                    powershell '''
                        Write-Host "=== Docker Build, Push & Deploy Pipeline ==="

                        # Resolve app version
                        if (-not $env:APP_VERSION -or $env:APP_VERSION.Trim() -eq "") {
                            $appVersion = "1.0.0"
                        } else {
                            $appVersion = $env:APP_VERSION.Trim()
                        }
                        Write-Host "Resolved App Version: $appVersion"

                        # JAR path (fat JAR created by Maven Assembly plugin)
                        $jarPath = "target/coffee-dms-1.0.0-jar-with-dependencies.jar"
                        if (-not (Test-Path $jarPath)) {
                            throw "JAR file not found at $jarPath. Make sure Maven build produced the fat JAR."
                        }
                        Write-Host "Using JAR for Docker build: $jarPath"

                        # Docker image tags
                        $imageName  = $env:DOCKER_IMAGE_NAME
                        $context    = (Get-Location).Path
                        $versionTag = "$($imageName):$appVersion"
                        $latestTag  = "$($imageName):latest"

                        Write-Host "Docker build context: $context"
                        Write-Host "Building image tags: $versionTag and $latestTag"

                        # Try to logout first (ignore failures)
                        docker logout *>$null

                        # Build image
                        docker build -f "Dockerfile" `
                                     --build-arg "JAR_FILE=$jarPath" `
                                     -t "$versionTag" `
                                     -t "$latestTag" `
                                     "$context"

                        if ($LASTEXITCODE -ne 0) {
                            throw "Docker build failed"
                        }

                        # Docker Hub login using PAT via --password-stdin
                        $dockerUser = "jojodecarlos"
                        Write-Host "Logging into Docker Hub as $dockerUser ..."
                        $env:DOCKERHUB_TOKEN | docker login -u $dockerUser --password-stdin

                        if ($LASTEXITCODE -ne 0) {
                            throw "Docker Hub login failed – check the PAT in Jenkins credentials."
                        }

                        # Push versioned tag
                        docker push "$versionTag"
                        if ($LASTEXITCODE -ne 0) {
                            throw "Docker push failed for $versionTag"
                        }

                        # Push latest tag
                        docker push "$latestTag"
                        if ($LASTEXITCODE -ne 0) {
                            throw "Docker push failed for $latestTag"
                        }

                        # Logout
                        docker logout *>$null
                        Write-Host "✅ Successfully pushed $versionTag and $latestTag to Docker Hub."
                    '''
                }
            }
        }
    }
}
