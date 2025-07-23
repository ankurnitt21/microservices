
# Microservices Project CI/CD Pipeline


This document outlines the architecture and setup process for the automated CI/CD pipeline for this microservices project. The pipeline is designed to build, containerize, and deploy each microservice independently to a production-like environment.

## 1. CI/CD Architecture Overview

The pipeline leverages GitHub Actions and a self-hosted runner to achieve a robust and secure deployment workflow. The process is triggered by a `git push` to the `master` branch and is filtered by path, ensuring only the modified service is rebuilt and deployed.

The high-level flow is as follows:

```
[Developer] -> git push -> [GitHub Repository]
                                  |
            (Trigger GitHub Actions Workflow)
                                  |
      +---------------------------+------------------------------+
      |                           |                              |
      v                           v                              v
[Job 1: Build & Push]       [Job 2: Deploy]                 [Docker Hub]
(Runs on GitHub Cloud)      (Runs on Self-Hosted Runner)           ^
      |                           |                              |
      +-----> pushes image ------>+------------------------------+
                                  |
                                  +-----> pulls image ----> [Production VM]
                                  |
                                  +--> docker compose up --> [Service Restarted]
```

### Technology Stack
- **CI/CD Platform:** GitHub Actions
- **Containerization:** Docker & Docker Compose
- **Artifact Registry:** Docker Hub
- **Deployment Target:** Ubuntu VM with a Self-Hosted GitHub Runner

---

## 2. Environment Setup Guide

To replicate this setup on a new production server (Ubuntu VM), follow these steps.


### 2.1. Self-Hosted Runner Setup
The runner is the agent that will execute deployment jobs directly on your VM.

1.  **Register the Runner:**
    -   In your GitHub repository, navigate to **`Settings` > `Actions` > `Runners`**.
    -   Click **"New self-hosted runner"** and select **`Linux`**.
    -   Follow the provided `Download` and `Configure` commands on your Ubuntu VM. This will link the runner to your repository.

2.  **Run the Runner as a Service (Recommended):**
    After configuration, install and start the runner as a background service so it runs persistently and starts on boot.

    ```bash
    # Navigate to the runner's directory (e.g., ~/actions-runner)
    cd ~/actions-runner
    
    # Install the service
    sudo ./svc.sh install

    # Start the service
    sudo ./svc.sh start
    ```

3.  **Verify Runner Status:**
    -   **On GitHub:** The runner should appear in the `Runners` list with a green dot and an **"Idle"** status.
    -   **On the VM:** Check the service status.
        ```bash
        sudo ./svc.sh status
        ```
        The output should show **`Active: active (running)`**.

---
Of course. You are right, I completely forgot to include the section on setting up the secrets, which is a critical part of the documentation. My apologies.

Here is the updated section for your `README.md` file that specifically covers how to create and configure the necessary Docker Hub secrets. You can add this as a new section (e.g., Section 3) in the `README.md` file I provided earlier.

---

### 3. Configuring CI/CD Secrets

The CI/CD pipeline requires credentials to authenticate with Docker Hub in order to push the container images. These credentials should **never** be hardcoded into the workflow files. Instead, we use GitHub's encrypted secrets storage.

There are two secrets required for this pipeline to function:

1.  `DOCKERHUB_USERNAME`: Your username for Docker Hub.
2.  `DOCKERHUB_TOKEN`: A Personal Access Token (PAT) generated from Docker Hub.

#### 3.1. How to Create a Docker Hub Access Token

An access token is more secure than using your actual password.

