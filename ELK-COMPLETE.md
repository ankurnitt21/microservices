# 📊 Complete Logging Setup Guide (ELK Stack)

## 🤔 What is Logging and Why Do We Need It?

**Simple Explanation:**
Imagine you're running a restaurant. You want to know:
- How many customers came today?
- What did they order?
- Were there any problems in the kitchen?
- Which dishes are most popular?

**Logging is like keeping a detailed diary** of everything that happens in your application:
- Who accessed which API?
- Were there any errors?
- How long did each request take?
- What data was processed?

**ELK Stack = Your Log Management System:**
- **E**lasticsearch = The filing cabinet (stores all logs)
- **L**ogstash = The processor (cleans and organizes logs)
- **K**ibana = The dashboard (shows pretty charts and graphs)
- **Filebeat** = The collector (gathers logs from your apps)

## 🏗️ How It Works (Simple Version)

```
[Your App] → [Log File] → [Filebeat] → [Kafka] → [Logstash] → [Elasticsearch] → [Kibana Dashboard]
```

**Step-by-step:**
1. Your app writes logs to a file (like a diary entry)
2. Filebeat reads the file and sends logs to Kafka (message queue)
3. Logstash processes the logs (cleans them up)
4. Elasticsearch stores the logs (searchable database)
5. Kibana shows you pretty dashboards and charts

## 🚀 Quick Start (Get Logging Running)

### Option 1: Basic Setup (Recommended for Beginners)

This setup uses local files only - no external services needed.

**Step 1: Start the ELK Stack**

```bash
# Start the main application first
docker-compose up -d

# Start the ELK stack
docker-compose -f docker-compose-elk.yml up -d
```

**Step 2: Check if Everything is Running**

```bash
# Check ELK services
docker-compose -f docker-compose-elk.yml ps

# You should see:
# - elasticsearch (port 9200)
# - kibana (port 5601)
# - logstash (port 5044)
# - filebeat
```

**Step 3: View Your Logs**

1. **Open Kibana:** http://localhost:5601
2. **Create Index Pattern:**
   - Go to "Stack Management" → "Index Patterns"
   - Click "Create index pattern"
   - Type: `microservices-*`
   - Click "Next step"
   - Choose "@timestamp" as time field
   - Click "Create index pattern"

3. **View Logs:**
   - Go to "Discover" tab
   - You should see logs from your microservices!

### Option 2: Advanced Setup (With Kafka Cloud)

This uses Confluent Cloud Kafka for better reliability and scalability.

## 🛠️ Detailed Setup Instructions

### Step 1: Configure Your Applications for Logging

**What we're doing:** Making sure each microservice writes logs in a format that ELK can understand.

**For each service** (user-backend, product-service, order-service, inventory-service):

1. **Check Maven Dependencies** (should already be there):

```xml
<!-- In pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

2. **Check Logging Configuration** (should already exist):

Look for `src/main/resources/logback-spring.xml` in each service:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console logging (what you see in terminal) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File logging (JSON format for ELK) -->
    <appender name="FILE_JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/logs/${spring.application.name}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/logs/${spring.application.name}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE_JSON"/>
    </root>
</configuration>
```

**What this does:**
- Writes logs to console (for development)
- Writes logs to `/var/logs/[service-name].log` in JSON format
- Keeps 7 days of log history
- Rotates logs daily

3. **Check Docker Volume Mounting**:

In `docker-compose.yml`, each service should have:

```yaml
user-backend:
  # ... other config
  volumes:
    - ./all-logs:/var/logs  # This maps container logs to host folder
```

### Step 2: Set Up ELK Stack Configuration

**Create the ELK configuration files:**

1. **Create directory structure:**

```bash
mkdir -p elk-stack-configs
```

2. **Create Filebeat configuration** (`elk-stack-configs/filebeat.yml`):

```yaml
# Filebeat configuration - collects logs from files
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/my-app-logs/*.log  # Where to find log files
  json.keys_under_root: true      # Parse JSON logs
  json.add_error_key: true        # Add errors if JSON parsing fails
  fields:
    logtype: microservices        # Tag for identification
  fields_under_root: true

# Where to send the logs
output.logstash:
  hosts: ["logstash:5044"]        # Send to Logstash container

# Logging level for Filebeat itself
logging.level: info
```

3. **Create Logstash configuration** (`elk-stack-configs/logstash.conf`):

