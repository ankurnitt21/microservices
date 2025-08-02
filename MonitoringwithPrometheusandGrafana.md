# Monitoring with Prometheus and Grafana

This document outlines the setup, configuration, and usage of the Prometheus and Grafana monitoring stack within this microservices project.

## Table of Contents

1.  [Overview](#1-overview)
2.  [Architecture](#2-architecture)
3.  [Prerequisites](#3-prerequisites)
4.  [Implementation Steps](#4-implementation-steps)
    *   [Step 1: Instrumenting the Microservices](#step-1-instrumenting-the-microservices)
    *   [Step 2: Configuring Prometheus](#step-2-configuring-prometheus)
    *   [Step 3: Integrating into Docker Compose](#step-3-integrating-into-docker-compose)
5.  [How to Run](#5-how-to-run)
6.  [Verification and Usage](#6-verification-and-usage)
    *   [Verifying Prometheus Targets](#verifying-prometheus-targets)
    *   [Setting Up and Using Grafana](#setting-up-and-using-grafana)
7.  [Troubleshooting](#7-troubleshooting)
8.  [Next Steps](#8-next-steps)

## 1. Overview

To ensure the reliability and performance of our distributed system, we have implemented a robust monitoring solution using Prometheus and Grafana.

*   **Prometheus** acts as our metrics backbone. It is a powerful time-series database that periodically "scrapes" (pulls) metrics data from each of our microservices. These metrics include JVM health, CPU/memory usage, HTTP request timings, and custom application-level metrics.

*   **Grafana** is our visualization layer. It connects to Prometheus as a data source and allows us to build powerful, interactive dashboards to visualize the collected metrics, analyze trends, and quickly diagnose issues.

This setup provides us with the core pillars of **observability**, helping us understand the internal state of our system from its external outputs.

## 2. Architecture

The monitoring components are integrated directly into our Docker Compose environment and communicate over a shared network.

```plaintext
+-------------------------------------------------------------+
|                     Developer / User                        |
+-------------------------------------------------------------+
             |                                    ^
             | HTTP Requests                      | Views Dashboards
             v                                    |
+--------------------------+         +--------------------------+
| Grafana (localhost:3000) |<--------| Prometheus (localhost:9090)|
+--------------------------+ Queries +--------------------------+
                                                 ^
                                                 | Scrapes Metrics (e.g., every 15s)
                                                 |
+-------------------------------------------------------------------------+
|                      Docker Shared Network (shared-network)               |
|                                                                         |
|  +-----------------+  +-----------------+  +-----------------+  +-----+  |
|  |  user-backend   |  | product-service |  |  order-service  |  | ... |  |
|  | localhost:8080  |  | localhost:8081  |  | localhost:8083  |  |     |  |
|  +-----------------+  +-----------------+  +-----------------+  +-----+  |
|  Exposes /actuator/prometheus endpoint for scraping                     |
|                                                                         |
+-------------------------------------------------------------------------+
```

## 3. Prerequisites

*   Docker Desktop installed and running.
*   Docker Compose V2 installed.
*   The project source code cloned to your local machine.

## 4. Implementation Steps

Follow these steps to enable the monitoring stack.

### Step 1: Instrumenting the Microservices

Each individual Spring Boot microservice must be configured to generate and expose metrics in a format that Prometheus understands.

For **each** service (`user-backend`, `product-service`, `order-service`, `api-gateway`, etc.):

1.  **Add Maven Dependencies:**
    Ensure the following two dependencies are present in each service's `pom.xml`:

    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    ```

2.  **Expose the Prometheus Endpoint:**
    In the `src/main/resources/application.yml` of each service, add the following configuration to expose the `/actuator/prometheus` endpoint over HTTP:

    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: prometheus,health
    ```

3.  **Rebuild Docker Images:** After adding the dependencies and configuration, the Docker images for these services must be rebuilt. The command in the [How to Run](#5-how-to-run) section will handle this automatically.

### Step 2: Configuring Prometheus

We need to create a central configuration file that tells Prometheus which services to monitor.

1.  **Create the Directory:** In the root of the project (the same level as `docker-compose.yml`), create a new folder named `prometheus`.

2.  **Create the Configuration File:** Inside the new `prometheus` folder, create a file named `prometheus.yml`.

3.  **Add Configuration:** Paste the following content into `prometheus/prometheus.yml`. This file instructs Prometheus to scrape the `/actuator/prometheus` endpoint from each of our services, finding them by their service name on the shared Docker network.

    ```yaml
    # prometheus/prometheus.yml
    global:
      scrape_interval: 15s # How frequently to scrape targets.

    scrape_configs:
      # This job discovers and scrapes our Spring Boot microservices
      - job_name: 'spring-micrometer-services'
        metrics_path: '/actuator/prometheus'
        static_configs:
          - targets:
            # We use the container_name/service name and internal container port
            - 'config-server:8888'
            - 'service-discovery:8761'
            - 'user-backend:8080'
            - 'product-service:8081'
            - 'inventory-service:8082'
            - 'order-service:8083'
            - 'api-gateway:8085'
    ```

### Step 3: Integrating into Docker Compose

Finally, we add Prometheus and Grafana as services to our `docker-compose.yml` file.

1.  **Add Service Definitions:**
    Append the following service definitions to the `services:` block in your `docker-compose.yml` file.

    ```yaml
    # ... (inside the 'services:' block, after the api-gateway service)

      prometheus:
        image: prom/prometheus:latest
        container_name: prometheus
        ports:
          - "9090:9090"
        networks:
          - shared-network
        volumes:
          # Mount the configuration file we created from the host into the container
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
          # This named volume will persist your dashboards and configurations
          - grafana_data:/var/lib/grafana
        environment:
          # This allows anonymous login for ease of use in development
          - GF_SECURITY_ADMIN_PASSWORD=admin
        depends_on:
          - prometheus
    ```

2.  **Define the Grafana Volume:**
    At the very end of your `docker-compose.yml` file, add `grafana_data` to the `volumes:` block to ensure your dashboards are saved.

    ```yaml
    # ... (at the end of the file)
    volumes:
      postgres_data:
        driver: local
      grafana_data:
        driver: local
    ```

## 5. How to Run

With all configurations in place, launch the entire stack using a single command from the project root.

```bash
docker-compose up -d --build
```

*   `--build`: This flag forces Docker to rebuild the images for your microservices, which is essential to include the new dependencies and configurations from Step 1.
*   `-d`: This runs the containers in detached mode (in the background).

## 6. Verification and Usage

Once the stack is running, follow these steps to verify that everything is working correctly.

### Verifying Prometheus Targets

1.  Open your web browser and navigate to the Prometheus UI: [**http://localhost:9090**](http://localhost:9090).
2.  Click on the **Status** menu item in the top navigation bar, then select **Targets**.
3.  You should see a list of all your microservices under the `spring-micrometer-services` job. After a minute, their **State** should turn green and read **UP**.

### Setting Up and Using Grafana

1.  **Login to Grafana:**
    *   Navigate to the Grafana UI: [**http://localhost:3000**](http://localhost:3000).
    *   Log in with the default credentials:
        *   **Username:** `admin`
        *   **Password:** `admin`
    *   You will be prompted to change your password.

2.  **Add Prometheus as a Data Source:**
    *   On the left menu, hover over the gear icon (**Configuration**) and click on **Data Sources**.
    *   Click the **Add data source** button.
    *   Select **Prometheus** from the list.
    *   In the **URL** field under "Prometheus server URL", enter `http://prometheus:9090`.
        *   *Note: We use the service name `prometheus` because Grafana is in the same Docker network and can resolve it.*
    *   Click the **Save & test** button at the bottom. You should see a green checkmark confirming "Data source is working".

3.  **Import a Pre-built Dashboard:**
    *   On the left menu, hover over the four-squares icon (**Dashboards**) and click **Import**.
    *   In the "Import via grafana.com" field, enter the ID for a popular Spring Boot dashboard: `12900`.
    *   Click the **Load** button.
    *   On the next screen, at the bottom, there will be a dropdown to select a Prometheus data source. Choose the **Prometheus** source you just added.
    *   Click **Import**.

You will now be taken to a comprehensive dashboard visualizing the health and performance of all your microservices in real-time.

## 7. Troubleshooting

*   **Targets are "DOWN" in Prometheus:**
    *   Verify you completed Step 1 (adding dependencies and YAML config) for that *specific* service.
    *   Ensure there are no typos in the service name or port in `prometheus/prometheus.yml`.
    *   Check the service logs (`docker-compose logs <service_name>`) to ensure it started without errors.

*   **Grafana shows "No Data" on the dashboard:**
    *   Ensure you selected the correct Prometheus data source when you imported the dashboard.
    *   Verify that your targets are "UP" in the Prometheus UI. If Prometheus has no data, Grafana can't display it.

*   **"Connection Refused" when adding Prometheus data source:**
    *   Make sure you used the URL `http://prometheus:9090` and not `localhost`. The Grafana container needs to use the Docker network name to find the Prometheus container.
