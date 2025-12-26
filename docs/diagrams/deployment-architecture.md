# Deployment Architecture

## Docker Compose Stack

Complete containerized environment for local development and testing.

```mermaid
flowchart TB
    subgraph "Docker Compose Stack"
        subgraph "Application Layer"
            APP[Order Fulfillment API<br/>Spring Boot 3.2<br/>Port 8080<br/>Health: /actuator/health]
        end
        
        subgraph "Data Layer"
            DB[(PostgreSQL 15<br/>Port 5432<br/>Database: orderfulfillment<br/>User: postgres)]
        end
        
        subgraph "Message Broker Layer"
            ZOOKEEPER[Apache Zookeeper<br/>Port 2181<br/>Kafka Coordination]
            KAFKA[Apache Kafka<br/>Port 9092<br/>Topics: order-events]
        end
        
        subgraph "Observability Layer"
            PROMETHEUS[Prometheus<br/>Port 9090<br/>Metrics Collection]
            GRAFANA[Grafana<br/>Port 3000<br/>Dashboards & Visualization]
        end
    end
    
    subgraph "External"
        CLIENT[REST Clients<br/>Postman, curl, Frontend]
        BROWSER[Web Browser<br/>Monitoring Tools]
    end
    
    CLIENT -->|HTTP 8080| APP
    APP -->|JDBC| DB
    APP -->|Produce Events| KAFKA
    APP -->|Consume Events| KAFKA
    APP -->|Expose Metrics| PROMETHEUS
    KAFKA -.->|Depends on| ZOOKEEPER
    PROMETHEUS -->|Scrape /actuator/prometheus| APP
    GRAFANA -->|Query| PROMETHEUS
    BROWSER -->|Access Dashboards| GRAFANA
    BROWSER -->|View Metrics| PROMETHEUS
    BROWSER -->|Swagger UI| APP
    
    style APP fill:#e1f5e1
    style DB fill:#e1e5f5
    style KAFKA fill:#fff4e1
    style ZOOKEEPER fill:#fff4e1
    style PROMETHEUS fill:#ffe1f5
    style GRAFANA fill:#ffe1f5
```

---

## Container Specifications

### 1. Order Fulfillment Application
```yaml
Service: order-fulfillment-api
Image: Built from Dockerfile
Base: eclipse-temurin:17-jre-alpine
Ports: 8080:8080
Health Check: curl http://localhost:8080/actuator/health
Depends On: postgres, kafka
Environment:
  - SPRING_PROFILES_ACTIVE=docker
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/orderfulfillment
  - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
Resources:
  - Memory: 512MB
  - CPU: 0.5 cores
```

### 2. PostgreSQL Database
```yaml
Service: postgres
Image: postgres:15-alpine
Ports: 5432:5432
Environment:
  - POSTGRES_DB=orderfulfillment
  - POSTGRES_USER=postgres
  - POSTGRES_PASSWORD=postgres
Volumes:
  - postgres_data:/var/lib/postgresql/data
Health Check: pg_isready -U postgres
```

### 3. Apache Kafka
```yaml
Service: kafka
Image: confluentinc/cp-kafka:7.5.0
Ports: 9092:9092
Environment:
  - KAFKA_BROKER_ID=1
  - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
  - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
  - KAFKA_AUTO_CREATE_TOPICS_ENABLE=true
Depends On: zookeeper
```

### 4. Apache Zookeeper
```yaml
Service: zookeeper
Image: confluentinc/cp-zookeeper:7.5.0
Ports: 2181:2181
Environment:
  - ZOOKEEPER_CLIENT_PORT=2181
  - ZOOKEEPER_TICK_TIME=2000
```

### 5. Prometheus
```yaml
Service: prometheus
Image: prom/prometheus:v2.47.0
Ports: 9090:9090
Volumes:
  - ./prometheus.yml:/etc/prometheus/prometheus.yml
  - prometheus_data:/prometheus
Command: --config.file=/etc/prometheus/prometheus.yml
```

### 6. Grafana
```yaml
Service: grafana
Image: grafana/grafana:10.1.0
Ports: 3000:3000
Environment:
  - GF_SECURITY_ADMIN_PASSWORD=admin
Volumes:
  - grafana_data:/var/lib/grafana
Depends On: prometheus
```

---

## Network Architecture

