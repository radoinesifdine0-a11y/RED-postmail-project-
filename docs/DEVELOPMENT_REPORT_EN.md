# StoreParcelAPI - Development Report

## Project Overview
Building a high-performance REST API for retail stores to search customer parcels.

**Target Performance**: P95 response time < 60ms with 50,000+ parcels

**Date**: December 30, 2025

---

## Checkpoint Status

| Checkpoint | Description | Status |
|------------|-------------|--------|
| 1 | Application runs, health check works | ✅ COMPLETE |
| 2 | Generate 50,000 test parcels | ✅ COMPLETE |
| 3 | Gatling tests run | ✅ COMPLETE |
| 4 | Measure baseline performance | ✅ COMPLETE |
| 5 | P95 < 200ms | ✅ COMPLETE |
| 6 | P95 < 100ms | ✅ COMPLETE |
| 7 | P95 < 60ms (TARGET) | ✅ COMPLETE (58ms) |

---

## Progress Log

### Step 1: Maven Project Structure (pom.xml) ✅

**What we created**: The `pom.xml` file - Maven's project configuration.

**Why**: Maven manages our dependencies (libraries) and builds the project. Without it, we'd have to manually download every JAR file.

**Key dependencies added**:
| Dependency | Purpose |
|-----------|---------|
| spring-boot-starter-web | REST API support |
| spring-boot-starter-data-mongodb | MongoDB database connection |
| spring-boot-starter-validation | Request validation |
| spring-boot-starter-hateoas | HAL+JSON pagination format |
| lombok | Auto-generate getters/setters |

---

### Step 2: Main Application Class ✅

**File**: `StoreParcelsApplication.java`

**What it does**: Entry point of the application. The `@SpringBootApplication` annotation tells Spring to:
1. Scan for components (controllers, services, repositories)
2. Auto-configure based on dependencies
3. Start the embedded Tomcat server

```java
@SpringBootApplication
public class StoreParcelsApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoreParcelsApplication.class, args);
    }
}
```

---

### Step 3: Package Structure ✅

```
src/main/java/com/kantic/storeParcelsWS/
├── controller/   → Handles HTTP requests
├── service/      → Business logic
├── repository/   → Database operations
├── model/        → Entity classes (data structure)
├── dto/          → Request/Response objects
├── exception/    → Custom error handling
└── config/       → Configuration classes
```

**Why this structure?**: Separation of concerns. Each layer has one responsibility:
- Controller: "What endpoint was called?"
- Service: "What business rules apply?"
- Repository: "How do I query the database?"

---

### Step 4: Docker Setup ✅

**Files created**:
- `docker-compose.yml` - Orchestrates MongoDB container
- `Dockerfile` - Builds our application container

**How to use**:
```bash
# Start MongoDB
docker-compose up -d

# Check it's running
docker ps
```

**Why Docker?**: Consistent environment. MongoDB runs the same way on every developer's machine.

---

### Step 5: Application Configuration ✅

**File**: `application.yml`

**Profiles explained**:
- `local`: Development on your machine (MongoDB on localhost)
- `docker`: Running inside Docker (MongoDB via container name)
- `gcp`: Cloud deployment (MongoDB URI from environment variable)

**Why profiles?**: Same code, different configurations. No code changes needed between environments.

---

### Step 6: Model/Entity Classes ✅

**Files created**:
- `ParcelStatus.java` - Enum with all possible statuses
- `Customer.java` - Embedded document for customer info
- `StorageLocation.java` - Where parcel is stored in the store
- `Parcel.java` - Main entity with all fields

**Key annotations**:
| Annotation | Meaning |
|-----------|---------|
| `@Document` | This class = MongoDB collection |
| `@Id` | MongoDB's internal ID |
| `@Field("name")` | Map to different field name in DB |
| `@Indexed` | Create index for fast queries |
| `@CompoundIndex` | Multi-field index |

**Why indexes matter**: Without indexes, MongoDB scans ALL 50,000 documents for every query. With indexes, it jumps directly to matching documents → P95 < 60ms possible!

---

### Step 7: Repository Layer ✅

**Files created**:
- `ParcelRepository.java` - Main repository interface
- `ParcelRepositoryCustom.java` - Custom search interface
- `ParcelRepositoryCustomImpl.java` - Search implementation

**What is a Repository?**: Interface that defines database operations.

**Spring Data magic**: Just define method names, Spring generates the implementation!

