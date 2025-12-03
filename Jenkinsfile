pipeline {
    agent any

    // tools {
    //     jdk    "JDK17"
    //     maven  "Maven3"
    // }

    environment {
        // Pipeline-level environment variables
        // NOTE: Credentials are injected via withCredentials in the Docker stage
        MAVEN_HOME = "C:\\Program Files\\Apache\\apache-maven-3.9.11-bin\\apache-maven-3.9.11"
        PATH       = "${MAVEN_HOME}\\bin;${PATH}"
        // Default DB connection values (can be overridden per-environment)
        DB_HOST    = "localhost"
        DB_PORT    = "3306"
        DB_NAME    = "coffee_inventory_db"
        JDBC_URL   = "jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=UTC"
    }

    parameters {
        choice(
            name: 'ENV',
            choices: ['dev', 'prod'],
            description: 'Select deployment environment (dev or prod)'
        )
        string(
            name: 'ROLLBACK_TAG',
            defaultValue: '',
            description: 'Docker image tag to roll back to (optional)'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test (Maven)') {
            steps {
                bat """
                    echo Using Maven Home: %MAVEN_HOME%
                    mvn -B clean package
                """
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build, Push & Deploy') {
            when {
                expression {
                    params.ENV == 'dev' || params.ENV == 'prod'
                }
            }
            steps {
                script {
                    // Use username+password credential (ID must match Jenkins configuration)
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKERHUB_USR',
                        passwordVariable: 'DOCKERHUB_PSW'
                    )]) {

                        // Resolve app version from Maven POM
                        def appVersion = bat(
                            script: '"%MAVEN_HOME%\\bin\\mvn" -q -Dexec.cleanupDaemonThreads=false ' +
                                    '-Dexec.mainClass=none ' +
                                    '-Dexec.args=none ' +
                                    '-Dexec.classpathScope=runtime ' +
                                    'help:evaluate -Dexpression=project.version -DforceStdout',
                            returnStdout: true
                        ).trim()

                        echo "Resolved App Version: ${appVersion}"

                        // Choose final version tag (use ROLLBACK_TAG if provided)
                        def versionTag = params.ROLLBACK_TAG?.trim()
                        if (!versionTag) {
                            versionTag = appVersion
                        }

                        // Normalize environment for image naming
                        def envName = params.ENV.toLowerCase()

                        // Build Docker image name with environment
                        def imageName = "jojodecarlos/coffee-bean-inventory"
                        def fullTag   = "${imageName}:${versionTag}"
                        def latestTag = "${imageName}:latest"

                        echo "Using image name: ${imageName}"
                        echo "Version tag to deploy: ${versionTag}"
                        echo "ENV selected: ${envName}"

                        // Verify JAR exists
                        def jarFile = "target/coffee-dms-${appVersion}-jar-with-dependencies.jar"
                        if (!fileExists(jarFile)) {
                            error "JAR file not found: ${jarFile}. Maven build may have failed or artifact path is incorrect."
                        }

                        echo "Using JAR for Docker build: ${jarFile}"
                        echo "Docker build context: ${pwd()}"

                        // Switch to PowerShell for Docker build, login, and push
                        powershell """
                            Write-Host "=== Docker Build, Push & Deploy Pipeline ==="

                            # Show basic environment info
                            Write-Host "Current directory: $(Get-Location)"
                            Write-Host "Listing key files in workspace:"
                            Get-ChildItem -Recurse -Include coffee-dms-*-jar-with-dependencies.jar | ForEach-Object {
                                Write-Host "  Found JAR: " \$_.FullName
                            }

                            # Show jar file path that Jenkins resolved
                            \$jarFile   = "${jarFile}"
                            \$imageName = "${imageName}"
                            \$versionTag = "${versionTag}"
                            \$fullTag   = "${fullTag}"
                            \$latestTag = "${latestTag}"

                            Write-Host "Jar file to use: \$jarFile"
                            Write-Host "Image name: \$imageName"
                            Write-Host "Full tag: \$fullTag"
                            Write-Host "Latest tag: \$latestTag"

                            if (-Not (Test-Path \$jarFile)) {
                                Write-Host "ERROR: JAR file not found at path: \$jarFile"
                                exit 1
                            }

                            Write-Host "Building Docker image..."
                            docker build `
                                -f "Dockerfile" `
                                --build-arg "JAR_FILE=\$jarFile" `
                                -t "\$fullTag" `
                                -t "\$latestTag" `
                                "${pwd}"

                            if (\$LASTEXITCODE -ne 0) {
                                throw "Docker build failed"
                            }

                            Write-Host "Docker build completed successfully."

                            # Debug: Show docker CLI and env vars before login
                            Write-Host "Docker CLI version:"
                            docker --version

                            Write-Host "Docker username from env: \$env:DOCKERHUB_USR"
                            if (\$env:DOCKERHUB_PSW) {
                                Write-Host "Docker token length: \$(\$env:DOCKERHUB_PSW.Length) characters"
                            } else {
                                Write-Host "Docker token is NOT set in environment!"
                            }

                            Write-Host "Logging in to Docker Hub as: \$env:DOCKERHUB_USR"

                            # Use the password from Jenkins credential
                            \$env:DOCKERHUB_PSW | docker login -u "\$env:DOCKERHUB_USR" --password-stdin

                            if (\$LASTEXITCODE -ne 0) {
                                throw "Docker Hub login failed"
                            }

                            Write-Host "Docker Hub login succeeded. Pushing image..."

                            docker push "\$fullTag"
                            if (\$LASTEXITCODE -ne 0) {
                                throw "Failed to push image with tag \$fullTag"
                            }

                            docker push "\$latestTag"
                            if (\$LASTEXITCODE -ne 0) {
                                throw "Failed to push image with tag \$latestTag"
                            }

                            Write-Host "Docker image push completed successfully."

                            # (Optional) Deploy step would go here (e.g., docker run / kubectl apply)
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline completed for ENV=${params.ENV}, ROLLBACK_TAG=${params.ROLLBACK_TAG}"
        }
    }
}
