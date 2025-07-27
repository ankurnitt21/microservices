
# ELK Stack Centralized Logging Documentation

## 1. High-Level Architecture Flow

The journey of a single log message from application to dashboard follows this path:

```
[Microservice] -> [Log File on Host Machine] -> [Filebeat] -> [Confluent Cloud (Kafka)] -> [Logstash] -> [Elasticsearch] -> [Kibana UI]
```

| Component | Role |
| :--- | :--- |
| **Spring Boot App** | Generates logs and writes them to a local file in JSON format. |
| **Docker Volume** | Makes the log file written inside the container visible on the host machine. |
| **Filebeat** | A lightweight "shipper" that watches the log files for changes and sends new logs to Kafka. |
| **Confluent Cloud**| A managed, highly reliable Kafka service that acts as a central "buffer" for all logs. |
| **Logstash** | Consumes logs from the Kafka topic, processes them, and sends them to Elasticsearch. |
| **Elasticsearch**| A powerful search engine that indexes and stores the logs. |
| **Kibana** | A web UI for searching, analyzing, and visualizing the logs stored in Elasticsearch. |

---

## 2. Setup Instructions

Follow these steps to set up the entire logging pipeline from scratch.

### Step 1: Confluent Cloud Setup

1.  **Create Cluster:** Sign up for a free account at [Confluent Cloud](https://confluent.cloud/) and create a **Basic** cluster.
2.  **Create Topic:** Inside your new cluster, create a topic to receive the logs.
    *   **Topic Name:** `microservice-logs`
3.  **Generate API Keys:**
    *   Navigate to your cluster's **API keys** section.
    *   Create a new key. Confluent will provide a **Key** (username) and a **Secret** (password).
    *   **Crucially, copy the Key, Secret, and the cluster's Bootstrap Server URL and save them.**

### Step 2: Create `.env` File for Credentials

To keep secrets out of configuration files, we use an environment file.

1.  In the root of your `microservices` project, create a file named `.env`.
2.  Add your Confluent Cloud credentials to this file:

    ```env
    # Confluent Cloud Credentials
    KAFKA_BOOTSTRAP_URL="pkc-....us-east-1.aws.confluent.cloud:9092"
    KAFKA_API_KEY="YOUR_API_KEY"
    KAFKA_API_SECRET="YOUR_API_SECRET"
    ```
3.  **Important:** Add the `.env` file to your `.gitignore` file to prevent it from ever being committed to Git.
    ```
    # .gitignore
    .env
    ```

### Step 3: Configure Microservices for File-Based Logging

Each microservice must be configured to write logs to a file in a structured JSON format.

1.  **Add Dependency:** In the `pom.xml` of **each** microservice (`user-backend`, `product-service`, etc.), ensure this dependency is present:
    ```xml
    <dependency>
        <groupId>net.logstash.logback</groupId>
        <artifactId>logstash-logback-encoder</artifactId>
        <version>7.4</version>
    </dependency>
    ```

2.  **Create `logback-spring.xml`:** In the `src/main/resources/` directory of **each** microservice, create a file named `logback-spring.xml` with the following content:
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <configuration>
        <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
        <springProperty scope="context" name="springAppName" source="spring.application.name"/>
        <appender name="FILE_JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/logs/${springAppName}.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/logs/${springAppName}.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>7</maxHistory>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
        </appender>
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [${springAppName}] %-5level - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="FILE_JSON"/>
            <appender-ref ref="CONSOLE"/>
        </root>
    </configuration>
    ```

### Step 4: Configure Docker Compose Volumes

In the main `docker-compose.yml` file, mount a shared log directory into each microservice container.

1.  On your host machine (the Ubuntu VM), create the shared log directory:
    ```bash
    mkdir -p /home/ankur/Desktop/microservices/all-logs
    ```
2.  In your `docker-compose.yml`, add the `volumes` section to each application service:
    ```yaml
      user-backend:
        # ... other configurations
        volumes:
          - ./all-logs:/var/logs
    
      product-service:
        # ... other configurations
        volumes:
          - ./all-logs:/var/logs
    
      # (Repeat for inventory-service and order-service)
    ```

### Step 5: Configure the ELK Stack

The ELK stack is defined in a separate `docker-compose-elk.yml` file and uses its own configuration files.

1.  **Create `filebeat.yml`:** In a new directory `elk-stack-configs`, create this file. It reads from the shared log directory and sends data to Kafka.
    ```yaml
    # filebeat.yml
    filebeat.inputs:
    - type: log
      enabled: true
      paths:
        - /var/log/my-app-logs/*.log
      json.keys_under_root: true
      json.add_error_key: true

    output.kafka:
      hosts: ["${KAFKA_BOOTSTRAP_URL}"]
      topic: 'microservice-logs'
      username: "${KAFKA_API_KEY}"
      password: "${KAFKA_API_SECRET}"
      ssl.enabled: true
      sasl.mechanism: PLAIN
    ```

2.  **Create `logstash.conf`:** In the same `elk-stack-configs` directory, create this file. It consumes from Kafka and sends data to Elasticsearch.
    ```
    # logstash.conf
    input {
      kafka {
        bootstrap_servers => "${KAFKA_BOOTSTRAP_URL}"
        topics => ["microservice-logs"]
        codec => "json"
        group_id => "logstash_consumer_group"
        auto_offset_reset => "earliest"
        security_protocol => "SASL_SSL"
        sasl.mechanism => "PLAIN"
        sasl_jaas_config => 'org.apache.kafka.common.security.plain.PlainLoginModule required username="${KAFKA_API_KEY}" password="${KAFKA_API_SECRET}";'
      }
    }
    output {
      elasticsearch {
        hosts => ["http://elasticsearch:9200"]
        index => "microservices-%{+YYYY.MM.dd}"
      }
    }
    ```

---

## 6. How to Run

The application stack and the ELK stack are run separately.

1.  **Start the Microservices Stack:**
    ```bash
    docker compose up --build -d
    ```
2.  **Start the ELK Stack:**
    ```bash
    docker compose -f docker-compose-elk.yml up -d
    ```

---

## 7. Internal Working: The Journey of a Single Log Message

1.  **Generation:** When you call an API on `user-backend`, its `SLF4J` logger creates a log event.
2.  **Formatting:** The `logstash-logback-encoder` in `logback-spring.xml` intercepts this event and formats it into a single JSON string.
3.  **File Writing:** The `RollingFileAppender` writes this JSON string as a new line into the `/var/logs/user-backend.log` file **inside the container**.
4.  **Volume Mount:** Because of the Docker volume (`./all-logs:/var/logs`), this new line instantly appears in the `/home/ankur/Desktop/microservices/all-logs/user-backend.log` file on the **host VM**.
5.  **Detection:** The `Filebeat` container, which is monitoring the `all-logs` directory via its own volume mount, detects this new line.
6.  **Shipping:** `Filebeat` reads the new line and, using the credentials from its `environment` variables, sends the log message over the internet to your **Confluent Cloud Kafka topic**.
7.  **Consumption:** The `Logstash` container is continuously listening to the Confluent Cloud topic. It receives the new log message.
8.  **Processing:** Logstash parses the JSON and prepares to send it to Elasticsearch.
9.  **Indexing:** Logstash sends the final JSON document to the `Elasticsearch` container. Elasticsearch analyzes the data and stores it in the `microservices-YYYY.MM.DD` index, making it searchable.
10. **Visualization:** When you open the **Discover** tab in **Kibana**, it queries Elasticsearch for the latest data in the `microservices-*` index pattern and displays your log message in the UI.