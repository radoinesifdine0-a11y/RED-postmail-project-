# StoreParcelAPI

# Store Parcel API - Product Requirements Document

## Document Information

**Project Name:** Store Parcel API (storeParcelsWS) **Version:** 1.0.0 **Last Updated:** 2025 **Purpose:** Training exercise - Build a high-performance parcel search API from scratch


---

## 1. Business Requirements

### 1.1 Overview

Build a RESTful web service that allows retail stores to search for customer parcels awaiting pickup. The system must handle high-volume searches across multiple stores with sub-60ms response times.

### 1.2 Business Goals

* **Primary Goal:** Enable store staff to quickly locate customer parcels
* **Performance SLA:** 95th percentile response time must be under 60ms
* **Scalability:** Support searches across 50,000+ parcels
* **User Experience:** Fast, intuitive search with multiple filter options

### 1.3 Success Metrics

| Metric | Target |
|----|----|
| P95 Response Time | <60ms |
| Success Rate | ≥99% |
| Concurrent Users | 50 users minimum |
| Search Accuracy | 100% |
| Uptime | 99.9% |


---

## 2. Functional Requirements

### 2.1 Search Functionality

**Primary Use Case:** Store staff searches for parcels using various criteria

**Required Search Capabilities:**


1. **By Store Code** - Find all parcels at a specific store
2. **By Customer Name** - Search by first name and/or last name
3. **By Parcel ID** - Direct lookup by unique parcel identifier
4. **By Order ID** - Find parcels associated with an order
5. **By Status** - Filter by parcel status (ready, in transit, etc.)
6. **By Customer Contact** - Search by email or phone number

**Multi-Criteria Search:**

* Support combining multiple search criteria with AND logic
* Example: Store + Customer Name + Status

**Sorting:**

* Sort results by expiration date (ascending/descending)
* Sort results by customer name (ascending/descending)
* Sort results by status update date (ascending/descending)
* Support multiple sort fields with priority order

**Pagination:**

* Default page size: 20 items
* Support custom page sizes (10, 20, 50, 100)
* Include total count and page metadata

### 2.2 Data Operations

**Required Operations:**

* Search with filters and sorting
* Count matching parcels
* Retrieve individual parcel details
* Health check endpoint

**Not Required:**

* Create/update/delete parcels (read-only API)
* Authentication or authorization
* User management


---

## 3. Non-Functional Requirements

### 3.1 Performance

**Primary SLA:**

* **P95 Response Time:** <60ms for all search operations
* **P99 Response Time:** <100ms target
* **P50 Response Time:** <30ms target

**Load Requirements:**

* Handle 50 concurrent users
* Support sustained load of 100 requests/second
* Graceful degradation under peak load

**Performance Under Constraints:**

* Database contains 50,000+ parcels
* Search across all stores simultaneously
* Complex multi-criteria queries with sorting

### 3.2 Scalability

* Horizontal scalability not required (single instance acceptable)
* Database must efficiently handle 50,000+ documents
* Optimized for read-heavy workload

### 3.3 Reliability

* 99.9% uptime target
* Graceful error handling and recovery
* Comprehensive logging for debugging

### 3.4 Usability

* RESTful API design
* Clear error messages
* Standard HTTP status codes
* JSON request/response format


---

## 4. Technical Constraints

### 4.1 Technology Stack

**Mandatory Technologies:**

| Component | Technology | Version |
|----|----|----|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.4.4 |
| **Database** | MongoDB | 6.0+ |
| **Build Tool** | Maven | 3.x |
| **Container** | Docker | Latest |

**Why These Choices:**

* Java 21: Virtual threads for high concurrency
* Spring Boot 3.4.4: Modern framework with excellent MongoDB support
* MongoDB: Flexible schema, fast reads, good indexing capabilities
* Docker: Consistent deployment environment

### 4.2 Deployment Constraints

* **Local Deployment Only** - No cloud services (GCP, AWS, Azure)
* **Local MongoDB** - Standard MongoDB 6.0+, NOT MongoDB Atlas
* **No MongoDB Atlas Features** - Cannot use Atlas Search, Atlas Triggers, etc.
* **Docker Support** - Must run in Docker containers
* **No Authentication** - No OAuth2, JWT, or API keys required