```ruby
# Logstash configuration - processes and forwards logs

input {
  beats {
    port => 5044  # Receive from Filebeat
  }
}

filter {
  # Add timestamp if missing
  if ![timestamp] {
    mutate {
      add_field => { "timestamp" => "%{@timestamp}" }
    }
  }
  
  # Parse log level
  if [level] {
    mutate {
      uppercase => [ "level" ]
    }
  }
}

output {
  # Send to Elasticsearch
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
    index => "microservices-%{+YYYY.MM.dd}"  # Daily indexes
  }
  
  # Also output to console for debugging
  stdout { 
    codec => rubydebug 
  }
}
```

### Step 3: Docker Compose Configuration

**Check your `docker-compose-elk.yml` file:**

```yaml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    networks:
      - elk

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    container_name: kibana
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch
    networks:
      - elk

  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    container_name: logstash
    volumes:
      - ./elk-stack-configs/logstash.conf:/usr/share/logstash/pipeline/logstash.conf:ro
    ports:
      - "5044:5044"
    environment:
      - "LS_JAVA_OPTS=-Xmx256m -Xms256m"
    depends_on:
      - elasticsearch
    networks:
      - elk

  filebeat:
    image: docker.elastic.co/beats/filebeat:8.11.0
    container_name: filebeat
    user: root
    volumes:
      - ./elk-stack-configs/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - ./all-logs:/var/log/my-app-logs:ro  # Read from shared log directory
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    depends_on:
      - logstash
    networks:
      - elk

networks:
  elk:
    driver: bridge

volumes:
  elasticsearch_data:
    driver: local
```

## 🧪 Testing Your Logging Setup

### Step 1: Generate Some Logs

```bash
# Make some API calls to generate logs
curl http://localhost:8085/api/users
curl http://localhost:8085/api/products
curl -X POST http://localhost:8085/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Test User", "email": "test@example.com"}'
```

### Step 2: Check Log Files

```bash
# Check if log files are being created
ls -la all-logs/

# You should see files like:
# user-backend.log
# product-service.log
# order-service.log

# Check log content
tail -f all-logs/user-backend.log
```

### Step 3: Check Elasticsearch

```bash
# Check if Elasticsearch is receiving data
curl http://localhost:9200/_cat/indices

# Should show microservices-YYYY.MM.DD index
```

### Step 4: View in Kibana

1. **Open Kibana:** http://localhost:5601
2. **Wait for it to load** (takes 1-2 minutes first time)
3. **Create Index Pattern:**
   - Go to "Stack Management" (hamburger menu)
   - Click "Index Patterns"
   - Click "Create index pattern"
   - Index pattern name: `microservices-*`
   - Time field: `@timestamp`
   - Click "Create"

4. **View Logs:**
   - Go to "Discover"
   - Select your index pattern
   - You should see your application logs!

## 📊 Creating Useful Dashboards

### Basic Log Analysis Queries

In Kibana Discover, try these searches:

```bash
# Show only error logs
level: ERROR

# Show logs from specific service
logger_name: "com.example.user_backend.*"

# Show logs with specific message
message: "user"

# Show logs from last hour
@timestamp: [now-1h TO now]
```

### Creating Your First Dashboard

1. **Go to Dashboard** → "Create new dashboard"
2. **Add visualizations:**
   - **Log Count Over Time:** Shows how many logs per hour
   - **Log Levels Pie Chart:** Shows distribution of INFO/WARN/ERROR
   - **Top Services:** Which services generate most logs
   - **Error Messages:** Recent error messages

3. **Save your dashboard** for future use

## 🚨 Common Issues and Solutions

### Issue 1: No Logs Appearing in Kibana

**Check list:**
```bash
# 1. Are log files being created?
ls -la all-logs/

# 2. Is Filebeat running?
docker-compose -f docker-compose-elk.yml logs filebeat

# 3. Is Logstash receiving data?
docker-compose -f docker-compose-elk.yml logs logstash

# 4. Is Elasticsearch healthy?
curl http://localhost:9200/_cluster/health
```

**Common fixes:**
- Wait longer (ELK stack takes 2-3 minutes to start)
- Restart ELK stack: `docker-compose -f docker-compose-elk.yml restart`
- Check file permissions: `sudo chmod 644 all-logs/*`

### Issue 2: Elasticsearch Memory Errors

**Problem:** Elasticsearch crashes with OutOfMemory errors

**Solution:**
```yaml
# In docker-compose-elk.yml, reduce memory usage:
elasticsearch:
  environment:
    - "ES_JAVA_OPTS=-Xms256m -Xmx256m"  # Reduced from 512m
```

### Issue 3: Filebeat Permission Errors

