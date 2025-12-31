package com.kantic.storeParcelsWS.controller;

import com.kantic.storeParcelsWS.model.Parcel;
import com.kantic.storeParcelsWS.util.TestDataGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoints for development and testing.
 *
 * @Profile({"local", "docker"}) - ONLY available in dev environments!
 * This prevents accidental data generation/deletion in production.
 *
 * Endpoints:
 * - POST /store_parcels/admin/generate-test-data  → Generate 50,000 parcels
 * - DELETE /store_parcels/admin/clear-data        → Delete all parcels
 * - GET /store_parcels/admin/count                → Count parcels
 */
@Slf4j
@RestController
@RequestMapping("/store_parcels/admin")
@RequiredArgsConstructor
@Profile({"local", "docker"})  // NOT available in GCP/production!
public class AdminController {

    private final TestDataGenerator testDataGenerator;
    private final MongoTemplate mongoTemplate;

    /**
     * Generate test data (50,000 parcels).
     *
     * POST /store_parcels/admin/generate-test-data
     *
     * This will:
     * 1. Clear existing data
     * 2. Generate 50,000 random parcels
     * 3. Create indexes
     *
     * Takes about 5-10 seconds.
     */
    @PostMapping("/generate-test-data")
    public ResponseEntity<Map<String, Object>> generateTestData() {
        log.info("=== Starting test data generation ===");
        long startTime = System.currentTimeMillis();

        // Clear existing data first
        mongoTemplate.dropCollection(Parcel.class);
        log.info("Existing data cleared");

        // Generate new data
        int count = testDataGenerator.generateTestData();

        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "parcelsGenerated", count,
            "durationMs", duration,
            "message", String.format("Generated %d parcels in %dms", count, duration)
        ));
    }

    /**
     * Clear all parcel data.
     *
     * DELETE /store_parcels/admin/clear-data
     *
     * Use with caution! Deletes everything.
     */
    @DeleteMapping("/clear-data")
    public ResponseEntity<Map<String, String>> clearData() {
        log.info("Clearing all parcel data");

        long countBefore = mongoTemplate.count(new Query(), Parcel.class);
        mongoTemplate.dropCollection(Parcel.class);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "deletedCount", String.valueOf(countBefore),
            "message", String.format("Deleted %d parcels", countBefore)
        ));
    }

    /**
     * Get current parcel count.
     *
     * GET /store_parcels/admin/count
     *
     * Useful to verify data generation worked.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCount() {
        long count = mongoTemplate.count(new Query(), Parcel.class);

        return ResponseEntity.ok(Map.of(
            "collection", "parcels",
            "count", count
        ));
    }
}