### 4.3 Database Constraints

**Standard MongoDB Features Only:**

* Standard indexes (single field, compound, text)
* Aggregation pipeline
* Standard query operators ($eq, $in, $regex, etc.)
* Standard sorting and pagination

**NOT Available (Atlas-only features):**

* `$search` operator
* Atlas Search indexes
* Atlas Full-Text Search
* Vector search


---

## 5. Data Model (Business View)

### 5.1 Parcel Entity

A parcel represents a package waiting for customer pickup at a retail store.

**Core Attributes:**

| Field | Type | Description | Required |
|----|----|----|----|
| Parcel ID | String | Unique parcel identifier | Yes |
| Order ID | String | Associated customer order | Yes |
| Store ID | String | Pickup location code | Yes |
| Status | Enum | Current parcel status | Yes |
| Status Update Date | DateTime | When status last changed | Yes |
| Expiration Date | DateTime | When parcel expires | Yes |
| Is Expired | Boolean | Expired flag | Yes |
| Is Paid | Boolean | Payment status | Yes |

**Customer Information:**

| Field | Type | Description | Required |
|----|----|----|----|
| First Name | String | Customer first name | Yes |
| Last Name | String | Customer last name | Yes |
| Email | String | Contact email | No |
| Phone Number | String | Contact phone | No |

**Storage Location:**

| Field | Type | Description | Required |
|----|----|----|----|
| Store ID | String | Physical store code | Yes |
| Location Code | String | Storage rack/bin code | No |

### 5.2 Status Values

```
- READY_FOR_PICKUP
- IN_TRANSIT
- DELIVERED
- EXPIRED
- RETURNED
- CANCELLED
```

### 5.3 Sample Data Distribution

Your test dataset should reflect realistic distribution:

* 10 store locations (ST001 through ST010)
* \~5,000 parcels per store
* 70% READY_FOR_PICKUP, 15% IN_TRANSIT, 10% DELIVERED, 5% EXPIRED
* Diverse customer names (20 first names, 20 last names)
* Random but valid email addresses and phone numbers


---

## 6. API Contract

### 6.1 Search Endpoint

**HTTP Method:** `POST` **Path:** `/store_parcels/search` **Content-Type:** `application/json`

**Request Schema:**

```json
{
  "criterias": [
    {
      "field": "string (required)",
      "operator": "string (required)",
      "value": "any (required)"
    }
  ],
  "sorts": [
    {
      "index": "string (required)",
      "field": "string (required)",
      "order": "asc|desc (required)"
    }
  ],
  "pagination": {
    "page": "integer (required, min: 1)",
    "pageSize": "integer (required, min: 10, max: 100)"
  }
}
```

**Supported Fields:**

* `parcelId`
* `customerOrderId`
* `deliveryStoreId`
* `parcelStatus`
* `customer.firstName`
* `customer.lastName`
* `customer.email`
* `customer.phoneNumber`

**Supported Operators:**

* `EQ` - Equals
* `NE` - Not equals
* `IN` - In array
* `NIN` - Not in array
* `LE` - Less than or equal
* `GE` - Greater than or equal
* `like` - Pattern matching (case-insensitive)

**Response Schema (HAL+JSON):**

```json
{
  "_embedded": {
    "storeParcels": [
      {
        "parcelId": "string",
        "customerOrderId": "string",
        "deliveryStoreId": "string",
        "parcelStatus": "string",
        "isPaid": "boolean",
        "customer": {
          "firstName": "string",
          "lastName": "string",
          "email": "string",
          "phoneNumber": "string"
        },
        "expirationDate": "ISO-8601 datetime",
        "isExpired": "boolean"
      }
    ]
  },
  "page": {
    "number": "integer",
    "size": "integer",
    "totalPages": "integer",
    "totalElements": "integer"
  }
}
```

**Response Headers:**

```
Content-Type: application/json

X-Processing-Time: <milliseconds>
```

**Status Codes:**

* `200 OK` - Successful search
* `400 Bad Request` - Invalid request format
* `500 Internal Server Error` - Server error

### 6.2 Health Check Endpoint

**HTTP Method:** `GET` **Path:** `/store_parcels/health`

