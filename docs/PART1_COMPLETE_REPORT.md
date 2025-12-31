# StoreParcelAPI - Part 1 Complete Report

## Project Goal
Build a **high-performance REST API** that allows retail stores to search for customer parcels.

**Performance Target**: P95 response time < 60ms with 50,000+ parcels

**What does P95 mean?**
- P95 = 95th percentile
- If P95 is 58ms, it means 95% of all requests complete in 58ms or less
- Only 5% of requests take longer than 58ms
- This is a better metric than "average" because it shows worst-case performance for most users

---

## What We Built

### 1. Project Structure

```
storeparcelapi/
├── src/main/java/com/kantic/storeParcelsWS/
│   ├── StoreParcelsApplication.java    # Entry point
│   ├── controller/
│   │   ├── ParcelController.java       # REST endpoints
│   │   └── AdminController.java        # Test data generation
│   ├── service/
│   │   ├── ParcelService.java          # Business logic interface
│   │   └── ParcelServiceImpl.java      # Business logic implementation
│   ├── repository/
│   │   ├── ParcelRepository.java       # Database interface
│   │   ├── ParcelRepositoryCustom.java # Custom search interface
│   │   └── ParcelRepositoryCustomImpl.java # Search implementation
│   ├── model/
│   │   ├── Parcel.java                 # Main entity
│   │   ├── Customer.java               # Customer info
│   │   ├── StorageLocation.java        # Storage info
│   │   └── ParcelStatus.java           # Status enum
│   ├── dto/
│   │   ├── SearchRequestDTO.java       # Search request structure
│   │   ├── CriteriaDTO.java            # Search criteria
│   │   ├── SortDTO.java                # Sort options
│   │   ├── PaginationDTO.java          # Pagination params
│   │   ├── ParcelResponseDTO.java      # Parcel response
│   │   └── CustomerResponseDTO.java    # Customer response
│   └── util/
│       └── TestDataGenerator.java      # Generates 50,000 parcels
├── src/test/java/com/kantic/storeParcelsWS/simulation/
│   ├── ParcelSearchSimulation.java     # Full Gatling load test
│   ├── BaselineSimulation.java         # Baseline test
│   └── QuickTestSimulation.java        # Quick verification test
├── src/main/resources/
│   └── application.yml                 # Configuration
├── pom.xml                             # Maven dependencies
├── docker-compose.yml                  # MongoDB container
└── Dockerfile                          # Application container
```

---

## Architecture Explained (For Beginners)

### The Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENT (Browser, Postman, curl)         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      CONTROLLER LAYER                        │
│  "What endpoint was called? What data was sent?"            │
│                                                              │
│  ParcelController.java                                       │
│  - Receives HTTP requests                                    │
│  - Validates input                                           │
│  - Returns HTTP responses                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       SERVICE LAYER                          │
│  "What business rules apply? How do I transform data?"      │
│                                                              │
│  ParcelServiceImpl.java                                      │
│  - Contains business logic                                   │
│  - Converts entities to DTOs                                 │
│  - Orchestrates operations                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      REPOSITORY LAYER                        │
│  "How do I query the database?"                             │
│                                                              │
│  ParcelRepository.java                                       │
│  - Database queries                                          │
│  - CRUD operations                                           │
│  - Custom search with criteria                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        DATABASE                              │
│                        MongoDB                               │
│                                                              │
│  Collection: parcels (50,000 documents)                     │
│  Indexes: parcelId, store+status, customer name             │
└─────────────────────────────────────────────────────────────┘
```

### Why This Structure?

**Separation of Concerns**: Each layer has ONE job:
- Controller: Handle HTTP (don't put business logic here!)
- Service: Business rules (don't put database queries here!)
- Repository: Database access (don't put HTTP handling here!)

**Benefits**:
- Easy to test each layer independently
- Easy to change one layer without affecting others
- Code is organized and easy to find

---

## API Endpoints Created

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/store_parcels/health` | Health check |
| POST | `/store_parcels/search` | Search parcels with criteria |
| GET | `/store_parcels/parcel/{id}` | Get single parcel by ID |
| GET | `/store_parcels/browse` | Browser-friendly search |
| POST | `/store_parcels/admin/generate-test-data` | Generate 50,000 test parcels |
| DELETE | `/store_parcels/admin/clear-data` | Delete all parcels |
| GET | `/store_parcels/admin/count` | Count parcels |

