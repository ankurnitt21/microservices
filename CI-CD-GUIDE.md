# 🚀 CI/CD Pipeline Setup Guide

## 📋 What is CI/CD?

**CI/CD** stands for **Continuous Integration/Continuous Deployment**. Think of it as an automated assistant that:

- **Watches your code** for changes
- **Tests your code** automatically when you make changes
- **Builds Docker images** of your updated services
- **Deploys the new version** to your server automatically

**Why is this useful?**
- No manual deployment steps
- Faster releases
- Fewer human errors
- Automatic testing before deployment

## 🏗️ How Our Pipeline Works

```
[You push code] → [GitHub Actions] → [Build Docker Image] → [Deploy to Server]
```

**Detailed Flow:**
1. You make changes to a service (e.g., `user-backend/`)
2. You push the changes to GitHub
3. GitHub Actions detects the change and starts a workflow
4. It builds a new Docker image with your changes
5. It pushes the image to Docker Hub
6. It tells your server to download and run the new image
7. Your service is updated with zero downtime!

## 🎯 Prerequisites

### 1. GitHub Repository
- Your code must be in a GitHub repository
- You need admin access to configure secrets and runners

### 2. Docker Hub Account
- Sign up at [hub.docker.com](https://hub.docker.com)
- We'll use this to store your Docker images

### 3. Deployment Server
- A Linux server (Ubuntu recommended) where your app will run
- Could be AWS EC2, DigitalOcean, or your own server
- Must have Docker installed

## 🔧 Step-by-Step Setup

### Step 1: Configure Docker Hub Secrets

**Why do we need this?** GitHub Actions needs permission to push Docker images to Docker Hub.

1. **Create Docker Hub Access Token:**
   - Go to [Docker Hub](https://hub.docker.com)
   - Login and go to Account Settings → Security
   - Click "New Access Token"
   - Name: `github-actions-microservices`
   - Permissions: "Read & Write"
   - Click "Generate" and **copy the token immediately**

2. **Add Secrets to GitHub:**
   - Go to your GitHub repository
   - Click "Settings" → "Secrets and variables" → "Actions"
   - Click "New repository secret"
   - Add these two secrets:
     - Name: `DOCKERHUB_USERNAME`, Value: your Docker Hub username
     - Name: `DOCKERHUB_TOKEN`, Value: the token you just created

### Step 2: Set Up Self-Hosted Runner

**What is a runner?** It's a program that runs on your server and executes the deployment commands.

1. **On GitHub:**
   - Go to your repository → Settings → Actions → Runners
   - Click "New self-hosted runner"
   - Choose "Linux"
   - Follow the download and configure commands

2. **On Your Server:**
   ```bash
   # Download and configure (commands from GitHub)
   mkdir actions-runner && cd actions-runner
   curl -o actions-runner-linux-x64-2.311.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz
   tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz
   
   # Configure (use the token from GitHub)
   ./config.sh --url https://github.com/YOUR_USERNAME/microservices --token YOUR_TOKEN
   
   # Install as a service so it runs automatically
   sudo ./svc.sh install
   sudo ./svc.sh start
   ```

3. **Verify Setup:**
   - Check GitHub: Runner should show as "Idle" with a green dot
   - Check server: `sudo ./svc.sh status` should show "Active: active (running)"

### Step 3: Create Workflow Files

**What are workflows?** YAML files that tell GitHub Actions what to do when code changes.

Create a `.github/workflows/` directory in your project and add these files:

**File: `.github/workflows/user-backend-ci.yml`**
```yaml
name: CI/CD for User Backend

on:
  push:
    branches: [ master, main ]
    paths: [ 'user-backend/**' ]  # Only run when user-backend changes
  workflow_dispatch:  # Allow manual trigger

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        
      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
          
      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: ./user-backend
          push: true
          tags: ${{ secrets.DOCKERHUB_USERNAME }}/user-backend:latest

  deploy:
    needs: build-and-push
    runs-on: self-hosted
    steps:
      - name: Pull latest code
        run: |
          cd /path/to/your/microservices
          git pull
          
      - name: Update and restart service
        run: |
          cd /path/to/your/microservices
          docker pull ${{ secrets.DOCKERHUB_USERNAME }}/user-backend:latest
          docker-compose up -d --no-deps user-backend
```

**Create similar files for each service:**
- `.github/workflows/product-service-ci.yml`
- `.github/workflows/inventory-service-ci.yml`
- `.github/workflows/order-service-ci.yml`
- `.github/workflows/api-gateway-ci.yml`

Just replace `user-backend` with the appropriate service name in each file.

### Step 4: Update Docker Compose for Production

**Modify your `docker-compose.yml` to use Docker Hub images:**

```yaml
version: '3.8'

services:
  user-backend:
    image: ${DOCKERHUB_USERNAME}/user-backend:latest  # Use environment variable
    container_name: user-backend
    ports:
      - "8080:8080"
    # ... rest of configuration

  product-service:
    image: ${DOCKERHUB_USERNAME}/product-service:latest
    container_name: product-service
    ports:
      - "8081:8081"
    # ... rest of configuration
```

**Create a `.env` file on your server:**
```env
DOCKERHUB_USERNAME=your-dockerhub-username
# ... other environment variables
```

## 🧪 Testing Your Pipeline

### Test 1: Make a Simple Change

1. **Edit a file in `user-backend/`:**
   ```bash
   # Add a comment to UserController.java
   echo "// Pipeline test" >> user-backend/src/main/java/com/example/user_backend/controller/UserController.java
   ```

2. **Commit and push:**
   ```bash
   git add .
   git commit -m "Test CI/CD pipeline"
   git push origin main
   ```

3. **Check GitHub Actions:**
   - Go to your repository → Actions tab
   - You should see a workflow running
   - Wait for it to complete (green checkmark)

4. **Check your server:**
   ```bash
   docker ps  # Should show updated timestamp for user-backend
   docker logs user-backend  # Should show recent startup logs
   ```

### Test 2: Verify Selective Deployment

1. **Edit only `product-service/`:**
   ```bash
   echo "// Another test" >> product-service/src/main/java/com/example/product_service/controller/ProductController.java
   git add .
   git commit -m "Test product service pipeline"
   git push
   ```

2. **Verify only product-service workflow runs:**
   - Check GitHub Actions
   - Only the product-service workflow should trigger
   - Other services should remain unchanged

## 📊 Monitoring Your Deployments

### GitHub Actions Dashboard
- **Repository → Actions** shows all workflow runs
- **Green checkmark** = successful deployment
- **Red X** = failed deployment (click for details)
- **Yellow circle** = currently running

### Server Monitoring
```bash
# Check running containers
docker ps

# Check container logs
docker logs [container-name]

# Check system resources
docker stats

# Check disk space
df -h
```

### Rollback if Needed
```bash
# Rollback to previous version
docker pull your-username/user-backend:previous-tag
docker-compose up -d --no-deps user-backend
```

## 🚨 Troubleshooting Common Issues

### Issue 1: "secrets.DOCKERHUB_USERNAME not found"

**Problem:** GitHub secrets not configured correctly.

**Solution:**
1. Double-check secret names match exactly
2. Verify secrets are set at repository level (not organization)
3. Re-create secrets if needed

### Issue 2: Self-hosted runner offline

**Problem:** Runner shows as offline in GitHub.

**Solution:**
```bash
# Check runner service
sudo ./svc.sh status

# Restart if needed
sudo ./svc.sh stop
sudo ./svc.sh start

# Check logs
sudo journalctl -u actions.runner.* -f
```

### Issue 3: Docker build fails

**Problem:** Docker image build fails in GitHub Actions.

**Solution:**
1. Test build locally first:
   ```bash
   cd user-backend
   docker build -t test-image .
   ```
2. Check Dockerfile syntax
3. Ensure all dependencies are available
4. Check GitHub Actions logs for specific error

### Issue 4: Deployment doesn't update

**Problem:** New image builds but service doesn't update.

**Solution:**
```bash
# Force pull latest image
docker pull your-username/user-backend:latest

# Remove old container and recreate
docker-compose down user-backend
docker-compose up -d user-backend

# Check if image was actually updated
docker images | grep user-backend
```

## 🔧 Advanced Configuration

### Environment-Specific Deployments

**For multiple environments (staging, production):**

```yaml
# In your workflow
- name: Deploy to staging
  if: github.ref == 'refs/heads/develop'
  run: |
    docker-compose -f docker-compose.staging.yml up -d --no-deps user-backend

- name: Deploy to production  
  if: github.ref == 'refs/heads/main'
  run: |
    docker-compose -f docker-compose.prod.yml up -d --no-deps user-backend
```

### Automated Testing

**Add testing before deployment:**

```yaml
- name: Run tests
  run: |
    cd user-backend
    ./mvnw test

- name: Build and push (only if tests pass)
  if: success()
  uses: docker/build-push-action@v5
  # ... rest of build config
```

### Notifications

**Add Slack/Discord notifications:**

```yaml
- name: Notify on success
  if: success()
  run: |
    curl -X POST -H 'Content-type: application/json' \
    --data '{"text":"✅ User Backend deployed successfully!"}' \
    ${{ secrets.SLACK_WEBHOOK_URL }}

- name: Notify on failure
  if: failure()
  run: |
    curl -X POST -H 'Content-type: application/json' \
    --data '{"text":"❌ User Backend deployment failed!"}' \
    ${{ secrets.SLACK_WEBHOOK_URL }}
```

## 🎯 Best Practices

### ✅ Do This:

1. **Test locally first** before pushing
2. **Use specific tags** for production (not just `latest`)
3. **Monitor deployments** and set up alerts
4. **Keep secrets secure** - never commit them to code
5. **Use staging environment** for testing changes
6. **Document your workflows** and keep them simple

### ❌ Avoid This:

1. **Don't deploy directly to production** without testing
2. **Don't hardcode credentials** in workflow files
3. **Don't ignore failed deployments**
4. **Don't deploy all services** for small changes
5. **Don't forget to monitor** resource usage on your server

## 🚀 Production Considerations

### Security
- Use separate credentials for production
- Implement proper access controls
- Regularly rotate secrets and tokens
- Use HTTPS for all communications

### Reliability
- Set up health checks for all services
- Implement proper logging and monitoring
- Have a rollback strategy ready
- Test disaster recovery procedures

### Performance
- Monitor deployment times
- Optimize Docker images for size
- Use multi-stage builds to reduce image size
- Consider using a container registry closer to your server

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Build Push Action](https://github.com/docker/build-push-action)
- [Self-hosted Runners Guide](https://docs.github.com/en/actions/hosting-your-own-runners)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

---

**🎉 Congratulations!** You now have a complete CI/CD pipeline that automatically deploys your microservices whenever you make changes. This is the same type of automation used by major tech companies to deploy code multiple times per day!