**Response:**

```json
{
  "status": "UP",
  "timestamp": 1705315800000
}
```

**Status Codes:**

* `200 OK` - Service is healthy
* `503 Service Unavailable` - Service is down


---

## 7. Search Scenarios

### 7.1 Example Searches

**Scenario 1: Find parcels at a specific store**

```json
{
  "criterias": [
    {
      "field": "deliveryStoreId",
      "operator": "EQ",
      "value": "ST001"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20
  }
}
```

**Expected Performance:** P95 <60ms


---

**Scenario 2: Find customer's parcel by name**

```json
{
  "criterias": [
    {
      "field": "deliveryStoreId",
      "operator": "EQ",
      "value": "ST001"
    },
    {
      "field": "customer.firstName",
      "operator": "EQ",
      "value": "John"
    },
    {
      "field": "customer.lastName",
      "operator": "EQ",
      "value": "Doe"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20
  }
}
```

**Expected Performance:** P95 <60ms


---

**Scenario 3: Search with sorting**

```json
{
  "criterias": [
    {
      "field": "deliveryStoreId",
      "operator": "EQ",
      "value": "ST001"
    },
    {
      "field": "parcelStatus",
      "operator": "EQ",
      "value": "READY_FOR_PICKUP"
    }
  ],
  "sorts": [
    {
      "index": "0",
      "field": "expirationDateTime",
      "order": "asc"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20
  }
}
```

**Expected Performance:** P95 <60ms


---

**Scenario 4: Pattern matching**

```json
{
  "criterias": [
    {
      "field": "deliveryStoreId",
      "operator": "EQ",
      "value": "ST001"
    },
    {
      "field": "customer.firstName",
      "operator": "like",
      "value": "joh"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20
  }
}
```

**Expected Performance:** P95 <60ms


---

## 8. Performance Validation Requirements

### 8.1 Test Data Generation

**Requirements:**

* Generate 50,000 sample parcels
* Distribute across 10 stores evenly
* Use realistic names, emails, phone numbers
* Random but valid dates
* Mix of statuses reflecting realistic distribution

**Data Generation Script:**

Create `scripts/generate-test-data.js`:

```javascript
// Generate 50,000 sample parcels distributed across stores

const stores = ['ST001', 'ST002', 'ST003', 'ST004', 'ST005', 'ST006', 'ST007', 'ST008', 'ST009', 'ST010'];
const statuses = ['READY_FOR_PICKUP', 'IN_TRANSIT', 'DELIVERED', 'EXPIRED'];
const firstNames = ['John', 'Jane', 'Michael', 'Sarah', 'David', 'Emma', 'Robert', 'Lisa', 'James', 'Maria',
                    'William', 'Anna', 'Richard', 'Sophie', 'Thomas', 'Emily', 'Charles', 'Olivia', 'Daniel', 'Isabella'];
const lastNames = ['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis', 'Rodriguez', 'Martinez',
                   'Hernandez', 'Lopez', 'Wilson', 'Anderson', 'Thomas', 'Taylor', 'Moore', 'Jackson', 'Martin', 'Lee'];

const parcels = [];

for (let i = 1; i <= 50000; i++) {
  const storeId = stores[Math.floor(Math.random() * stores.length)];
  const status = statuses[Math.floor(Math.random() * statuses.length)];
  const firstName = firstNames[Math.floor(Math.random() * firstNames.length)];
  const lastName = lastNames[Math.floor(Math.random() * lastNames.length)];

  const parcel = {
    parcelId: `P${String(i).padStart(6, '0')}`,
    customerOrderId: `ORD${String(Math.floor(Math.random() * 100000)).padStart(6, '0')}`,
    deliveryStoreId: storeId,
    parcelStatus: status,
    parcelStatusUpdateDate: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000),
    expirationDateTime: new Date(Date.now() + Math.random() * 60 * 24 * 60 * 60 * 1000),
    isExpired: Math.random() > 0.9,
    isPaid: Math.random() > 0.1,
    customer: {
      firstName: firstName,
      lastName: lastName,
      email: `${firstName.toLowerCase()}.${lastName.toLowerCase()}@example.com`,
      phoneNumber: `+33${Math.floor(Math.random() * 1000000000)}`
    },
    storeParcelStorageLocation: {
      storeId: storeId,
      storeStorageLocationCode: `RACK-${String.fromCharCode(65 + Math.floor(Math.random() * 5))}-${Math.floor(Math.random() * 50) + 1}`
    }
  };

  parcels.push(parcel);
}

print(`Generated ${parcels.length} parcels`);
db.parcels.insertMany(parcels);
print('Data inserted successfully');
```