### Search Request Example

```json
{
    "criterias": [
        {"field": "deliveryStoreId", "operator": "EQ", "value": "ST001"},
        {"field": "parcelStatus", "operator": "EQ", "value": "READY_FOR_PICKUP"}
    ],
    "sort": {
        "field": "expirationDateTime",
        "direction": "ASC"
    },
    "pagination": {
        "page": 1,
        "pageSize": 20
    }
}
```

### Available Operators

| Operator | Meaning | Example |
|----------|---------|---------|
| EQ | Equals | `status = "PENDING"` |
| NE | Not Equals | `status != "CANCELLED"` |
| IN | In list | `status IN ["PENDING", "READY"]` |
| NIN | Not in list | `status NOT IN ["CANCELLED"]` |
| LIKE | Contains (regex) | `name LIKE "John"` |
| LE | Less or Equal | `date <= "2025-01-01"` |
| GE | Greater or Equal | `date >= "2025-01-01"` |

---

## Database Design

### MongoDB Indexes (Why They Matter)

**Without indexes**: MongoDB scans ALL 50,000 documents for every query = SLOW!

**With indexes**: MongoDB jumps directly to matching documents = FAST!

```java
@CompoundIndexes({
    // Index 1: For store + status queries (most common)
    @CompoundIndex(
        name = "store_status_expiration_idx",
        def = "{'deliveryStoreId': 1, 'parcelStatus': 1, 'expirationDateTime': 1}"
    ),
    // Index 2: For customer name searches
    @CompoundIndex(
        name = "customer_name_idx",
        def = "{'customer.firstName': 1, 'customer.lastName': 1}"
    )
})
```

**Why compound indexes?**
- A compound index on `{store, status}` can handle:
  - Queries on `store` alone
  - Queries on `store + status` together
  - But NOT queries on `status` alone (order matters!)

---

## Test Data Generation

We generated **50,000 realistic parcels** with:
- 10 stores (ST001-ST010)
- 8 statuses (PENDING, IN_TRANSIT, DELIVERED, etc.)
- Random customer names
- Random expiration dates

**Distribution**:
- ~5,000 parcels per store
- ~6,250 parcels per status

**Generation time**: 4.3 seconds (batch inserts of 1,000)

---

# Gatling Performance Testing - Detailed Explanation

## What is Gatling?

Gatling is a **load testing tool** that:
1. Simulates many users hitting your API simultaneously
2. Measures response times under load
3. Generates beautiful HTML reports with charts

**Why use Gatling?**
- Real-world APIs don't have 1 user - they have hundreds or thousands
- You need to know: "Will my API be fast when 100 people use it at once?"
- Gatling answers this question

---

## Our Gatling Test Configuration

### Test Scenarios

We created **5 different scenarios** to simulate real-world usage:

| Scenario | What It Does | Rate | Why This Rate? |
|----------|--------------|------|----------------|
| **Simple Search** | Search all parcels (no filters) | 2 req/sec | Less common - users usually filter |
| **Store Search** | Search by store ID | 4 req/sec | Very common - store employees check their store |
| **Store + Status** | Search by store AND status | 4 req/sec | Very common - "show me ready parcels in ST001" |
| **Customer Search** | Search by customer name (LIKE) | 2 req/sec | Less common, but more expensive (regex) |
| **Random Store** | Random store selection | 8 req/sec | Simulates different stores hitting API |

**Total load**: 20 requests/second for 30 seconds = **600 requests**

### Why These Settings?

1. **20 requests/second**: This simulates a realistic load for a retail API
   - 10 stores
   - Each store might have 2 employees searching
   - Each employee makes ~1 request per second

2. **30 second duration**: Long enough to:
   - Warm up the JVM (Java Virtual Machine)
   - Get stable measurements
   - Not so long that we waste time

