# Store Parcels API

High-performance REST API for retail stores to search customer parcels.

## Prerequisites

Before running the application, install:

### 1. Java 21

**macOS (using Homebrew):**
```bash
brew install openjdk@21

# Add to your shell profile (~/.zshrc or ~/.bash_profile):
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"

# Reload profile
source ~/.zshrc
```

**Verify installation:**
```bash
java -version
# Should show: openjdk version "21.x.x"
```

### 2. Docker Desktop

**macOS:**
1. Download from: https://www.docker.com/products/docker-desktop/
2. Install and start Docker Desktop
3. Verify:
```bash
docker --version
```

---

## Quick Start

### Step 1: Start MongoDB

```bash
cd /Users/redouane/Desktop/homework/storeparcelapi

# Start MongoDB container
docker-compose up -d

# Verify it's running
docker ps
```

### Step 2: Run the Application

```bash
# Run with Maven wrapper
./mvnw spring-boot:run

# OR if you have Maven installed globally:
mvn spring-boot:run
```

### Step 3: Test the Endpoints

```bash
# Health check
curl http://localhost:8080/store_parcels/health

# Search parcels (empty result initially)
curl -X POST http://localhost:8080/store_parcels/search \
  -H "Content-Type: application/json" \
  -d '{
    "criterias": [],
    "pagination": {"page": 1, "pageSize": 20}
  }'
```

---

## Project Structure

```
storeparcelapi/
├── src/main/java/com/kantic/storeParcelsWS/
│   ├── StoreParcelsApplication.java   # Entry point
│   ├── controller/                    # REST endpoints
│   │   └── ParcelController.java
│   ├── service/                       # Business logic
│   │   ├── ParcelService.java
│   │   └── ParcelServiceImpl.java
│   ├── repository/                    # Database operations
│   │   ├── ParcelRepository.java
│   │   ├── ParcelRepositoryCustom.java
│   │   └── ParcelRepositoryCustomImpl.java
│   ├── model/                         # Entity classes
│   │   ├── Parcel.java
│   │   ├── Customer.java
│   │   ├── StorageLocation.java
│   │   └── ParcelStatus.java
│   └── dto/                           # Request/Response objects
│       ├── SearchRequestDTO.java
│       ├── CriteriaDTO.java
│       ├── SortDTO.java
│       ├── PaginationDTO.java
│       ├── ParcelResponseDTO.java
│       └── CustomerResponseDTO.java
├── src/main/resources/
│   └── application.yml                # Configuration
├── docs/                              # Documentation
│   ├── DEVELOPMENT_REPORT_EN.md
│   └── RAPPORT_DEVELOPPEMENT_FR.md
├── pom.xml                            # Maven dependencies
├── docker-compose.yml                 # Docker configuration
├── Dockerfile                         # Container build
└── README.md                          # This file
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/store_parcels/health` | Health check |
| POST | `/store_parcels/search` | Search parcels |
| GET | `/store_parcels/parcel/{id}` | Get single parcel |

---

## Next Steps (Part 1 Checkpoints)

1. [ ] **Checkpoint 1**: Application runs and health check works
2. [ ] **Checkpoint 2**: Generate 50,000 test parcels
3. [ ] **Checkpoint 3**: Gatling tests run
4. [ ] **Checkpoint 4**: Measure baseline performance
5. [ ] **Checkpoint 5**: P95 < 200ms
6. [ ] **Checkpoint 6**: P95 < 100ms
7. [ ] **Checkpoint 7**: P95 < 60ms (TARGET!)

---

## Useful Commands

```bash
# Start MongoDB
docker-compose up -d

# Stop MongoDB
docker-compose down

# View MongoDB logs
docker logs mongodb

# Connect to MongoDB shell
docker exec -it mongodb mongosh store_parcels_db

# Build the application
./mvnw clean package

# Run tests
./mvnw test
```
