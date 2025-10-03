# 🚀 Complete Microservices Project Setup Guide

## 📋 What Is This Project?

This is a **complete e-commerce microservices system** built with Spring Boot that demonstrates modern cloud-native architecture patterns. Think of it as a mini-Amazon backend system with:

- **User Management** - Create, update, delete users
- **Product Catalog** - Manage products and their details  
- **Inventory System** - Track stock quantities
- **Order Processing** - Place orders, check inventory, calculate pricing
- **Secure Authentication** - Login with your Google account
- **Monitoring & Logging** - See how your system is performing

## 🏗️ System Architecture (Simple Explanation)

```
[You] → [API Gateway] → [Individual Services] → [Database]
                     ↓
              [Monitoring Tools]
```

**What happens when you place an order:**
1. You send a request through the API Gateway (the front door)
2. API Gateway checks if you're logged in (using Google)
3. Order Service checks if the product exists (asks Product Service)
4. Order Service checks if there's enough stock (asks Inventory Service)
5. If everything is OK, your order is saved to the database
6. You get a confirmation back

## 🛠️ Technologies Used (Don't worry, we'll set these up!)

- **Java Spring Boot** - The programming framework
- **PostgreSQL** - The database to store data
- **Docker** - Containers to run everything
- **Google OAuth** - Login with your Google account
- **Prometheus & Grafana** - Monitor system performance
- **ELK Stack** - Collect and view logs

## 📦 Services in This Project

| Service | Port | What It Does |
|---------|------|-------------|
| **API Gateway** | 8085 | Main entrance, handles login |
| **User Service** | 8080 | Manages user accounts |
| **Product Service** | 8081 | Manages product catalog |
| **Inventory Service** | 8082 | Tracks product quantities |
| **Order Service** | 8083 | Processes orders |
| **Config Server** | 8888 | Manages configuration |
| **Service Discovery** | 8761 | Helps services find each other |
| **Database** | 5432 | Stores all data |
| **Prometheus** | 9090 | Collects metrics |
| **Grafana** | 3000 | Shows performance dashboards |

## 🎯 Prerequisites (What You Need Before Starting)

### 1. Install Required Software