3. **Mixed scenarios**: Real APIs receive different types of queries
   - Some are simple (fast)
   - Some are complex (slower)
   - We need to test both

---

## Understanding the Gatling Code

### HTTP Configuration

```java
HttpProtocolBuilder httpProtocol = http
    .baseUrl("http://127.0.0.1:8080")    // Base URL for all requests
    .acceptHeader("application/json")    // We expect JSON responses
    .contentTypeHeader("application/json"); // We send JSON
```

### Scenario Definition

```java
ScenarioBuilder storeSearch = scenario("Store Search")
    .exec(
        http("Search by Store ST001")           // Name for reports
            .post("/store_parcels/search")      // POST request
            .body(StringBody("{...json...}"))   // Request body
            .check(status().is(200))            // Verify success
    );
```

### Load Injection

```java
setUp(
    // 4 users per second for 30 seconds
    storeSearch.injectOpen(constantUsersPerSec(4).during(30))
).protocols(httpProtocol);
```

**What does `constantUsersPerSec(4).during(30)` mean?**
- Every second, 4 new "virtual users" start
- Each user makes their request and finishes
- This continues for 30 seconds
- Total: 4 × 30 = 120 requests for this scenario

### Assertions

```java
.assertions(
    global().responseTime().percentile(95.0).lt(60),  // P95 < 60ms
    global().successfulRequests().percent().gt(99.0)  // >99% success
)
```

**What happens if assertions fail?**
- The test fails
- You know your API doesn't meet requirements
- Time to optimize!

---

## Test Results Explained

### Our Results

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                        600 (OK=600    KO=0     )
> min response time                                      5 (OK=5      KO=-     )
> max response time                                     88 (OK=88     KO=-     )
> mean response time                                    18 (OK=18     KO=-     )
> std deviation                                         16 (OK=16     KO=-     )
> response time 50th percentile                         11 (OK=11     KO=-     )
> response time 75th percentile                         16 (OK=16     KO=-     )
> response time 95th percentile                         58 (OK=58     KO=-     )
> response time 99th percentile                         64 (OK=64     KO=-     )
> mean requests/sec                                     20 (OK=20     KO=-     )
================================================================================
```

### What Each Metric Means

| Metric | Value | Meaning |
|--------|-------|---------|
| **request count** | 600 (OK=600, KO=0) | 600 requests, all succeeded, 0 failed |
| **min response time** | 5ms | Fastest request took 5 milliseconds |
| **max response time** | 88ms | Slowest request took 88 milliseconds |
| **mean response time** | 18ms | Average of all requests |
| **std deviation** | 16ms | How spread out the times are |
| **50th percentile (P50)** | 11ms | Half of requests faster than this |
| **75th percentile (P75)** | 16ms | 75% of requests faster than this |
| **95th percentile (P95)** | 58ms | 95% of requests faster than this ✅ |
| **99th percentile (P99)** | 64ms | 99% of requests faster than this |
| **mean requests/sec** | 20 | Throughput: 20 requests processed per second |

### Why P95 is the Key Metric

- **Average (mean)** can be misleading - one very slow request doesn't show up much
- **P95** shows what "almost everyone" experiences
- If P95 = 58ms, 95 out of 100 users get responses in 58ms or less
- Only 5 users might wait longer

---

## Performance Achievement

| Target | Actual | Status |
|--------|--------|--------|
| P95 < 60ms | **58ms** | ✅ PASSED |
| Success rate > 99% | **100%** | ✅ PASSED |

**Why did we achieve good performance?**

1. **MongoDB Indexes**: Queries use indexes, not full collection scans
2. **Batch Processing**: Data generation used batch inserts (1,000 at a time)
3. **Efficient DTOs**: Only return needed fields, not entire documents
4. **Connection Pooling**: MongoDB driver reuses connections
5. **JVM Warmup**: After warmup, JIT compilation optimizes hot paths

---

## How to Run the Tests

### Prerequisites
1. MongoDB running: `docker-compose up -d`
2. Application running: `mvn spring-boot:run`
3. Test data generated: `curl -X POST http://localhost:8080/store_parcels/admin/generate-test-data`

### Run Gatling Tests