**Problem:** Filebeat can't read log files

**Solution:**
```bash
# Fix file permissions
sudo chown -R $USER:$USER all-logs/
chmod 644 all-logs/*
```

### Issue 4: Kibana Shows "No Index Pattern"

**Problem:** Can't create index pattern in Kibana

**Solution:**
1. Wait for data to arrive (check Elasticsearch first)
2. Make sure index name matches: `microservices-*`
3. Generate more logs by using your APIs
4. Check Logstash logs for errors

## 🔧 Advanced Configuration

### Adding Kafka (For Production)

If you want to use Confluent Cloud Kafka:

1. **Sign up for Confluent Cloud** (free tier available)
2. **Create a cluster and topic** called `microservice-logs`
3. **Get your credentials** (bootstrap server, API key, secret)
4. **Update your `.env` file:**

```env
KAFKA_BOOTSTRAP_URL=pkc-xxxxx.us-east-1.aws.confluent.cloud:9092
KAFKA_API_KEY=your-api-key
KAFKA_API_SECRET=your-api-secret
```

5. **Update Filebeat configuration:**

```yaml
# In filebeat.yml
output.kafka:
  hosts: ["${KAFKA_BOOTSTRAP_URL}"]
  topic: 'microservice-logs'
  username: "${KAFKA_API_KEY}"
  password: "${KAFKA_API_SECRET}"
  ssl.enabled: true
  sasl.mechanism: PLAIN
```

6. **Update Logstash configuration:**

```ruby
# In logstash.conf
input {
  kafka {
    bootstrap_servers => "${KAFKA_BOOTSTRAP_URL}"
    topics => ["microservice-logs"]
    codec => "json"
    group_id => "logstash_consumer_group"
    security_protocol => "SASL_SSL"
    sasl.mechanism => "PLAIN"
    sasl_jaas_config => 'org.apache.kafka.common.security.plain.PlainLoginModule required username="${KAFKA_API_KEY}" password="${KAFKA_API_SECRET}";'
  }
}
```

### Log Retention and Management

**Automatic cleanup:**
```bash
# Delete old logs (run weekly)
find all-logs/ -name "*.log" -mtime +7 -delete

# Or use logrotate for automatic rotation
```

**Elasticsearch index management:**
```bash
# Delete old indices
curl -X DELETE "localhost:9200/microservices-2024.01.*"
```

## 📈 Monitoring Your Logs

### Important Metrics to Watch:

1. **Error Rate:** How many ERROR level logs per hour?
2. **Response Times:** How long do API calls take?
3. **Popular Endpoints:** Which APIs are used most?
4. **User Activity:** How many unique users per day?
5. **System Health:** Any OutOfMemory or database connection errors?

### Setting Up Alerts:

You can configure Kibana to send alerts when:
- Error rate exceeds threshold
- Specific error messages appear
- Service stops generating logs (indicates it's down)

## 🎯 Best Practices

### ✅ Good Logging Practices:

1. **Log meaningful information:**
   ```java
   log.info("User {} created order {} for product {}", userId, orderId, productId);
   ```

2. **Use appropriate log levels:**
   - `ERROR`: Something broke that needs immediate attention
   - `WARN`: Something suspicious but not critical
   - `INFO`: Important business events
   - `DEBUG`: Detailed technical information

3. **Include context:**
   ```java
   log.error("Failed to process order {} for user {}: {}", orderId, userId, exception.getMessage());
   ```

4. **Don't log sensitive data:**
   ```java
   // ❌ Bad
   log.info("User password: {}", password);
   
   // ✅ Good  
   log.info("User {} logged in successfully", username);
   ```

### ⚠️ What to Avoid:

1. **Too much logging:** Don't log every variable
2. **Logging passwords/tokens:** Security risk
3. **Logging in loops:** Can create huge log files
4. **Inconsistent formats:** Makes searching difficult

## 🚀 Production Considerations

When deploying to production:

1. **Use managed ELK services** (AWS OpenSearch, Elastic Cloud)
2. **Set up log retention policies** (keep 30-90 days)
3. **Configure log shipping over encrypted connections**
4. **Set up monitoring and alerts**
5. **Regular backup of important logs**

## 📚 Additional Resources

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Kibana User Guide](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Logstash Reference](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Filebeat Documentation](https://www.elastic.co/guide/en/beats/filebeat/current/index.html)

---

**🎉 Congratulations!** You now have a complete logging system that collects, processes, and visualizes logs from all your microservices. You can track user activity, monitor system health, and quickly debug issues!