```java
Optional<Parcel> findByParcelId(String parcelId);
// Auto-generates: db.parcels.findOne({parcelId: value})
```

**Custom search implementation**: Handles dynamic multi-criteria search with:
- Multiple operators (EQ, NE, IN, LIKE, LE, GE)
- Sorting
- Pagination

---

### Step 8: DTO Layer ✅

**Files created**:
- `SearchRequestDTO.java` - Search request structure
- `CriteriaDTO.java` - Single search criterion
- `SortDTO.java` - Sort option
- `PaginationDTO.java` - Pagination parameters
- `ParcelResponseDTO.java` - Parcel in API response
- `CustomerResponseDTO.java` - Customer in API response

**Why DTOs?**
- Control what data is exposed to the API
- Decouple API contract from database schema
- Hide internal fields (like MongoDB's `_id`)

---

### Step 9: Service Layer ✅

**Files created**:
- `ParcelService.java` - Service interface
- `ParcelServiceImpl.java` - Service implementation

**What a Service does**:
- Contains business logic
- Converts entities to DTOs
- Sits between Controller and Repository

---

### Step 10: Controller Layer ✅

**File**: `ParcelController.java`

**Endpoints created**:
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/store_parcels/health` | Health check |
| POST | `/store_parcels/search` | Search parcels |
| GET | `/store_parcels/parcel/{id}` | Get single parcel |

---

### Step 11: Environment Setup ✅

**Installed**:
- Java 21 (OpenJDK via Homebrew)
- Docker Desktop
- Maven 3.9.12

**Commands used**:
```bash
# Install Java
brew install openjdk@21

# Configure Java in ~/.zshrc
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

# Install Docker
brew install --cask docker

# Install Maven
brew install maven
```

---

### Step 12: Application Testing ✅

**MongoDB started**:
```bash
docker-compose up -d
```

**Application started**:
```bash
mvn spring-boot:run
```

**Startup log** (1.476 seconds):
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.4.4)

Started StoreParcelsApplication in 1.476 seconds
```

**Health check test**:
```bash
curl http://localhost:8080/store_parcels/health
```

**Response**:
```json
{
    "status": "UP",
    "timestamp": 1767110196871
}
```

**Search test** (empty database):
```bash
curl -X POST http://localhost:8080/store_parcels/search \
  -H "Content-Type: application/json" \
  -d '{"criterias":[],"pagination":{"page":1,"pageSize":20}}'
```

**Response**:
```json
{
    "_embedded": {
        "storeParcels": []
    },
    "page": {
        "number": 1,
        "size": 20,
        "totalPages": 0,
        "totalElements": 0
    }
}
```

---

### Step 13: Generate 50,000 Test Parcels (Checkpoint 2) ✅

**Files created**:
- `TestDataGenerator.java` - Generates random parcels in batches
- `AdminController.java` - Endpoints for data management (local/docker only)

**What TestDataGenerator does**:
1. Creates parcels with random but realistic data
2. Uses batch inserts (1000 at a time) for speed
3. Creates indexes after data insertion

**Admin endpoints** (only in local/docker profiles):
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/store_parcels/admin/generate-test-data` | Generate 50,000 parcels |
| DELETE | `/store_parcels/admin/clear-data` | Clear all data |
| GET | `/store_parcels/admin/count` | Count parcels |

**Generation results**:
```bash
curl -X POST http://localhost:8080/store_parcels/admin/generate-test-data
```
```json
{
    "status": "success",
    "parcelsGenerated": 50000,
    "durationMs": 4337,
    "message": "Generated 50000 parcels in 4337ms"
}
```

**Search tests**:
| Test | Query | Results |
|------|-------|---------|
| Store ST001 | `deliveryStoreId = ST001` | 4,975 parcels |
| Customer "John" | `customer.firstName LIKE John` | 1,984 parcels |
| Multi-criteria | `ST001 + READY_FOR_PICKUP` | 619 parcels |

**Data distribution** (10 stores, 8 statuses):
- ~5,000 parcels per store
- ~6,250 parcels per status

---

## Files Created Summary

| Category | Files |
|----------|-------|
| **Configuration** | `pom.xml`, `application.yml`, `docker-compose.yml`, `Dockerfile` |
| **Main** | `StoreParcelsApplication.java` |
| **Model** | `Parcel.java`, `Customer.java`, `StorageLocation.java`, `ParcelStatus.java` |
| **Repository** | `ParcelRepository.java`, `ParcelRepositoryCustom.java`, `ParcelRepositoryCustomImpl.java` |
| **Service** | `ParcelService.java`, `ParcelServiceImpl.java` |
| **Controller** | `ParcelController.java`, `AdminController.java` |
| **DTO** | `SearchRequestDTO.java`, `CriteriaDTO.java`, `SortDTO.java`, `PaginationDTO.java`, `ParcelResponseDTO.java`, `CustomerResponseDTO.java` |
| **Utility** | `TestDataGenerator.java` |
| **Documentation** | `README.md`, `DEVELOPMENT_REPORT_EN.md`, `RAPPORT_DEVELOPPEMENT_FR.md` |

**Total**: 22 files created

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT                                │
│                   (Postman, curl, etc.)                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      CONTROLLER                              │
│    ParcelController.java                                    │
│    - POST /store_parcels/search                             │
│    - GET  /store_parcels/health                             │
│    - GET  /store_parcels/parcel/{id}                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       SERVICE                                │
│    ParcelService.java                                       │
│    - Business logic                                         │
│    - Entity → DTO conversion                                │
│    - Query delegation                                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      REPOSITORY                              │
│    ParcelRepository.java                                    │
│    - Database queries                                       │
│    - Pagination                                             │
│    - Sorting                                                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       MONGODB                                │
│    Collection: parcels                                      │
│    Indexes: parcelId, store+status, customer name           │
└─────────────────────────────────────────────────────────────┘
```

---

### Step 14: Gatling Performance Testing (Checkpoints 3-7) ✅

**Files created**:
- `ParcelSearchSimulation.java` - Full load test with multiple scenarios
- `BaselineSimulation.java` - Quick baseline test
- `QuickTestSimulation.java` - Fast verification test

**What is Gatling?**
Gatling is a load testing tool that:
- Simulates many concurrent users hitting your API
- Measures response times, throughput, error rates
- Generates detailed HTML reports with charts

**Dependencies added to pom.xml**:
```xml
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>
    <version>3.10.3</version>
    <scope>test</scope>
</dependency>
```

**Test Scenarios**:
| Scenario | Description | Rate |
|----------|-------------|------|
| Simple Search | Search all parcels | 2 req/sec |
| Store Search | Search by store ID | 4 req/sec |
| Store + Status | Multi-criteria search | 4 req/sec |
| Customer Search | LIKE query on name | 2 req/sec |
| Random Store | Random store selection | 8 req/sec |

**Performance Results**:
| Metric | Result | Target |
|--------|--------|--------|
| **P95 Response Time** | **58ms** | < 60ms ✅ |
| P99 Response Time | 64ms | - |
| Mean Response Time | 18ms | - |
| Min Response Time | 5ms | - |
| Max Response Time | 88ms | - |
| Success Rate | 100% | > 99% ✅ |
| Total Requests | 600 | - |

**Run Gatling tests**:
```bash
# Full load test (30 seconds)
mvn gatling:test -Dgatling.simulationClass=com.kantic.storeParcelsWS.simulation.ParcelSearchSimulation

# Quick test (5 seconds)
mvn gatling:test -Dgatling.simulationClass=com.kantic.storeParcelsWS.simulation.QuickTestSimulation
```

Reports are generated in: `target/gatling/*/index.html`

---

## Next Steps

- [x] Generate 50,000 test parcels (Checkpoint 2) ✅
- [x] Add Gatling for performance testing (Checkpoint 3) ✅
- [x] Measure baseline performance (Checkpoint 4) ✅
- [x] Achieve P95 < 60ms (Checkpoints 5-7) ✅ **ACHIEVED: 58ms**
- [ ] Part 2: Write endpoints (POST, PUT, DELETE)
- [ ] Part 3: MCP Controller
- [ ] Part 4: GCP Deployment

---

## Useful Commands

```bash
# Start MongoDB
docker-compose up -d

# Stop MongoDB
docker-compose down

# Run the application
mvn spring-boot:run

# Build the application
mvn clean package

# Test health endpoint
curl http://localhost:8080/store_parcels/health

# Test search endpoint
curl -X POST http://localhost:8080/store_parcels/search \
  -H "Content-Type: application/json" \
  -d '{"criterias":[],"pagination":{"page":1,"pageSize":20}}'
```
