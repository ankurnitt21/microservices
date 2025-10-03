# 📊 Complete Monitoring Setup Guide (Prometheus & Grafana)

## 🤔 What is Monitoring and Why Do We Need It?

**Simple Explanation:**
Imagine you're managing a busy restaurant. You want to know:
- How many customers are we serving per hour?
- How long are customers waiting for their food?
- Is the kitchen running out of ingredients?
- Are any of our chefs getting overwhelmed?

**Monitoring your microservices is similar** - we want to track:
- How many API requests per second?
- How fast are responses?
- Is any service using too much memory?
- Are there any errors happening?

**Prometheus + Grafana = Your Restaurant Dashboard:**
- **Prometheus** = The data collector (like a waiter taking notes)
- **Grafana** = The pretty dashboard (like a TV screen showing all the stats)

## 🏗️ How It Works (Simple Version)

```
[Your Apps] → [Generate Metrics] → [Prometheus Collects] → [Grafana Displays]
```

**Step-by-step:**
1. Your apps automatically track things like request count, response time, memory usage
2. Prometheus visits each app every 15 seconds and asks "How are you doing?"
3. Prometheus stores all this data in its database
4. Grafana reads from Prometheus and shows beautiful charts and graphs

## 🚀 Quick Start (Get Monitoring Running in 5 Minutes)

### Step 1: Start Everything

```bash
# Start your microservices (if not already running)
docker-compose up -d --build

# Check that Prometheus and Grafana are running
docker-compose ps | grep -E "(prometheus|grafana)"
```

You should see:
- `prometheus` on port 9090
- `grafana` on port 3000

### Step 2: Check Prometheus is Collecting Data

1. **Open Prometheus:** http://localhost:9090
2. **Check targets:** Go to "Status" → "Targets"
3. **You should see all your services as "UP":**
   - user-backend:8080
   - product-service:8081
   - inventory-service:8082
   - order-service:8083
   - api-gateway:8085

If any show as "DOWN", wait 1-2 minutes or check the troubleshooting section.

### Step 3: Set Up Grafana Dashboard

1. **Open Grafana:** http://localhost:3000
2. **Login:** 
   - Username: `admin`
   - Password: `admin`
   - (You'll be asked to change the password - choose something you'll remember)

3. **Add Prometheus Data Source:**
   - Click the gear icon (⚙️) on the left → "Data Sources"
   - Click "Add data source"
   - Choose "Prometheus"
   - URL: `http://prometheus:9090`
   - Click "Save & test" (should show green checkmark)

4. **Import a Dashboard:**
   - Click the "+" icon on the left → "Import"
   - In the "Import via grafana.com" box, enter: `12900`
   - Click "Load"
   - Choose your Prometheus data source from the dropdown
   - Click "Import"

**🎉 You're done!** You should now see a beautiful dashboard with charts showing your microservices performance.

## 📊 Understanding Your Dashboard

### Key Metrics Explained:

**1. HTTP Requests per Second**
- Shows how busy your services are
- Higher = more traffic
- Sudden spikes might indicate problems or high load

**2. Response Time (Latency)**
- How long APIs take to respond
- Lower is better (under 200ms is great)
- Spikes indicate performance issues

**3. Memory Usage**
- How much RAM each service is using
- Steadily increasing = possible memory leak
- Sudden jumps = service under stress

**4. CPU Usage**
- How much processing power is being used
- High CPU = service working hard
- 100% CPU = service might be overloaded

**5. Error Rate**
- Percentage of requests that failed
- Should be close to 0%
- Spikes indicate bugs or system issues

### What Good vs Bad Looks Like:

**✅ Healthy System:**
- Steady request rate with normal fluctuations
- Response times under 500ms
- Memory usage stable or slowly growing
- Error rate under 1%

**⚠️ Problems to Watch For:**
- Response times suddenly jumping to seconds
- Memory continuously growing (memory leak)
- Error rate above 5%
- CPU constantly at 100%

## 🛠️ Detailed Setup (Understanding the Configuration)

### How Services Expose Metrics

Each Spring Boot service automatically provides metrics at `/actuator/prometheus`. 

**Check it yourself:**
```bash
# See raw metrics from user service
curl http://localhost:8080/actuator/prometheus

# You'll see lines like:
# http_server_requests_seconds_count{method="GET",uri="/api/users"} 5.0
# jvm_memory_used_bytes{area="heap"} 123456789.0
```