**Import Data:**

```bash
# Create scripts directory

mkdir -p scripts

# Save the script to scripts/generate-test-data.js

# Run the script

docker exec -i mongodb mongosh store_parcels_db < scripts/generate-test-data.js

# Or if MongoDB is local:
mongosh store_parcels_db < scripts/generate-test-data.js
```

**Verify Data:**

```bash
# Check count

docker exec mongodb mongosh store_parcels_db --eval "db.parcels.countDocuments()"

# Sample query

docker exec mongodb mongosh store_parcels_db --eval "db.parcels.findOne()"
```

### 8.2 Load Testing with Gatling

**Your Task:** Create a Gatling performance test to validate the P95 <60ms requirement.

**Install Gatling (Standalone):**

Download Gatling from: <https://gatling.io/open-source/>

```bash
# Download Gatling

curl -L "https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/3.10.5/gatling-charts-highcharts-bundle-3.10.5-bundle.zip" -o gatling.zip

# Extract

unzip gatling.zip

cd gatling-charts-highcharts-bundle-3.10.5
```

**Gatling Test Requirements:**


1. **Create simulation file:** `user-files/simulations/storeparcel/ParcelSearchSimulation.scala`
2. **Test Scenarios to Implement:**
   * Search by store code only
   * Search by store code + customer first name
   * Search by store code + status + customer last name with sorting
3. **Load Pattern:**
   * Ramp up to 50 concurrent users over 30 seconds
   * Maintain load for 60 seconds
   * Distribute load across all 3 scenarios
4. HTTP status 200
   * Response contains expected JSON structure
   * Response contains pagination metadata
   * Response time per request

**Gatling Documentation:**

* Official docs: <https://gatling.io/docs/gatling/>
* HTTP requests: <https://gatling.io/docs/gatling/reference/current/http/>
* Scenarios: <https://gatling.io/docs/gatling/reference/current/core/scenario/>
* Assertions: <https://gatling.io/docs/gatling/reference/current/core/assertions/>

**Run Gatling Test:**

* Success rate must be ≥99%

```bash
# Navigate to Gatling directory

cd gatling-charts-highcharts-bundle-3.10.5

# Run simulation (Linux/Mac)
./bin/gatling.sh

# Or on Windows

bin\gatling.bat

# Select your simulation from the menu
```

* P95 response time must be <60ms

**Or run directly:**

```bash
./bin/gatling.sh -s storeparcel.ParcelSearchSimulation
```

**Gatling Report:**

After the test completes, open the HTML report:

```
gatling-charts-highcharts-bundle-3.10.5/results/parcelsearchsimulation-<timestamp>/index.html
```

**Report Metrics to Analyze:**

* **P50, P95, P99 Response Times** - Must meet <60ms target
* **Requests per Second** - Throughput capacity
* **Success Rate** - Should be ≥99%
* **Response Time Distribution** - Identify outliers
* **Response Time Over Time** - Check for degradation


---

## 9. Deployment Requirements

### 9.1 Docker Support

**Requirements:**

* Application must run in Docker container
* Docker Compose setup for local development
* MongoDB must run in separate Docker container
* Containers must communicate via Docker network

**Docker Compose Structure:**

```yaml

version: '3.8'
services:
  mongodb:
    image: mongo:6.0
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db

  storeparcel-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - MONGODB_URI=mongodb://mongodb:27017
      - MONGODB_DATABASE=store_parcels_db
    depends_on:
      - mongodb

volumes:
  mongodb_data:
```

**Start Command:**

```bash

docker-compose up -d
```

### 9.2 Configuration

**Application Configuration Structure:**

