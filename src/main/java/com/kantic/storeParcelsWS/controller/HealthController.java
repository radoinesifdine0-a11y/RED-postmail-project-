package com.kantic.storeParcelsWS.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health Controller for Cloud Run probes.
 *
 * Provides three health endpoints:
 * - /health       → Basic health check (quick)
 * - /health/live  → Liveness probe (is the app alive?)
 * - /health/ready → Readiness probe (is MongoDB connected?)
 *
 * Cloud Run uses these probes to:
 * - Determine if the container is ready to receive traffic
 * - Restart the container if it becomes unhealthy
 */
@Slf4j
@RestController
@RequestMapping("/store_parcels")
@RequiredArgsConstructor
public class HealthController {

    private final MongoTemplate mongoTemplate;

    /**
     * Basic health check endpoint.
     *
     * GET /store_parcels/health
     *
     * Used by:
     * - Docker health checks
     * - Load balancers
     * - Monitoring systems
     *
     * @return 200 OK with status and timestamp
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Liveness probe - is the application alive?
     *
     * GET /store_parcels/health/live
     *
     * Used by Cloud Run to determine if the container should be restarted.
     * This should be a quick check that doesn't depend on external services.
     *
     * @return 200 OK if the app is running
     */
    @GetMapping("/health/live")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /**
     * Readiness probe - is the application ready to receive traffic?
     *
     * GET /store_parcels/health/ready
     *
     * Used by Cloud Run to determine if the container can receive requests.
     * Checks MongoDB connectivity to ensure the app can actually serve requests.
     *
     * @return 200 OK if MongoDB is connected, 503 Service Unavailable otherwise
     */
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        try {
            // Ping MongoDB to verify connection
            mongoTemplate.getDb().runCommand(new Document("ping", 1));

            log.debug("Readiness check passed - MongoDB connected");

            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "mongodb", "connected",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Readiness check failed - MongoDB connection error: {}", e.getMessage());

            return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "mongodb", "disconnected",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
