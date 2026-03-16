# Inventory Service - Observable Spring Boot Application

A comprehensive observability implementation demonstrating metrics, logs, and distributed tracing using industry-standard tools.

## Table of Contents
- [Overview](#overview)
- [Day 1: Metrics and Logs](#day-1-metrics-and-logs)
- [Day 2: Distributed Tracing](#day-2-distributed-tracing)
- [Getting Started](#getting-started)
- [Accessing the Observability Stack](#accessing-the-observability-stack)
- [Troubleshooting](#troubleshooting)
- [References](#references)

---

## Overview

This project implements a production-ready observability stack for a Spring Boot inventory management service, covering the three pillars of observability:

1. **Metrics** (Day 1) - Application performance indicators
2. **Logs** (Day 1) - Structured JSON logging with aggregation
3. **Traces** (Day 2) - Distributed tracing with context propagation

### Complete Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                     │
│  ┌────────────┐  ┌────────────┐  ┌────────────────────────┐ │
│  │  Metrics   │  │    Logs    │  │   Distributed Traces   │ │
│  │ (Micrometer)│  │ (Logback)  │  │  (Micrometer Tracing)  │ │
│  └─────┬──────┘  └──────┬─────┘  └───────────┬────────────┘ │
└────────┼─────────────────┼──────────────────────┼────────────┘
         │                 │                      │
         ▼                 ▼                      ▼
    Prometheus          Promtail          OTel Collector
         │                 │                      │
         │                 ▼                      ▼
         │               Loki                  Tempo
         │                 │                      │
         └─────────────────┴──────────────────────┘
                           │
                           ▼
                       Grafana
              (Unified Visualization)
```

### Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| **Spring Boot** | 3.2.2 | Application framework |
| **Micrometer** | - | Metrics facade |
| **Micrometer Tracing** | - | Tracing facade |
| **OpenTelemetry** | - | Tracing instrumentation |
| **Prometheus** | latest | Metrics storage and querying |
| **Loki** | latest | Log aggregation |
| **Promtail** | latest | Log shipping |
| **Tempo** | 2.3.0 | Distributed tracing backend |
| **OpenTelemetry Collector** | latest | Trace collection and forwarding |
| **Grafana** | latest | Unified observability dashboard |

---

## Day 1: Metrics and Logs

### What Was Implemented

**Day 1** focused on establishing the foundation of observability through metrics collection and structured logging.

#### 1.1 Metrics with Micrometer and Prometheus

**Dependencies:**
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

**Configuration (`application.properties`):**
```properties
# Actuator Configuration
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.health.show-details=always
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true

# Metrics Configuration
management.metrics.tags.application=${spring.application.name}
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

**Custom Metrics Implemented:**
- `inventory_requests_total` - Counter for total requests by status (success/error)
- HTTP request duration histograms with percentiles
- JVM metrics (memory, GC, threads)
- System metrics (CPU, disk usage)

**Prometheus Scraping:**
- Endpoint: `http://localhost:8080/actuator/prometheus`
- Scrape interval: 15 seconds
- Retention: 15 days

#### 1.2 Structured Logging with Logback and Loki

**Dependencies:**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**Logback Configuration (`logback-spring.xml`):**
```xml
<appender name="JSON_FILE" class="ch.qos.logback.core.FileAppender">
    <file>logs/inventory-service.log</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeContext>true</includeContext>
        <includeMdc>true</includeMdc>
        <includeStructuredArguments>true</includeStructuredArguments>
        <customFields>{"application":"inventory-service"}</customFields>
    </encoder>
</appender>
```

**Log Features:**
- JSON-formatted logs for machine parsing
- Structured fields: timestamp, level, logger, thread, message, application
- File-based logging to `logs/inventory-service.log`
- Promtail ships logs to Loki for aggregation

**Example Log Entry:**
```json
{
  "timestamp": "2026-03-16T08:54:23.123",
  "level": "INFO",
  "message": "Successfully retrieved item: Laptop with quantity: 50",
  "logger_name": "com.nashtech.inventory.service.InventoryService",
  "thread_name": "http-nio-8080-exec-2",
  "application": "inventory-service"
}
```

#### 1.3 Error Simulation

Implemented random failures and delays for observability testing:
- **15% error rate** - Simulates database connection failures (HTTP 500)
- **20% delay rate** - Adds random latency (100-1000ms)

```java
// Simulate random exception (15% chance)
if (random.nextInt(100) < 15) {
    logger.error("Simulated error occurred while fetching itemId: {}", itemId);
    incrementRequestCounter("error");
    throw new RuntimeException("Simulated database connection failure");
}

// Simulate random delay (20% chance)
if (random.nextInt(100) < 20) {
    long delay = 100 + random.nextInt(900);
    logger.info("Simulating processing delay of {}ms", delay);
    Thread.sleep(delay);
}
```

### Day 1 Verification

**Check Metrics:**
```bash
curl http://localhost:8080/actuator/prometheus | grep inventory_requests_total
```

**Check Logs:**
```bash
tail -f logs/inventory-service.log | jq .
```

**View in Grafana:**
1. Metrics: Explore → Prometheus → Query: `inventory_requests_total`
2. Logs: Explore → Loki → Query: `{application="inventory-service"}`

---

## Day 2: Distributed Tracing

### What Was Implemented

**Day 2** extended the observability stack with distributed tracing capabilities, enabling end-to-end request tracking and log-trace correlation.

#### 2.1 Distributed Tracing with OpenTelemetry

**New Dependencies Added (`pom.xml`):**
```xml
<!-- Micrometer Tracing Bridge for OpenTelemetry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- OpenTelemetry Exporter for OTLP -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

**Tracing Configuration (`application.properties`):**
```properties
# Tracing Configuration (Day 2)
management.tracing.sampling.probability=1.0
management.tracing.enabled=true
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
```

**What This Enables:**
- Automatic instrumentation of HTTP requests
- Unique Trace IDs for request correlation
- Span IDs for individual operations
- Trace context propagation via W3C Trace Context headers

#### 2.2 Log-Trace Correlation

**Updated Logback to Include Trace Context:**

The existing `logback-spring.xml` configuration automatically includes trace context in logs through MDC:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <includeContext>true</includeContext>
    <includeMdc>true</includeMdc>  <!-- Enables automatic traceId/spanId -->
</encoder>
```

**Enhanced Log Output:**
```json
{
  "timestamp": "2026-03-16T09:22:36.175",
  "level": "DEBUG",
  "message": "Returning item: Mouse for itemId: ITEM002",
  "traceId": "38759906c05e362118df9cfd7d59479c",
  "spanId": "1fd87d7fb643d408",
  "application": "inventory-service"
}
```

**Benefit:** Click on `traceId` in Loki to jump directly to the corresponding trace in Tempo.

#### 2.3 Custom Span with Attributes

**Implementation (`InventoryService.java`):**

Added custom span for inventory validation with business context:

```java
private final ObservationRegistry observationRegistry;

private InventoryItem validateInventory(String itemId) {
    return Observation.createNotStarted("inventory.validation", observationRegistry)
            .lowCardinalityKeyValue("item.id", itemId)
            .observe(() -> {
                logger.debug("Validating inventory for itemId: {}", itemId);
                InventoryItem item = inventory.get(itemId);

                if (item == null) {
                    logger.warn("Item not found with ID: {}", itemId);
                    incrementRequestCounter("error");
                    throw new IllegalArgumentException("Item not found: " + itemId);
                }

                logger.debug("Inventory validation successful for itemId: {}", itemId);
                return item;
            });
}
```

**Trace Hierarchy:**
```
GET /api/inventory/items/ITEM001
  └─ HTTP GET /api/inventory/items/{itemId}  [200 OK, 45ms]
      └─ inventory.validation  [12ms]
          - item.id: ITEM001
```

**Custom Attribute Benefits:**
- Search traces by specific item IDs: `{ span.item.id = "ITEM001" }`
- Filter slow validations for specific items
- Correlate errors to specific inventory items

#### 2.4 OpenTelemetry Collector Setup

**New Service in `docker-compose.yml`:**
```yaml
otel-collector:
  image: otel/opentelemetry-collector-contrib:latest
  container_name: otel-collector
  ports:
    - "4317:4317"  # OTLP gRPC
    - "4318:4318"  # OTLP HTTP
    - "8888:8888"  # Prometheus metrics
    - "8889:8889"  # Prometheus exporter metrics
  volumes:
    - ./otel-collector-config.yaml:/etc/otel-collector-config.yaml
  command: ["--config=/etc/otel-collector-config.yaml"]
```

**Collector Configuration (`otel-collector-config.yaml`):**
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 1s
    send_batch_size: 1024

exporters:
  otlp:
    endpoint: tempo:4317
    tls:
      insecure: true
  debug:
    verbosity: detailed

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp, debug]
```

**Data Flow:**
1. Application → OTLP HTTP (port 4318) → OTel Collector
2. OTel Collector → Batch Processing → Tempo (port 4317)
3. Grafana queries Tempo for trace visualization

#### 2.5 Grafana Tempo Integration

**New Service in `docker-compose.yml`:**
```yaml
tempo:
  image: grafana/tempo:2.3.0
  container_name: tempo
  ports:
    - "3200:3200"
  command: [ "-config.file=/etc/tempo.yaml" ]
  volumes:
    - ./tempo-config.yaml:/etc/tempo.yaml:ro
    - tempo-data:/var/tempo
```

**Tempo Configuration (`tempo-config.yaml`):**
```yaml
server:
  http_listen_port: 3200

distributor:
  receivers:
    otlp:
      protocols:
        http:
        grpc:

ingester:
  max_block_duration: 5m

storage:
  trace:
    backend: local
    wal:
      path: /var/tempo/wal
    local:
      path: /var/tempo/blocks
```

**Grafana Datasource Configuration:**

Updated `grafana/provisioning/datasources/datasources.yml`:

```yaml
# Loki with Tempo correlation
- name: Loki
  type: loki
  access: proxy
  url: http://loki:3100
  editable: true
  jsonData:
    maxLines: 1000
    derivedFields:
      - datasourceUid: tempo
        matcherRegex: "\"traceId\":\"(\\w+)\""
        name: TraceID
        url: "${__value.raw}"

# Tempo with Loki correlation
- name: Tempo
  type: tempo
  uid: tempo
  access: proxy
  url: http://tempo:3200
  editable: true
  jsonData:
    httpMethod: GET
    tracesToLogs:
      datasourceUid: loki
      filterByTraceID: true
      filterBySpanID: false
      mapTagNamesEnabled: false
      tags: ['application']
```

**Bidirectional Navigation:**
- Loki → Tempo: Click traceId in logs to view trace
- Tempo → Loki: Click "Logs for this span" to see related logs

#### 2.6 Error Traces and Root Cause Analysis

**Failed Span Visualization:**

When a request fails (HTTP 500), Tempo captures:
- HTTP status code: 500
- Outcome: SERVER_ERROR
- Exception type and message in span attributes
- Full stack trace in span events
- Duration and timing information

**Example Error Trace:**
```
GET /api/inventory/items/ITEM003
  └─ HTTP GET /api/inventory/items/{itemId}  [500 ERROR, 23ms]
      - outcome: SERVER_ERROR
      - status: 500
      - exception: RuntimeException
      - exception.message: Simulated database connection failure
```

### Day 2 Verification

**Generate Test Traffic:**
```bash
for i in {1..50}; do
  curl -s http://localhost:8080/api/inventory/items/ITEM00$((RANDOM % 5 + 1)) > /dev/null
  sleep 0.3
done
```

**Check Traces in Logs:**
```bash
tail -f logs/inventory-service.log | jq '{message, traceId, spanId}'
```

**Query Tempo API:**
```bash
# Search for traces
curl -s "http://localhost:3200/api/search?tags=service.name%3Dinventory-service&limit=10" | jq .

# Get specific trace details
curl -s "http://localhost:3200/api/traces/<traceId>" | jq .
```

**View in Grafana:**
1. Navigate to http://localhost:3000
2. Go to Explore → Select "Tempo" datasource
3. Query type: **TraceQL**
4. Query examples:
   - All traces: `{ resource.service.name = "inventory-service" }`
   - Error traces: `{ status = error }`
   - By item ID: `{ span.item.id = "ITEM001" }`
5. Click on a trace to see Gantt chart with spans

---

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose

### Quick Start

1. **Clone the repository:**
```bash
git clone <repository-url>
cd day1-foundations-workshop
```

2. **Start the observability stack:**
```bash
docker compose up -d
```

3. **Verify all containers are running:**
```bash
docker compose ps
```

Expected output:
```
NAME              STATUS
grafana           running
loki              running
otel-collector    running
prometheus        running
promtail          running
tempo             running
```

4. **Build the application:**
```bash
mvn clean package
```

5. **Run the application:**
```bash
java -jar target/inventory-service-1.0.0-SNAPSHOT.jar
```

6. **Generate test traffic:**
```bash
# Using curl loop
for i in {1..30}; do
  curl http://localhost:8080/api/inventory/items/ITEM00$((RANDOM % 5 + 1))
  sleep 0.5
done
```

---

## Accessing the Observability Stack

### Service Endpoints

| Service | URL | Credentials |
|---------|-----|-------------|
| **Application** | http://localhost:8080 | - |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Prometheus** | http://localhost:9090 | - |
| **Tempo** | http://localhost:3200 | - |

### Application Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/inventory/items/{itemId}` | GET | Get inventory item (ITEM001-ITEM005) |
| `/api/inventory/health` | GET | Custom health check |
| `/actuator/health` | GET | Spring Boot health |
| `/actuator/prometheus` | GET | Prometheus metrics endpoint |
| `/actuator/metrics` | GET | Available metrics list |

### Grafana Datasources

Pre-configured datasources (no setup needed):

1. **Prometheus** - Metrics
   - Query examples:
     - `inventory_requests_total`
     - `rate(inventory_requests_total[5m])`
     - `http_server_requests_seconds_bucket`

2. **Loki** - Logs
   - Query examples:
     - `{application="inventory-service"}`
     - `{application="inventory-service"} | json | level="ERROR"`
     - `{application="inventory-service"} | json | traceId!=""`

3. **Tempo** - Traces
   - Query examples (TraceQL):
     - `{ resource.service.name = "inventory-service" }`
     - `{ status = error }`
     - `{ span.item.id = "ITEM001" }`
     - `{ duration > 100ms }`

### Exploring the Three Pillars

#### Metrics (Prometheus)
1. Go to Grafana → Explore → Prometheus
2. Try queries:
   ```promql
   # Total requests
   inventory_requests_total

   # Error rate
   rate(inventory_requests_total{status="error"}[5m])

   # Request duration 95th percentile
   histogram_quantile(0.95, http_server_requests_seconds_bucket)
   ```

#### Logs (Loki)
1. Go to Grafana → Explore → Loki
2. Try queries:
   ```logql
   # All logs
   {application="inventory-service"}

   # Error logs only
   {application="inventory-service"} | json | level="ERROR"

   # Logs with traces
   {application="inventory-service"} | json | traceId!=""

   # Search for specific item
   {application="inventory-service"} |= "ITEM001"
   ```

#### Traces (Tempo)
1. Go to Grafana → Explore → Tempo
2. Query type: **TraceQL** (not Search - see TEMPO_WORKAROUND.md)
3. Try queries:
   ```traceql
   # All traces from inventory service
   { resource.service.name = "inventory-service" }

   # Only error traces
   { status = error }

   # Traces for specific item
   { span.item.id = "ITEM001" }

   # Slow requests (>100ms)
   { duration > 100ms }
   ```

### Log-Trace Correlation Workflow

**Starting from Logs:**
1. Grafana → Explore → Loki
2. Query: `{application="inventory-service"} | json | level="ERROR"`
3. Click on a log entry to expand details
4. Find the `traceId` field
5. Click on the traceId value or the Tempo link icon
6. The corresponding trace opens in Tempo

**Starting from Traces:**
1. Grafana → Explore → Tempo
2. Find a trace with query: `{ resource.service.name = "inventory-service" }`
3. Click on the trace to view details
4. Click "Logs for this span" button
5. View all logs with the same traceId in Loki

---

## Project Structure

```
day1-foundations-workshop/
├── src/
│   ├── main/
│   │   ├── java/com/nashtech/inventory/
│   │   │   ├── controller/
│   │   │   │   └── InventoryController.java
│   │   │   ├── service/
│   │   │   │   └── InventoryService.java      # Custom metrics + spans
│   │   │   ├── model/
│   │   │   │   └── InventoryItem.java
│   │   │   └── InventoryServiceApplication.java
│   │   └── resources/
│   │       ├── application.properties          # Metrics + Tracing config
│   │       └── logback-spring.xml              # JSON logging config
│   └── test/
│       └── java/com/nashtech/inventory/
│           └── service/
│               └── InventoryServiceTest.java
├── docker-compose.yml                          # Full observability stack
├── prometheus.yml                              # Prometheus scrape config
├── promtail-config.yml                         # Promtail log shipping config
├── otel-collector-config.yaml                  # OTel Collector config (Day 2)
├── tempo-config.yaml                           # Tempo tracing config (Day 2)
├── grafana/
│   └── provisioning/
│       └── datasources/
│           └── datasources.yml                 # Pre-configured datasources
├── logs/
│   └── inventory-service.log                   # JSON log file
├── pom.xml                                     # Maven dependencies
├── README.md                                   # This file
├── SCREENSHOT_GUIDE.md                         # Screenshot instructions
└── TEMPO_WORKAROUND.md                         # Tempo TraceQL usage guide
```

---

## Troubleshooting

### Application Issues

**Application won't start:**
```bash
# Check if port 8080 is already in use
lsof -i :8080

# Check Java version
java -version  # Should be 17+
```

**Build failures:**
```bash
# Clean rebuild
mvn clean package -DskipTests
```

### Docker Issues

**Containers not starting:**
```bash
# Check container status
docker compose ps

# View container logs
docker compose logs <service-name>
docker compose logs grafana
docker compose logs tempo
docker compose logs otel-collector
```

**Port conflicts:**
```bash
# Check which ports are in use
docker compose ps
lsof -i :3000  # Grafana
lsof -i :9090  # Prometheus
lsof -i :3200  # Tempo
```

**Reset everything:**
```bash
# Stop and remove all containers and volumes
docker compose down -v

# Restart fresh
docker compose up -d
```

### Metrics Issues

**Metrics not appearing in Prometheus:**
1. Check application actuator endpoint:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```
2. Verify Prometheus is scraping:
   - Open http://localhost:9090/targets
   - Look for `inventory-service` target
   - Status should be "UP"

### Logs Issues

**Logs not appearing in Loki:**
1. Check log file exists:
   ```bash
   ls -la logs/inventory-service.log
   ```
2. Verify log format is JSON:
   ```bash
   tail logs/inventory-service.log | jq .
   ```
3. Check Promtail logs:
   ```bash
   docker compose logs promtail
   ```

### Traces Issues

**Traces not appearing in Tempo:**
1. Verify application is generating traces:
   ```bash
   tail logs/inventory-service.log | jq 'select(.traceId) | {message, traceId, spanId}'
   ```

2. Check OTel Collector is receiving traces:
   ```bash
   docker compose logs otel-collector | grep "inventory.validation"
   ```

3. Verify Tempo is running:
   ```bash
   curl -s http://localhost:3200/api/search?tags=service.name%3Dinventory-service | jq .
   ```

4. **Important:** Use TraceQL query type in Grafana, not Search
   - See `TEMPO_WORKAROUND.md` for details
   - The old Search API returns 400 errors
   - Use: Query type → **TraceQL**

**TraceId in logs but trace not in Tempo:**
- Traces take a few seconds to appear in Tempo
- Wait 10-15 seconds after generating traffic
- Try querying with TraceQL: `{ resource.service.name = "inventory-service" }`

**Custom span not visible:**
1. Verify ObservationRegistry is injected:
   ```bash
   grep -r "ObservationRegistry" src/main/java
   ```
2. Check OTel Collector debug logs:
   ```bash
   docker compose logs otel-collector | grep "inventory.validation"
   ```

---

## References

### Official Documentation
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Micrometer Tracing](https://micrometer.io/docs/tracing)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Loki](https://grafana.com/docs/loki/latest/)
- [Grafana Tempo](https://grafana.com/docs/tempo/latest/)
- [OpenTelemetry Collector](https://opentelemetry.io/docs/collector/)

### Grafana Resources
- [Tempo TraceQL](https://grafana.com/docs/tempo/latest/traceql/)
- [Loki LogQL](https://grafana.com/docs/loki/latest/logql/)
- [PromQL Documentation](https://prometheus.io/docs/prometheus/latest/querying/basics/)

### Learning Resources
- [The Three Pillars of Observability](https://www.oreilly.com/library/view/distributed-systems-observability/9781492033431/ch04.html)
- [OpenTelemetry Best Practices](https://opentelemetry.io/docs/concepts/observability-primer/)

---