```yaml

spring:
  application:
    name: storeParcelsWS
  data:
    mongodb:
      uri: mongodb://localhost:27017
      database: store_parcels_db

server:
  port: 8080
```

**Configuration Profiles:**

* `local` - Development on local machine
* `docker` - Running in Docker container


---

## 10. Progress Checkpoints

### 10.1 Checkpoint System

Track your progress through these incremental milestones. Each checkpoint validates a critical step toward the final goal.


---

#### 🔲 Checkpoint 1: Application Runs Successfully

**Goal:** Spring Boot application starts and responds to requests

**Validation:**

```bash
# Start application

mvn spring-boot:run

# Test health endpoint

curl http://localhost:8080/store_parcels/health

# Expected: {"status":"UP","timestamp":...}
```

**Success Criteria:**

* ✅ Application starts without errors
* ✅ Health endpoint returns 200 OK
* ✅ MongoDB connection established
* ✅ Basic search endpoint accessible (can return empty results)

**Estimated Time:** 4-8 hours


---

#### 🔲 Checkpoint 2: 50,000 Records Loaded

**Goal:** Test dataset generated and imported successfully

**Validation:**

```bash
# Check document count

docker exec mongodb mongosh store_parcels_db --eval "db.parcels.countDocuments()"

# Expected: 50000

# Verify data distribution

docker exec mongodb mongosh store_parcels_db --eval "db.parcels.aggregate([
  { \$group: { _id: '\$deliveryStoreId', count: { \$sum: 1 } } },
  { \$sort: { _id: 1 } }
])"

# Expected: ~5000 parcels per store (ST001-ST010)
```

**Success Criteria:**

* ✅ Exactly 50,000 documents in database
* ✅ Data distributed across 10 stores
* ✅ All required fields populated
* ✅ Sample queries return valid results

**Estimated Time:** 1-2 hours


---

#### 🔲 Checkpoint 3: Gatling Test Runs

**Goal:** Performance test suite executes successfully

**Validation:**

```bash
# Run Gatling simulation

cd gatling-charts-highcharts-bundle-3.10.5
./bin/gatling.sh -s storeparcel.ParcelSearchSimulation

# Expected: Report generated in results/ directory
```

**Success Criteria:**

* ✅ Gatling simulation compiles without errors
* ✅ All 3 test scenarios execute
* ✅ 50 concurrent users achieved
* ✅ HTML report generated with metrics
* ✅ Success rate >95% (even if slow)

**Estimated Time:** 4-6 hours (includes learning Gatling)


---

#### 🔲 Checkpoint 4: Baseline Measured

**Goal:** Document initial performance before optimization

**Validation:**

* Review Gatling report from Checkpoint 3
* Record P50, P95, P99 metrics
* Identify slowest scenarios

**Success Criteria:**