```bash
# Full load test (30 seconds, 600 requests)
mvn gatling:test -Dgatling.simulationClass=com.kantic.storeParcelsWS.simulation.ParcelSearchSimulation

# Quick test (5 seconds, 50 requests)
mvn gatling:test -Dgatling.simulationClass=com.kantic.storeParcelsWS.simulation.QuickTestSimulation

# Baseline test (10 users, 100 requests)
mvn gatling:test -Dgatling.simulationClass=com.kantic.storeParcelsWS.simulation.BaselineSimulation
```

### View Reports

After tests complete, open the HTML report:
```
target/gatling/[simulation-name-timestamp]/index.html
```

The report includes:
- Response time distribution charts
- Requests per second over time
- Active users over time
- Response time percentiles
- Detailed request/response logs

---

## Key Learnings from Part 1

### Technical Concepts Learned

1. **Spring Boot Architecture**
   - Controllers handle HTTP
   - Services contain business logic
   - Repositories access data

2. **MongoDB with Spring Data**
   - `@Document` annotation for entities
   - `@Indexed` and `@CompoundIndex` for performance
   - Custom repository implementation for complex queries

3. **DTOs (Data Transfer Objects)**
   - Separate API contract from database schema
   - Control what data is exposed
   - Validation annotations (`@NotBlank`, `@Min`, `@Max`)

4. **Performance Testing with Gatling**
   - Simulate realistic load
   - Measure percentiles, not just averages
   - Use assertions to enforce requirements

5. **Docker for Development**
   - Consistent environment across machines
   - Easy setup with `docker-compose up -d`

### Best Practices Applied

1. **Separation of Concerns**: Each class has one responsibility
2. **Interface-based Design**: `ParcelService` interface with `ParcelServiceImpl`
3. **Batch Operations**: Insert 1,000 records at a time, not 50,000 one by one
4. **Proper Indexing**: Create indexes for common query patterns
5. **Comprehensive Testing**: Test multiple scenarios, not just one

---

## Files Summary

| Category | Count | Files |
|----------|-------|-------|
| Configuration | 4 | pom.xml, application.yml, docker-compose.yml, Dockerfile |
| Main Application | 1 | StoreParcelsApplication.java |
| Controllers | 2 | ParcelController.java, AdminController.java |
| Services | 2 | ParcelService.java, ParcelServiceImpl.java |
| Repositories | 3 | ParcelRepository.java, ParcelRepositoryCustom.java, ParcelRepositoryCustomImpl.java |
| Models | 4 | Parcel.java, Customer.java, StorageLocation.java, ParcelStatus.java |
| DTOs | 6 | SearchRequestDTO, CriteriaDTO, SortDTO, PaginationDTO, ParcelResponseDTO, CustomerResponseDTO |
| Utilities | 1 | TestDataGenerator.java |
| Gatling Tests | 3 | ParcelSearchSimulation.java, BaselineSimulation.java, QuickTestSimulation.java |
| Documentation | 4 | README.md, DEVELOPMENT_REPORT_EN.md, RAPPORT_DEVELOPPEMENT_FR.md, PART1_COMPLETE_REPORT.md |

**Total: 30 files**

---

## Checkpoints Completed

| # | Checkpoint | Status | Details |
|---|------------|--------|---------|
| 1 | Application runs | ✅ | Starts in 1.4 seconds |
| 2 | Generate 50,000 parcels | ✅ | Generated in 4.3 seconds |
| 3 | Gatling tests run | ✅ | 3 simulation classes created |
| 4 | Measure baseline | ✅ | P95 = 32ms (quick test) |
| 5 | P95 < 200ms | ✅ | Achieved: 58ms |
| 6 | P95 < 100ms | ✅ | Achieved: 58ms |
| 7 | P95 < 60ms | ✅ | Achieved: **58ms** |

---

## Ready for Part 2!

Part 1 is complete. We have:
- A working REST API for searching parcels
- 50,000 test parcels in MongoDB
- Performance verified at P95 < 60ms
- Comprehensive Gatling test suite

**Next**: Part 2 will add write endpoints (POST, PUT, DELETE) for creating, updating, and deleting parcels.
