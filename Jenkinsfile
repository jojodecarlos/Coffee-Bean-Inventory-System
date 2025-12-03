pipeline {
    agent any

    // tools {
    //     jdk    'jdk21'
    //     maven  'Maven 3.9.11'
    // }

    parameters {
        choice(
            name: 'ENV',
            choices: ['staging', 'production'],
            description: 'Which environment to deploy to'
        )
        string(
            name: 'ROLLBACK_TAG',
            defaultValue: '',
            description: 'Optional: tag to deploy instead of new build (e.g., 1.0.0)'
        )
    }

    environment {
        IMAGE_NAME = 'jojodecarlos/coffee-bean-inventory'
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
                junit '**/target/surefire-reports/*.xml'
            }
        }

        stage('Docker Build, Push & Deploy') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKERHUB_USR',
                        passwordVariable: 'DOCKERHUB_PSW'
                    )
                ]) {
                    powershell '''
$ErrorActionPreference = "Stop"
Write-Host "=== Docker Build, Push & Deploy Pipeline ==="

$APP_VERSION = (& mvn help:evaluate "-Dexpression=project.version" -q "-DforceStdout").Trim()
if (-not $APP_VERSION) { throw "Could not read project.version from POM" }

Write-Host "Resolved App Version: $APP_VERSION"

$jar = Get-ChildItem -Path "target" -Filter *.jar | Select-Object -First 1
if (-not $jar) { throw "No JAR found under target/. Did Maven build succeed?" }

$relJar = Resolve-Path -Relative $jar.FullName
$relJar = $relJar -replace "^[.][\\\\/]", ""   # remove leading .\\
$relJar = $relJar -replace "\\\\", "/"         # convert slashes

Write-Host "Using JAR for Docker build: $relJar"

Set-Location -Path "$env:WORKSPACE"
$contextPath = (Get-Location).Path

Write-Host "Docker build context: $contextPath"

$imageName   = $env:IMAGE_NAME
$versionTag  = "$imageName:$APP_VERSION"
$latestTag   = "$imageName:latest"

$dockerCmd = "docker build -f ""Dockerfile"" --build-arg ""JAR_FILE=$relJar"" -t ""$versionTag"" -t ""$latestTag"" ""$contextPath"""

Write-Host "Running: $dockerCmd"

cmd /c $dockerCmd

if ($LASTEXITCODE -ne 0) { throw "Docker build failed" }






$env:DOCKERHUB_PSW | docker login -u "$env:DOCKERHUB_USR" --password-stdin
if ($LASTEXITCODE -ne 0) { throw "Docker Hub login failed" }

docker push "$env:IMAGE_NAME:$APP_VERSION"
if ($LASTEXITCODE -ne 0) { throw "Push failed ($APP_VERSION)" }

docker push "$env:IMAGE_NAME:latest"
if ($LASTEXITCODE -ne 0) { throw "Push failed (:latest)" }

docker logout
Write-Host "Pushed tags $APP_VERSION and latest"

if ($env:ROLLBACK_TAG -and $env:ROLLBACK_TAG.Trim() -ne "") {
    $TAG = $env:ROLLBACK_TAG.Trim()
    Write-Host "⚠️ Rollback mode: deploying tag $TAG"
}
else {
    $TAG = $APP_VERSION
    Write-Host "Deploying newly built tag $TAG"
}

switch ($env:ENV) {
    "staging" {
        $PORT = 8081
        $NAME = "coffee-inventory-staging"
    }
    "production" {
        $PORT = 8080
        $NAME = "coffee-inventory"
    }
    default { throw "Invalid ENV parameter: $env:ENV" }
}

Write-Host "Environment: $($env:ENV)"
Write-Host "Container name: $NAME"
Write-Host "Port: $PORT"

$DB_HOST     = "host.docker.internal"
$DB_PORT     = "3306"
$DB_NAME     = "coffee_db"
$DB_USER     = "coffee_user"
$DB_PASSWORD = "coffee_pass"
$JDBC_URL    = "jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

docker rm -f $NAME | Out-Null 2>&1

docker run -d `
  --name $NAME `
  -p ${PORT}:8080 `
  --restart unless-stopped `
  -e "SPRING_DATASOURCE_URL=$JDBC_URL" `
  -e "SPRING_DATASOURCE_USERNAME=$DB_USER" `
  -e "SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD" `
  "$env:IMAGE_NAME:$TAG"

if ($LASTEXITCODE -ne 0) { throw "docker run failed: $env:IMAGE_NAME:$TAG" }

Start-Sleep -Seconds 3
docker ps --filter "name=$NAME"

Write-Host "✅ Successfully deployed $env:IMAGE_NAME:$TAG → $env:ENV at http://localhost:$PORT"
'''
                }
            }
        }

    }
}