### Prometheus Configuration

**File:** `prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s  # How often to collect data

scrape_configs:
  - job_name: 'spring-micrometer-services'
    metrics_path: '/actuator/prometheus'  # Where to get metrics
    static_configs:
      - targets:
        - 'user-backend:8080'      # Service name:port
        - 'product-service:8081'
        - 'inventory-service:8082'
        - 'order-service:8083'
        - 'api-gateway:8085'
```

**What this means:**
- Every 15 seconds, Prometheus visits each service
- It asks for metrics at `/actuator/prometheus`
- It stores all the data for Grafana to use

### Docker Compose Configuration

**In your `docker-compose.yml`:**

```yaml
prometheus:
  image: prom/prometheus:latest
  container_name: prometheus
  ports:
    - "9090:9090"
  networks:
    - shared-network
  volumes:
    - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'

grafana:
  image: grafana/grafana:latest
  container_name: grafana
  ports:
    - "3000:3000"
  networks:
    - shared-network
  volumes:
    - grafana_data:/var/lib/grafana
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
  depends_on:
    - prometheus
```

## 🧪 Testing Your Monitoring

### Generate Some Load

```bash
# Make lots of requests to see metrics change
for i in {1..100}; do
  curl http://localhost:8085/api/users
  curl http://localhost:8085/api/products
  sleep 0.1
done
```

### Watch the Dashboards