**Windows Users:**
- Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Install [Git](https://git-scm.com/download/win)
- Install [Java 17](https://adoptium.net/) (Optional, for development)

**Mac Users:**
- Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Install Git: `brew install git`
- Install Java 17: `brew install openjdk@17`

**Linux Users:**
- Install Docker: `sudo apt install docker.io docker-compose`
- Install Git: `sudo apt install git`
- Install Java 17: `sudo apt install openjdk-17-jdk`

### 2. Create Google OAuth Application

**Why do we need this?** So users can login with their Google account.

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable the "Google+ API"
4. Go to "Credentials" → "Create Credentials" → "OAuth 2.0 Client ID"
5. Choose "Web application"
6. Add these redirect URIs:
   - `http://localhost:8085/login/oauth2/code/google`
   - `http://localhost:8085/swagger-ui/oauth2-redirect.html`
7. Copy your **Client ID** and **Client Secret** (we'll use these later)

## 🚀 Quick Start (Get Everything Running in 10 Minutes)

### Step 1: Download the Project

```bash
# Clone the project
git clone <your-repository-url>
cd microservices

# OR if you already have it, update it
git pull
```

### Step 2: Create Environment File

Create a file called `.env` in the project root folder:

```bash
# Windows
notepad .env

# Mac/Linux  
nano .env
```

Add this content (replace with your actual values):

```env
# Database Configuration
DB_USER=postgres
DB_PASSWORD=yourpassword123
DB_URL=jdbc:postgresql://postgres-db:5432/postgres

# Google OAuth (from Step 2 above)
GOOGLE_CLIENT_ID=your-google-client-id-here
GOOGLE_CLIENT_SECRET=your-google-client-secret-here

# Optional: Kafka credentials (for advanced logging)
KAFKA_BOOTSTRAP_URL=your-kafka-url
KAFKA_API_KEY=your-kafka-key
KAFKA_API_SECRET=your-kafka-secret
```

### Step 3: Start Everything

```bash
# Start all services (this will take 2-3 minutes first time)
docker-compose up -d --build

# Check if everything is running
docker-compose ps
```

You should see all services as "Up" or "healthy".

### Step 4: Test the System

1. **Check API Gateway**: http://localhost:8085
2. **Check Service Discovery**: http://localhost:8761
3. **Check Grafana**: http://localhost:3000 (admin/admin)
4. **Check individual services**:
   - User Service: http://localhost:8080/swagger-ui.html
   - Product Service: http://localhost:8081/swagger-ui.html
   - Order Service: http://localhost:8083/swagger-ui.html

## 🧪 How to Test the System

### Method 1: Using the API Gateway (Recommended)

1. Go to http://localhost:8085/api/users
2. You'll be redirected to Google login
3. Login with your Google account
4. You should see an empty list `[]` (no users yet)

### Method 2: Using Swagger UI (For Developers)

1. First, get an authentication token:
   - Go to http://localhost:8085/token
   - Login with Google
   - Copy the `id_token` value

2. Test individual services:
   - Go to http://localhost:8080/swagger-ui.html
   - Click "Authorize" button
   - Paste your token
   - Try the API endpoints

### Method 3: Create Test Data

```bash
# Add a test user
curl -X POST http://localhost:8085/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com"}'

# Add a test product  
curl -X POST http://localhost:8085/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "price": 999.99, "sku": "LAPTOP001"}'

# Check inventory
curl http://localhost:8085/api/inventory/quantity/LAPTOP001

# Place an order
curl -X POST http://localhost:8085/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "sku": "LAPTOP001", "quantity": 1}'
```

## 📊 Monitoring Your System

### Prometheus (System Metrics)
- URL: http://localhost:9090
- Check "Status" → "Targets" to see all services
- Try queries like: `http_requests_total`, `jvm_memory_used_bytes`

### Grafana (Pretty Dashboards)
- URL: http://localhost:3000
- Login: admin/admin
- Add Prometheus data source: `http://prometheus:9090`
- Import dashboard ID: `12900` for Spring Boot metrics

## 🔍 Troubleshooting Common Issues

### Problem: Services won't start
**Solution:**
```bash
# Check what's wrong
docker-compose logs [service-name]

# Restart everything
docker-compose down
docker-compose up -d --build
```

### Problem: "Connection refused" errors
**Solution:**
- Make sure Docker Desktop is running
- Check if ports are already in use: `netstat -an | grep 8080`
- Wait longer - services need time to start up

### Problem: Google login doesn't work
**Solution:**
- Double-check your `.env` file has correct Google credentials
- Verify redirect URIs in Google Cloud Console
- Make sure you're using `http://localhost:8085` (not 127.0.0.1)

### Problem: Database connection errors
**Solution:**
- Check your database password in `.env` file
- Wait for PostgreSQL to fully start (check with `docker-compose logs postgres-db`)

## 📁 Project Structure Explained

```
microservices/
├── api-gateway/          # Main entrance (port 8085)
├── user-backend/         # User management (port 8080)  
├── product-service/      # Product catalog (port 8081)
├── inventory-service/    # Stock tracking (port 8082)
├── order-service/        # Order processing (port 8083)
├── config-server/        # Configuration management
├── service-discovery/    # Service registry
├── prometheus/           # Monitoring config
├── elk-stack-configs/    # Logging config
├── docker-compose.yml    # Main services
├── docker-compose-elk.yml # Logging services
├── .env                  # Your secrets (create this!)
└── README.md            # This file
```

## 🎓 Learning Path (What to Study Next)

### Beginner Level
1. **Docker Basics** - Understanding containers
2. **REST APIs** - How services communicate
3. **Databases** - How data is stored

### Intermediate Level  
1. **Spring Boot** - The Java framework used
2. **Microservices Patterns** - Why we split things up
3. **OAuth/JWT** - How authentication works

### Advanced Level
1. **Service Mesh** - Advanced service communication
2. **Kubernetes** - Container orchestration
3. **CI/CD Pipelines** - Automated deployment

## 🚀 Next Steps

Once you have everything running:

1. **Explore the APIs** - Try all the endpoints
2. **Check the monitoring** - See how Prometheus and Grafana work
3. **Read the detailed guides**:
   - [OIDC Authentication Guide](./OIDC-COMPLETE.md)
   - [ELK Logging Guide](./ELK-COMPLETE.md)
   - [Monitoring Guide](./MONITORING-COMPLETE.md)
4. **Modify the code** - Try adding new features
5. **Deploy to cloud** - AWS, GCP, or Azure

## 🆘 Getting Help

- **GitHub Issues** - Report bugs or ask questions
- **Documentation** - Check the detailed `.md` files
- **Logs** - Always check `docker-compose logs [service-name]`
- **Community** - Spring Boot and Docker communities are very helpful

## 📝 License

This project is for educational purposes. Feel free to use it for learning and experimentation!

---

**🎉 Congratulations!** You now have a complete microservices system running on your machine. This is the same type of architecture used by companies like Netflix, Amazon, and Uber!