* ✅ Baseline metrics documented in [PERFORMANCE.md](http://PERFORMANCE.md)
* ✅ Response times measured for all scenarios
* ✅ Bottlenecks identified (likely: no indexes, in-memory sorting)

**Baseline Performance Expected:**

| Metric | Typical Baseline (No Optimization) |
|----|----|
| P50 | 200-500ms |
| P95 | 800-2000ms |
| P99 | 1500-3000ms |

**Estimated Time:** 1 hour


---

#### 🔲 Checkpoint 5: P95 <200ms

**Goal:** First optimization pass - basic performance improvements

**Validation:**

```bash
# Run Gatling test again
./bin/gatling.sh -s storeparcel.ParcelSearchSimulation

# Check P95 in report
# Expected: P95 < 200ms
```

**Success Criteria:**

* ✅ P95 response time under 200ms
* ✅ At least one index created
* ✅ Query explain() shows index usage
* ✅ Performance improvement documented

**Likely Optimizations at This Stage:**

* Basic index on `deliveryStoreId`
* Connection pool configuration adjusted
* Database-level sorting implemented

**Estimated Time:** 4-6 hours


---

#### 🔲 Checkpoint 6: P95 <100ms

**Goal:** Second optimization pass - compound indexes and query optimization

**Validation:**

```bash
# Run Gatling test
./bin/gatling.sh -s storeparcel.ParcelSearchSimulation

# Expected: P95 < 100ms
```

**Success Criteria:**

* ✅ P95 response time under 100ms
* ✅ Compound indexes created for common queries
* ✅ All queries use covered indexes where possible
* ✅ Response times consistent across scenarios

**Likely Optimizations at This Stage:**

* Compound indexes: `(deliveryStoreId, parcelStatus, expirationDateTime)`
* Text index for customer name searches
* Query projection optimization (only fetch needed fields)
* JVM tuning for virtual threads

**Estimated Time:** 6-10 hours


---

#### 🔲 Checkpoint 7: P95 <60ms ✅ **TARGET ACHIEVED**

**Goal:** Final optimization - meet production SLA

**Validation:**

```bash
# Run Gatling test multiple times for consistency

for i in {1..3}; do
  ./bin/gatling.sh -s storeparcel.ParcelSearchSimulation
  sleep 10

done

# Expected: P95 < 60ms in all runs
```

**Success Criteria:**

* ✅ P95 response time under 60ms (primary goal)
* ✅ P99 response time under 100ms
* ✅ Success rate ≥99%
* ✅ Performance consistent across multiple test runs
* ✅ All search scenarios meet target

**Final Optimizations May Include:**

* Index intersection optimization
* Connection pool fine-tuning
* MongoDB read preference configuration
* Application-level caching for rarely-changing data
* Virtual thread optimization

**Estimated Time:** 8-12 hours


---

### 10.2 Checkpoint Progress Tracking

Use this table to track your progress:

| Checkpoint | Target | Status | Date Completed | P95 Time | Notes |
|----|----|----|----|----|----|
| 1. App Runs | ✅ Working | ⬜ |    | N/A |    |
| 2. Data Loaded | 50k records | ⬜ |    | N/A |    |
| 3. Gatling Runs | Test executes | ⬜ |    |    |    |
| 4. Baseline | Metrics recorded | ⬜ |    | \~1000ms |    |
| 5. First Pass | P95 <200ms | ⬜ |    |    |    |
| 6. Second Pass | P95 <100ms | ⬜ |    |    |    |
| 7. Final Goal | P95 <60ms | ⬜ |    |    |    |


---

### 10.3 Required Deliverables

Your project is complete when you deliver:

✅ **Working Application**

* Spring Boot application that starts successfully
* All endpoints functional
* Meets performance SLA (P95 <60ms)
* Passes all test scenarios

✅ **Database Schema & Indexes**

* MongoDB collection structure
* Index design documentation
* Rationale for index choices

✅ **Docker Setup**

* Dockerfile for application
* docker-compose.yml
* README with docker commands

✅ **Performance Testing**

* Gatling simulation (Scala code)
* Load test results showing P95 <60ms
* Performance test documentation

✅ **Performance Report**

* Checkpoint progress table (filled out)
* Baseline metrics (Checkpoint 4)
* Intermediate metrics (Checkpoints 5-6)
* Final metrics (Checkpoint 7)
* Performance analysis and optimization strategies

✅ **Design Documentation**

* Architecture decisions
* Database design rationale
* Performance optimization strategies
* Trade-offs and considerations

### 10.4 Code Quality Requirements

* Clean, readable code
* Proper error handling
* Comprehensive logging
* Follow Spring Boot best practices
* Use Java 21 features appropriately

### 10.5 Documentation Requirements

**[README.md](http://README.md) must include:**

* Project overview
* Setup instructions
* Running the application
* Running tests
* Docker deployment
* Performance benchmarks

**[DESIGN.md](http://DESIGN.md) must include:**

* Architecture overview
* Database schema design
* Index strategy and rationale
* Performance optimization techniques
* Query optimization approach
* Trade-offs and decisions

**[PERFORMANCE.md](http://PERFORMANCE.md) must include:**

* Checkpoint 4: Baseline measurements
* Checkpoints 5-7: Progressive optimization results
* Final comparison table (baseline vs optimized)
* Lessons learned


---

## 11. Evaluation Criteria

Your solution will be evaluated on:

### 11.1 Functional Correctness (25%)

* All search scenarios work correctly
* Pagination functions properly
* Sorting works as expected
* Error handling is robust

### 11.2 Performance (40%)

* **P95 response time <60ms** (primary metric)
* P99 response time <100ms
* Handles 50 concurrent users
* Sustained throughput ≥100 req/sec

### 11.3 Code Quality (20%)

* Clean, maintainable code
* Proper separation of concerns
* Effective use of Spring Boot features
* Appropriate use of Java 21 features
* Good error handling and logging

### 11.4 Design & Documentation (15%)

* Well-thought-out database schema
* Optimal index strategy
* Clear documentation of decisions
* Understanding of trade-offs
* Performance optimization rationale


---

## 12. Hints and Considerations

### 12.1 Performance Considerations

Think about:

* How does MongoDB execute your queries?
* Where should sorting happen (database vs. application)?
* What indexes would benefit your search patterns?
* How can you minimize data transfer?
* What's the optimal connection pool size?

### 12.2 Database Design Considerations

Think about:

* What fields are queried most frequently?
* Which queries would benefit from compound indexes?
* How does index order affect query performance?
* What's the trade-off between index size and query speed?
* How can you leverage MongoDB's native sorting capabilities?

### 12.3 Application Design Considerations

Think about:

* How should you structure the query builder?
* Where should validation occur?
* How can you leverage Spring Data MongoDB features?
* What role do virtual threads play in performance?
* How should you handle errors gracefully?

### 12.4 Testing Considerations

Think about:

* How do you measure baseline performance?
* What metrics matter most for your use case?
* How do you isolate performance bottlenecks?
* What tools can help identify slow operations?
* How do you validate improvements?


---

## 13. Resources

### 13.1 Official Documentation

* [Spring Boot 3.4.4 Reference](https://docs.spring.io/spring-boot/docs/3.4.4/reference/html/)
* [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
* [MongoDB Manual 6.0](https://www.mongodb.com/docs/v6.0/)
* [MongoDB Indexes](https://www.mongodb.com/docs/manual/indexes/)
* [MongoDB Query Performance](https://www.mongodb.com/docs/manual/tutorial/analyze-query-plan/)
* [Java 21 Documentation](https://openjdk.org/projects/jdk/21/)
* [Virtual Threads (JEP 444)](https://openjdk.org/jeps/444)

### 13.2 Tools

* **MongoDB Compass** - GUI for database inspection and query analysis
* **IntelliJ IDEA Profiler** - Java application profiling
* **Gatling** - Load testing and performance validation
* **Docker Desktop** - Container management
* **Postman/Insomnia** - API testing

### 13.3 Key Concepts to Research

* MongoDB compound indexes
* Index intersection vs. compound indexes
* Database-level sorting vs. in-memory sorting
* Connection pooling best practices
* Spring Data MongoDB query methods
* MongoDB aggregation pipeline
* Virtual threads and concurrency
* HAL+JSON response format


---

## 14. Glossary

| Term | Definition |
|----|----|
| **P50** | 50th percentile - median response time |
| **P95** | 95th percentile - 95% of requests complete within this time |
| **P99** | 99th percentile - 99% of requests complete within this time |
| **Compound Index** | MongoDB index on multiple fields |
| **Text Index** | MongoDB index for text search (not Atlas Search) |
| **Aggregation Pipeline** | MongoDB query framework for data processing |
| **Virtual Thread** | Lightweight thread in Java 21 (Project Loom) |
| **HAL+JSON** | Hypermedia Application Language - JSON response format |
| **SLA** | Service Level Agreement - performance guarantee |
| **Docker Compose** | Tool for defining multi-container Docker applications |


---

## Document Version

| Version | Date | Changes |
|----|----|----|
| 1.0.0 | 2025-01 | Initial PRD for build-from-scratch exercise |

**Document Purpose:** Training exercise - Build high-performance API with best practices **Target Audience:** Software engineers learning performance-aware development **Exercise Type:** Build from scratch (no existing code provided)


# Suite ! 

[training-part3-mcp-server.md 38386](attachments/cb42de36-d9a5-46e9-b21f-c23c6f72a14f.md)

[training-part4-gcp-deployment.md 57438](attachments/1ead3968-3141-4b2b-b9de-5bde9be5fba3.md)

[training-part2-write-endpoints.md 31370](attachments/577e80ea-2714-4fc6-a964-f9ee68fd9428.md)