1.  **Log in to Docker Hub:** Navigate to [https://hub.docker.com/](https://hub.docker.com/) and log in.
2.  **Go to Security Settings:** Click on your username in the top-right corner, then select **"Account Settings"**. In the left sidebar, click on **"Security"**.
3.  **Create a New Token:** Click the **"New Access Token"** button.
4.  **Describe the Token:** Give your token a descriptive name that reminds you what it's for, for example, `github-actions-microservices`.
5.  **Set Permissions:** For the pipeline to be able to push images, set the permissions to **"Read & Write"**.
6.  **Generate and Copy:** Click the **"Generate"** button.

> **Important Note:** Docker Hub will only show you this token **ONCE**. Copy the token immediately and store it somewhere safe temporarily. If you lose it, you will have to generate a new one.

#### 3.2. How to Add Secrets to GitHub

Once you have your Docker Hub username and the newly generated access token, add them to your GitHub repository's secrets.

1.  **Navigate to Settings:** In your GitHub repository (`ankurnitt21/microservices`), go to the **`Settings`** tab.
2.  **Go to Actions Secrets:** In the left sidebar, navigate to **`Secrets and variables` > `Actions`**.
3.  **Add the First Secret:**
    *   Click the **"New repository secret"** button.
    *   **Name:** `DOCKERHUB_USERNAME`
    *   **Value:** Enter your Docker Hub username.
    *   Click **"Add secret"**.

4.  **Add the Second Secret:**
    *   Click **"New repository secret"** again.
    *   **Name:** `DOCKERHUB_TOKEN`
    *   **Value:** Paste the Docker Hub access token you copied earlier.
    *   Click **"Add secret"**.

Once both secrets are saved, your pipeline will have the necessary permissions to authenticate with Docker Hub and push your container images automatically.

---

## 4. Workflow Configuration

The core of the CI/CD logic resides in the YAML files within the `.github/workflows/` directory.

### 4.1. Workflow Strategy
- **Independent Workflows:** Each microservice has its own dedicated workflow file (e.g., `user-backend-ci.yml`).
- **Path Filtering:** Each workflow is triggered only when a push includes changes within that service's specific directory (e.g., `paths: - 'user-backend/**'`).
- **Two-Job Approach:**
    1.  **`build-and-push-image`:** Runs on a secure, ephemeral GitHub-hosted machine (`ubuntu-latest`). It builds the Docker image and pushes it to a public registry (Docker Hub).
    2.  **`deploy`:** Runs on the `self-hosted` runner (your VM). It pulls the newly built image and uses Docker Compose to restart only the updated service.

### 4.2. Sample Workflow (`user-backend-ci.yml`)

```yaml
name: CI/CD for User Backend

on:
  push:
    branches:
      - master
    paths:
      - 'user-backend/**'
  workflow_dispatch:

jobs:
  build-and-push-image:
    runs-on: ubuntu-latest
    steps:
      # ... (steps for checkout, login, build, and push)

  deploy:
    runs-on: self-hosted
    needs: build-and-push-image
    steps:
      - name: Deploy to Production Server
        run: |
          cd /home/ankur/Desktop/microservices
          git pull
          docker pull your_dockerhub_username/user-backend:latest
          docker compose up -d --no-deps user-backend
```

---

## 5. Developer Workflow

1.  Create a new feature or bugfix branch from `master`.
2.  Make code changes within a specific service directory (e.g., `product-service/`).
3.  Push the changes to your feature branch.
4.  Create a Pull Request (PR) to merge your branch into `master`.
5.  After the PR is reviewed and approved, merge it.
6.  The merge to `master` will automatically trigger the corresponding CI/CD workflow for the service you changed.

---

## 6. Verification & Monitoring

To verify a successful deployment after a pipeline run:

1.  **Check GitHub Actions:** The workflow run in the "Actions" tab should show green checkmarks (✔) for both jobs.
2.  **Check Docker Hub:** The repository for the deployed service should show an "Updated a few seconds ago" timestamp.
3.  **Check the Server:**
    -   SSH into your Ubuntu VM.
    -   Run `docker ps`. The `STATUS` column for the deployed service should show a recent uptime (e.g., "Up 2 minutes").
    -   Run `docker logs <service-container-name>` to ensure the application started without errors.