1. **In Prometheus** (http://localhost:9090):
   - Go to "Graph"
   - Try queries like:
     - `http_server_requests_seconds_count` (request count)
     - `jvm_memory_used_bytes` (memory usage)
     - `system_cpu_usage` (CPU usage)

2. **In Grafana** (http://localhost:3000):
   - Watch your dashboard update in real-time
   - You should see request counts increasing
   - Response times might spike briefly

### Create an Error to Test

```bash
# Try to access a non-existent endpoint
curl http://localhost:8085/api/nonexistent

# Check Grafana for error rate increase
```

## 📈 Creating Custom Dashboards

### Basic Dashboard Creation

1. **Create New Dashboard:**
   - Click "+" → "Dashboard"
   - Click "Add new panel"

2. **Add a Simple Chart:**
   - Query: `rate(http_server_requests_seconds_count[5m])`
   - Title: "Requests per Second"
   - This shows request rate over the last 5 minutes

3. **Add Memory Usage Chart:**
   - Query: `jvm_memory_used_bytes{area="heap"}`
   - Title: "Heap Memory Usage"
   - Shows how much memory each service is using

4. **Add Error Rate Chart:**
   - Query: `rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])`
   - Title: "Error Rate"
   - Shows 4xx and 5xx errors per second

### Useful Prometheus Queries

```bash
# Requests per second by service
rate(http_server_requests_seconds_count[5m])

# Average response time
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# Memory usage percentage
(jvm_memory_used_bytes / jvm_memory_max_bytes) * 100

# CPU usage percentage
system_cpu_usage * 100

# Error percentage
(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m]) / rate(http_server_requests_seconds_count[5m])) * 100
```

## 🚨 Setting Up Alerts

### Basic Alerting in Grafana

1. **Create Alert Rule:**
   - Edit a panel
   - Go to "Alert" tab
   - Set condition: "IS ABOVE 100" (for example, if error rate > 100 per minute)

2. **Set Notification Channel:**
   - Go to "Alerting" → "Notification channels"
   - Add email, Slack, or webhook
   - Test the notification

3. **Example Alert Rules:**
   - **High Error Rate:** Error rate > 5%
   - **High Response Time:** Average response time > 1 second
   - **High Memory Usage:** Memory usage > 80%
   - **Service Down:** No requests received in last 5 minutes

## 🚨 Common Issues and Solutions

### Issue 1: Prometheus Shows Targets as "DOWN"

**Check list:**
```bash
# 1. Is the service running?
docker-compose ps

# 2. Can Prometheus reach the service?
docker exec -it prometheus wget -qO- http://user-backend:8080/actuator/prometheus

# 3. Are metrics enabled in the service?
curl http://localhost:8080/actuator/prometheus
```

**Common fixes:**
- Wait 2-3 minutes for services to fully start
- Check service logs: `docker-compose logs user-backend`
- Verify network connectivity between containers

### Issue 2: Grafana Shows "No Data"

**Possible causes:**
- Wrong Prometheus URL (should be `http://prometheus:9090`)
- Prometheus targets are down
- Time range is incorrect (try "Last 1 hour")
- Query syntax error

**Solutions:**
```bash
# Test Prometheus connection from Grafana container
docker exec -it grafana wget -qO- http://prometheus:9090/api/v1/query?query=up

# Check if data exists in Prometheus
curl "http://localhost:9090/api/v1/query?query=up"
```

### Issue 3: Dashboard Import Fails

**Problem:** Can't import dashboard 12900

**Solutions:**
1. Try a different dashboard ID: `11378` or `4701`
2. Manually create panels using the custom dashboard section above
3. Check Grafana logs: `docker-compose logs grafana`

### Issue 4: High Memory Usage

**Problem:** Prometheus or Grafana using too much memory

**Solutions:**
```yaml
# In docker-compose.yml, limit memory
prometheus:
  deploy:
    resources:
      limits:
        memory: 512M

grafana:
  deploy:
    resources:
      limits:
        memory: 256M
```

## 🔧 Advanced Configuration

### Custom Metrics in Your Code

You can add custom business metrics to your Spring Boot services:

```java
// In your service class
@RestController
public class UserController {
    
    private final Counter userCreationCounter = Counter.builder("users_created_total")
        .description("Total number of users created")
        .register(Metrics.globalRegistry);
    
    private final Timer requestTimer = Timer.builder("user_request_duration")
        .description("Time taken to process user requests")
        .register(Metrics.globalRegistry);
    
    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return Timer.Sample.start(Metrics.globalRegistry)
            .stop(requestTimer.time(() -> {
                userCreationCounter.increment();
                return userService.createUser(user);
            }));
    }
}
```

### Retention and Performance

**Prometheus Configuration:**
```yaml
# In prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  
# Data retention (default is 15 days)
# Add to command line: --storage.tsdb.retention.time=30d
```

### Multiple Environments

For production, staging, and development:

```yaml
# Different Prometheus configs for each environment
scrape_configs:
  - job_name: 'production-services'
    static_configs:
      - targets: ['prod-user-service:8080']
    labels:
      environment: 'production'
      
  - job_name: 'staging-services' 
    static_configs:
      - targets: ['staging-user-service:8080']
    labels:
      environment: 'staging'
```

## 📊 Production Best Practices

### ✅ What to Monitor in Production:

1. **Golden Signals:**
   - **Latency:** How long requests take
   - **Traffic:** How many requests per second
   - **Errors:** What percentage of requests fail
   - **Saturation:** How full your services are

2. **Business Metrics:**
   - User registrations per hour
   - Orders placed per day
   - Revenue per minute
   - Active user sessions

3. **Infrastructure Metrics:**
   - CPU, memory, disk usage
   - Network bandwidth
   - Database connections
   - Cache hit rates

### ⚠️ Alert Thresholds for Production:

```yaml
# Example alert rules
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
  for: 5m
  
- alert: HighLatency
  expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 0.5
  for: 10m
  
- alert: ServiceDown
  expr: up == 0
  for: 1m
```

### 🚀 Scaling Considerations:

1. **Prometheus Federation:** For multiple Prometheus instances
2. **Long-term Storage:** Use Thanos or Cortex for historical data
3. **High Availability:** Run multiple Grafana instances
4. **Performance:** Use recording rules for expensive queries

## 📚 Additional Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Spring Boot Actuator Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Grafana Dashboard Library](https://grafana.com/grafana/dashboards/)

## 🎯 What's Next?

Once you're comfortable with basic monitoring:

1. **Set up alerting** for critical issues
2. **Create custom dashboards** for your specific business needs
3. **Add distributed tracing** with Jaeger or Zipkin
4. **Implement SLIs/SLOs** (Service Level Indicators/Objectives)
5. **Explore advanced Grafana features** like annotations and templating

---

**🎉 Congratulations!** You now have a complete monitoring system that tracks the health and performance of all your microservices. You can see real-time metrics, create beautiful dashboards, and set up alerts to catch problems before they affect your users!