```mermaid
graph TB
    subgraph "Bridge Network: order-fulfillment-network"
        APP[order-fulfillment-api<br/>Container IP: 172.20.0.2]
        DB[postgres<br/>Container IP: 172.20.0.3]
        KAFKA[kafka<br/>Container IP: 172.20.0.4]
        ZK[zookeeper<br/>Container IP: 172.20.0.5]
        PROM[prometheus<br/>Container IP: 172.20.0.6]
        GRAF[grafana<br/>Container IP: 172.20.0.7]
    end
    
    HOST[Host Machine<br/>localhost]
    
    HOST -->|8080| APP
    HOST -->|5432| DB
    HOST -->|9092| KAFKA
    HOST -->|2181| ZK
    HOST -->|9090| PROM
    HOST -->|3000| GRAF
    
    APP <-->|Internal DNS| DB
    APP <-->|Internal DNS| KAFKA
    KAFKA <-->|Internal DNS| ZK
    PROM -->|Internal DNS| APP
    GRAF -->|Internal DNS| PROM
    
    style HOST fill:#e1e5f5
```

**Network Type:** Bridge (default)  
**Network Name:** `order-fulfillment-network`  
**DNS Resolution:** Automatic via service names  
**Isolation:** All containers in same network can communicate

---

## Volume Management

```mermaid
flowchart LR
    subgraph "Named Volumes"
        PG_VOL[postgres_data<br/>Database files]
        PROM_VOL[prometheus_data<br/>Time-series data]
        GRAF_VOL[grafana_data<br/>Dashboards & settings]
    end
    
    subgraph "Containers"
        PG[postgres]
        PROM[prometheus]
        GRAF[grafana]
    end
    
    PG_VOL -.->|Mount at<br/>/var/lib/postgresql/data| PG
    PROM_VOL -.->|Mount at<br/>/prometheus| PROM
    GRAF_VOL -.->|Mount at<br/>/var/lib/grafana| GRAF
    
    style PG_VOL fill:#e1f5e1
    style PROM_VOL fill:#fff4e1
    style GRAF_VOL fill:#ffe1e1
```

**Persistence Strategy:**
- ✅ Database state persists across container restarts
- ✅ Prometheus metrics retained on restart
- ✅ Grafana dashboards and datasources saved
- ✅ Application is stateless (no volume needed)

---

## Startup Sequence

```mermaid
sequenceDiagram
    participant Docker
    participant ZK as Zookeeper
    participant Kafka
    participant DB as PostgreSQL
    participant App as Order API
    participant Prom as Prometheus
    participant Graf as Grafana

    Docker->>ZK: Start (no dependencies)
    ZK-->>Docker: Running (port 2181)
    
    Docker->>DB: Start (no dependencies)
    DB-->>Docker: Running (port 5432)
    
    Docker->>Kafka: Start (wait for ZK)
    Note over Kafka: depends_on: zookeeper
    Kafka-->>Docker: Running (port 9092)
    
    Docker->>App: Start (wait for DB, Kafka)
    Note over App: depends_on: postgres, kafka
    App->>DB: Test connection
    DB-->>App: Connection OK
    App->>Kafka: Connect to broker
    Kafka-->>App: Broker ready
    App-->>Docker: Running (port 8080)
    
    Docker->>Prom: Start (no dependencies)
    Prom-->>Docker: Running (port 9090)
    
    Docker->>Graf: Start (wait for Prom)
    Note over Graf: depends_on: prometheus
    Graf-->>Docker: Running (port 3000)
```

**Startup Order:**
1. Zookeeper (independent)
2. PostgreSQL (independent)
3. Kafka (after Zookeeper ready)
4. Order API (after PostgreSQL + Kafka ready)
5. Prometheus (independent)
6. Grafana (after Prometheus ready)

---

## Health Check Strategy

```mermaid
flowchart TB
    Start([Docker Compose Up])
    
    ZKHealth{Zookeeper<br/>Port 2181 listening?}
    DBHealth{PostgreSQL<br/>pg_isready?}
    KafkaHealth{Kafka<br/>Broker available?}
    AppHealth{Application<br/>/actuator/health UP?}
    PromHealth{Prometheus<br/>Port 9090 ready?}
    GrafHealth{Grafana<br/>Port 3000 ready?}
    
    AllHealthy[All Services Healthy]
    Retry[Wait & Retry]
    
    Start --> ZKHealth
    Start --> DBHealth
    
    ZKHealth -->|No| Retry
    ZKHealth -->|Yes| KafkaHealth
    
    DBHealth -->|No| Retry
    DBHealth -->|Yes| AppHealth
    
    KafkaHealth -->|No| Retry
    KafkaHealth -->|Yes| AppHealth
    
    AppHealth -->|No| Retry
    AppHealth -->|Yes| PromHealth
    
    PromHealth -->|Yes| GrafHealth
    
    GrafHealth -->|Yes| AllHealthy
    
    Retry --> ZKHealth
    Retry --> DBHealth
    
    style AllHealthy fill:#e1f5e1
    style Retry fill:#ffe1e1
```

**Health Check Configuration:**
- **Interval:** Every 30 seconds
- **Timeout:** 10 seconds
- **Retries:** 3 attempts
- **Start Period:** 60 seconds grace period

