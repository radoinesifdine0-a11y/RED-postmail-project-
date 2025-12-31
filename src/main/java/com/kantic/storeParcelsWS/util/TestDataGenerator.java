package com.kantic.storeParcelsWS.util;

import com.kantic.storeParcelsWS.model.Customer;
import com.kantic.storeParcelsWS.model.Parcel;
import com.kantic.storeParcelsWS.model.ParcelStatus;
import com.kantic.storeParcelsWS.model.StorageLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates test data for performance testing.
 *
 * WHY 50,000 PARCELS?
 * - Realistic dataset size for a retail store chain
 * - Large enough to test index effectiveness
 * - Small enough to fit in local MongoDB
 *
 * HOW IT WORKS:
 * 1. Generates parcels in batches of 1000 (faster than one-by-one)
 * 2. Uses random data from predefined arrays
 * 3. Creates indexes after data insertion
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataGenerator {

    private final MongoTemplate mongoTemplate;

    // Random data pools
    private static final String[] STORES = {
        "ST001", "ST002", "ST003", "ST004", "ST005",
        "ST006", "ST007", "ST008", "ST009", "ST010"
    };

    private static final ParcelStatus[] STATUSES = ParcelStatus.values();

    private static final String[] FIRST_NAMES = {
        "John", "Jane", "Michael", "Sarah", "David",
        "Emma", "Robert", "Lisa", "James", "Maria",
        "William", "Anna", "Richard", "Sophie", "Thomas",
        "Emily", "Charles", "Olivia", "Daniel", "Isabella",
        "Matthew", "Mia", "Anthony", "Charlotte", "Mark"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones",
        "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
        "Hernandez", "Lopez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee",
        "Perez", "Thompson", "White", "Harris", "Sanchez"
    };

    // Configuration
    private static final int PARCEL_COUNT = 50_000;  // Target: 50,000 parcels
    private static final int BATCH_SIZE = 1000;      // Insert 1000 at a time

    private final Random random = new Random();

    /**
     * Generate all test data.
     *
     * @return Number of parcels generated
     */
    public int generateTestData() {
        log.info("=== Starting test data generation: {} parcels ===", PARCEL_COUNT);
        long startTime = System.currentTimeMillis();

        List<Parcel> batch = new ArrayList<>(BATCH_SIZE);
        int totalInserted = 0;

        for (int i = 1; i <= PARCEL_COUNT; i++) {
            Parcel parcel = createRandomParcel(i);
            batch.add(parcel);

            // Insert batch when full
            if (batch.size() >= BATCH_SIZE) {
                mongoTemplate.insertAll(batch);
                totalInserted += batch.size();

                // Progress logging every 10,000
                if (totalInserted % 10000 == 0) {
                    log.info("Progress: {}/{} parcels inserted", totalInserted, PARCEL_COUNT);
                }
                batch.clear();
            }
        }

        // Insert remaining parcels
        if (!batch.isEmpty()) {
            mongoTemplate.insertAll(batch);
            totalInserted += batch.size();
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("=== Data generation complete: {} parcels in {}ms ===", totalInserted, duration);

        // Create indexes for fast queries
        createIndexes();

        return totalInserted;
    }

    /**
     * Create a random parcel with realistic data.
     */
    private Parcel createRandomParcel(int index) {
        String storeId = randomElement(STORES);
        ParcelStatus status = randomElement(STATUSES);
        String firstName = randomElement(FIRST_NAMES);
        String lastName = randomElement(LAST_NAMES);

        // Random dates
        Instant now = Instant.now();
        Instant statusUpdateDate = now.minus(random.nextInt(30), ChronoUnit.DAYS);
        Instant expirationDate = now.plus(random.nextInt(60), ChronoUnit.DAYS);

        // Build customer
        Customer customer = Customer.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com")
                .phoneNumber("+33" + (100000000 + random.nextInt(900000000)))
                .build();

        // Build storage location
        StorageLocation location = StorageLocation.builder()
                .storeId(storeId)
                .locationCode("RACK-" + (char)('A' + random.nextInt(5)) + "-" + (1 + random.nextInt(50)))
                .build();

        // Build parcel
        return Parcel.builder()
                .parcelId(String.format("P%06d", index))
                .orderId(String.format("ORD%06d", random.nextInt(100000)))
                .pickupStoreId(storeId)
                .status(status)
                .statusUpdateDate(statusUpdateDate)
                .expirationDate(expirationDate)
                .isExpired(random.nextDouble() > 0.9)  // 10% expired
                .isPaid(random.nextDouble() > 0.1)     // 90% paid
                .customer(customer)
                .storageLocation(location)
                .build();
    }

    /**
     * Create indexes for fast queries.
     *
     * CRITICAL FOR P95 < 60ms!
     * Without indexes, MongoDB scans ALL documents for every query.
     */
    private void createIndexes() {
        log.info("Creating indexes...");
        long startTime = System.currentTimeMillis();

        // Index 1: Unique parcel ID
        mongoTemplate.indexOps(Parcel.class).ensureIndex(
            new Index().on("parcelId", Sort.Direction.ASC).unique()
        );

        // Index 2: Store + Status + Expiration (most common search)
        mongoTemplate.indexOps(Parcel.class).ensureIndex(
            new Index()
                .on("pickupStoreId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("expirationDate", Sort.Direction.ASC)
                .named("store_status_expiration_idx")
        );

        // Index 3: Customer name search
        mongoTemplate.indexOps(Parcel.class).ensureIndex(
            new Index()
                .on("customer.firstName", Sort.Direction.ASC)
                .on("customer.lastName", Sort.Direction.ASC)
                .named("customer_name_idx")
        );

        // Index 4: Order ID lookup
        mongoTemplate.indexOps(Parcel.class).ensureIndex(
            new Index().on("orderId", Sort.Direction.ASC)
        );

        long duration = System.currentTimeMillis() - startTime;
        log.info("Indexes created in {}ms", duration);
    }

    /**
     * Helper: Get random element from array.
     */
    private <T> T randomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }
}
