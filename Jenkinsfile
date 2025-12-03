pipeline {
    agent any

//   tools {
//        jdk    'jdk 24'
//       maven  'Maven 3.9.11'
//  }

    parameters {
        choice(
            name: 'ENV',
            choices: ['staging', 'production'],
            description: 'Where to deploy this build'
        )
        string(
            name: 'ROLLBACK_TAG',
            defaultValue: '',
            description: 'Optional: existing image tag to roll back to'
        )
        string(
            name: 'APP_VERSION',
            defaultValue: '1.0.0',
            description: 'Application version for tagging images'
        )
    }

    environment {
        IMAGE_NAME = "jojodecarlos/coffee-bean-inventory"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test (Maven)') {
            steps {
                bat "mvn -B clean package"
            }
            post {
                success {
                    junit 'target/surefire-reports/*.xml'
                }
                always {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Docker Build, Push & Deploy') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKERHUB_USR', passwordVariable: 'DOCKERHUB_PSW')]) {
                    script {
                        def rollbackTag = params.ROLLBACK_TAG?.trim()
                        def appVersion = params.APP_VERSION?.trim() ?: "1.0.0"

                        // Decide the tag to use (either rollback tag or appVersion)
                        def effectiveTag = rollbackTag ? rollbackTag : appVersion

                        echo "Effective tag to use: ${effectiveTag}"
                        echo "Environment (ENV): ${params.ENV}"
                        echo "Rollback tag param: '${rollbackTag}'"
                        echo "APP_VERSION param: '${appVersion}'"

                        def isRollback = rollbackTag ? true : false

                        if (isRollback) {
                            echo "Rollback requested. Will attempt to pull and retag existing image ${env.IMAGE_NAME}:${rollbackTag}"
                        } else {
                            echo "No rollback tag provided. This is a normal build & push."
                        }

                        echo "Workspace: ${env.WORKSPACE}"
                        echo "Image name: ${env.IMAGE_NAME}"

                        powershell '''
                            $ErrorActionPreference = "Stop"

                            Write-Host "DEBUG: Jenkins WORKSPACE   = $env:WORKSPACE"
                            Write-Host "DEBUG: Jenkins IMAGE_NAME  = $env:IMAGE_NAME"
                            Write-Host "DEBUG: Jenkins APP_VERSION = $env:APP_VERSION"
                            Write-Host "DEBUG: Jenkins ENV         = $env:ENV"
                            Write-Host "DEBUG: Jenkins ROLLBACK_TAG= $env:ROLLBACK_TAG"

                            if ([string]::IsNullOrWhiteSpace($env:IMAGE_NAME)) {
                                throw "IMAGE_NAME is not set in environment! Check Jenkinsfile environment block."
                            }

                            $workspace = $env:WORKSPACE -replace "\\", "/"
                            Write-Host "Normalized Workspace Path: $workspace"

                            $workspacePath = $workspace
                            Write-Host "Using workspace path for Docker context: $workspacePath"

                            $dbHost   = "host.docker.internal"
                            $dbPort   = "3306"
                            $dbUser   = "root"
                            $dbPass   = "coffee_db"
                            $dbName   = "coffee_db"

                            Write-Host ("Database connection string: {0}@{1}:{2}/{3}" -f $dbUser, $dbHost, $dbPort, $dbName)

                            # If ROLLBACK_TAG is set, we do a pull & retag; otherwise build a new image
                            $rollbackTag = $env:ROLLBACK_TAG
                            $appVersion  = $env:APP_VERSION
                            $imageName   = $env:IMAGE_NAME

                            if ([string]::IsNullOrWhiteSpace($appVersion)) {
                                $appVersion = "1.0.0"
                                Write-Host "APP_VERSION not set; defaulting to $appVersion"
                            }

                            if ([string]::IsNullOrWhiteSpace($imageName)) {
                                throw "IMAGE_NAME is empty or null; cannot proceed with Docker operations."
                            }

                            $versionTag = "${imageName}:${appVersion}"
                            $latestTag  = "${imageName}:latest"

                            Write-Host "Computed version tag:  $versionTag"
                            Write-Host "Computed latest tag:   $latestTag"

                            Write-Host "DEBUG: Docker username = $env:DOCKERHUB_USR"
                            Write-Host "DEBUG: Docker image    = $env:IMAGE_NAME"
                            Write-Host ("DEBUG: Will push tags  = {0}:{1} and {0}:latest" -f $env:IMAGE_NAME, $appVersion)

                            if (-not [string]::IsNullOrWhiteSpace($rollbackTag)) {
                                # Rollback scenario: pull existing image and retag
                                $rollbackFullTag = "${imageName}:${rollbackTag}"
                                Write-Host "Rollback requested. Will pull and retag existing image: $rollbackFullTag"

                                docker pull $rollbackFullTag
                                if ($LASTEXITCODE -ne 0) {
                                    throw "Failed to pull existing image $rollbackFullTag"
                                }

                                docker tag $rollbackFullTag $versionTag
                                if ($LASTEXITCODE -ne 0) {
                                    throw "Failed to retag $rollbackFullTag as $versionTag"
                                }

                                docker tag $rollbackFullTag $latestTag
                                if ($LASTEXITCODE -ne 0) {
                                    throw "Failed to retag $rollbackFullTag as latest ($latestTag)"
                                }

                                Write-Host "Rollback image retagged as:"
                                Write-Host " - $versionTag"
                                Write-Host " - $latestTag"
                            }
                            else {
                                # Normal build scenario
                                Write-Host "Building image ${imageName}:$appVersion"
                                Write-Host "Using Docker build context: $workspacePath"

                                docker build `
                                    -f "Dockerfile" `
                                    --build-arg "JAR_FILE=target/coffee-dms-1.0.0-jar-with-dependencies.jar" `
                                    -t "${imageName}:${appVersion}" `
                                    -t "${imageName}:latest" `
                                    "$workspacePath"

                                if ($LASTEXITCODE -ne 0) {
                                    throw "Docker build failed"
                                }
                            }

                            Write-Host "Logging into Docker Hub as $env:DOCKERHUB_USR"
                            docker logout

                            docker login -u $env:DOCKERHUB_USR -p $env:DOCKERHUB_PSW
                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker Hub login failed"
                            }

                            Write-Host "Pushing tags:"
                            Write-Host " - $versionTag"
                            Write-Host " - $latestTag"

                            docker push $versionTag
                            if ($LASTEXITCODE -ne 0) {
                                throw "Failed to push image $versionTag"
                            }

                            docker push $latestTag
                            if ($LASTEXITCODE -ne 0) {
                                throw "Failed to push image $latestTag"
                            }

                            Write-Host "Docker image(s) pushed successfully."

                        '''
                    }
                }
            }
        }
    }
}