---

## Port Mapping Summary

| Service | Container Port | Host Port | Protocol | Purpose |
|---------|---------------|-----------|----------|---------|
| Order API | 8080 | 8080 | HTTP | REST API & Swagger UI |
| PostgreSQL | 5432 | 5432 | TCP | Database connections |
| Kafka | 9092 | 9092 | TCP | Kafka broker |
| Zookeeper | 2181 | 2181 | TCP | Kafka coordination |
| Prometheus | 9090 | 9090 | HTTP | Metrics queries |
| Grafana | 3000 | 3000 | HTTP | Dashboards |

**Access Points:**
- 🌐 API: http://localhost:8080
- 📚 Swagger: http://localhost:8080/swagger-ui.html
- 🏥 Health: http://localhost:8080/actuator/health
- 📊 Metrics: http://localhost:8080/actuator/prometheus
- 🔍 Prometheus: http://localhost:9090
- 📈 Grafana: http://localhost:3000

---

## Environment Configuration

### Development (docker-compose.yml)
```yaml
environment:
  SPRING_PROFILES_ACTIVE: docker
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/orderfulfillment
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
  LOGGING_LEVEL_COM_MIDLEVEL: DEBUG
```

### Production (Future - Kubernetes)
```yaml
environment:
  SPRING_PROFILES_ACTIVE: prod
  SPRING_DATASOURCE_URL: ${DB_CONNECTION_STRING}
  SPRING_KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BROKERS}
  LOGGING_LEVEL_COM_MIDLEVEL: INFO
  MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health,info,prometheus
```

---

## Resource Limits

```yaml
services:
  order-fulfillment-api:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
  
  postgres:
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 256M
        reservations:
          cpus: '0.25'
          memory: 128M
  
  kafka:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

**Total Stack Requirements:**
- **CPU:** ~3 cores
- **Memory:** ~2GB RAM
- **Disk:** ~5GB (with data volumes)

---

## Scaling Strategy (Future)

### Horizontal Scaling (Kubernetes)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-fulfillment-api
spec:
  replicas: 3  # Multiple instances
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

### Load Balancing
```mermaid
flowchart LR
    LB[Load Balancer<br/>Nginx/Ingress]
    POD1[API Pod 1]
    POD2[API Pod 2]
    POD3[API Pod 3]
    
    DB[(PostgreSQL<br/>Primary-Replica)]
    KAFKA[(Kafka Cluster<br/>3 Brokers)]
    
    LB --> POD1
    LB --> POD2
    LB --> POD3
    
    POD1 --> DB
    POD2 --> DB
    POD3 --> DB
    
    POD1 --> KAFKA
    POD2 --> KAFKA
    POD3 --> KAFKA
```

---

## Deployment Commands

### Start Stack
```bash
# Build and start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f order-fulfillment-api
```

### Stop Stack
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean state)
docker-compose down -v
```

### Rebuild Application
```bash
# Rebuild and restart application only
docker-compose up -d --build order-fulfillment-api
```

### Database Operations
```bash
# Access PostgreSQL
docker-compose exec postgres psql -U postgres -d orderfulfillment

# Backup database
docker-compose exec postgres pg_dump -U postgres orderfulfillment > backup.sql

# Restore database
docker-compose exec -T postgres psql -U postgres orderfulfillment < backup.sql
```

### Kafka Operations
```bash
# List topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# View messages
docker-compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-events --from-beginning
```

---

## Monitoring & Observability

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
docker-compose exec postgres pg_isready

# Kafka health
docker-compose exec kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

### Metrics
```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### Logs
```bash
# Application logs
docker-compose logs -f order-fulfillment-api

# All service logs
docker-compose logs -f

# Last 100 lines
docker-compose logs --tail=100 order-fulfillment-api
```

---

## Future Enhancements

### 🚀 Production Readiness
- [ ] Migrate to Kubernetes (K8s)
- [ ] Add Redis for caching
- [ ] Implement API Gateway (Kong/Ambassador)
- [ ] Add service mesh (Istio/Linkerd)
- [ ] Set up centralized logging (ELK Stack)
- [ ] Implement distributed tracing (Jaeger)

### 🔒 Security
- [ ] TLS/SSL certificates
- [ ] Network policies
- [ ] Secret management (Vault)
- [ ] Container scanning (Trivy)

### 📊 Observability
- [ ] Custom Grafana dashboards
- [ ] Alert rules in Prometheus
- [ ] PagerDuty integration
- [ ] Log aggregation (Elasticsearch)

---

## References
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Kafka Docker Guide](https://kafka.apache.org/quickstart)
- [Prometheus Configuration](https://prometheus.io/docs/prometheus/latest/configuration/configuration/)
