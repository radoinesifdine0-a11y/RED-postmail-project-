package com.kantic.storeParcelsWS.controller;

import com.kantic.storeParcelsWS.dto.ParcelResponseDTO;
import com.kantic.storeParcelsWS.dto.SearchRequestDTO;
import com.kantic.storeParcelsWS.dto.UpdateParcelRequestDTO;
import com.kantic.storeParcelsWS.service.ParcelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for parcel operations.
 *
 * Endpoints:
 * - GET  /store_parcels/health          → Health check
 * - POST /store_parcels/search          → Search parcels
 * - GET  /store_parcels/parcel/{id}     → Get single parcel
 *
 * @RestController = @Controller + @ResponseBody
 * - All methods return JSON by default
 *
 * @RequestMapping("/store_parcels") - Base path for all endpoints
 */
@Slf4j
@RestController
@RequestMapping("/store_parcels")
@RequiredArgsConstructor
public class ParcelController {

    private final ParcelService parcelService;

    // Note: Health endpoints moved to HealthController
    // - GET /health       → Basic health check
    // - GET /health/live  → Liveness probe
    // - GET /health/ready → Readiness probe (MongoDB check)

    /**
     * Search parcels with criteria, sorting, and pagination.
     *
     * POST /store_parcels/search
     *
     * Why POST instead of GET?
     * - Search criteria can be complex (multiple filters)
     * - GET has URL length limits
     * - POST body is cleaner for complex queries
     *
     * @Valid - Validates the request body using annotations in DTO
     * @RequestBody - Parses JSON body into SearchRequestDTO
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchParcels(
            @Valid @RequestBody SearchRequestDTO request) {

        long startTime = System.currentTimeMillis();

        // Execute search
        Page<ParcelResponseDTO> results = parcelService.searchParcels(request);

        long processingTime = System.currentTimeMillis() - startTime;

        // Build HAL+JSON style response
        Map<String, Object> response = new HashMap<>();

        // Embedded resources
        Map<String, Object> embedded = new HashMap<>();
        embedded.put("storeParcels", results.getContent());
        response.put("_embedded", embedded);

        // Pagination metadata
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("number", results.getNumber() + 1);  // Convert to 1-indexed
        pageInfo.put("size", results.getSize());
        pageInfo.put("totalPages", results.getTotalPages());
        pageInfo.put("totalElements", results.getTotalElements());
        response.put("page", pageInfo);

        log.info("Search completed in {}ms, returned {} results",
                 processingTime, results.getContent().size());

        return ResponseEntity.ok()
                .header("X-Processing-Time", String.valueOf(processingTime))
                .body(response);
    }

    /**
     * Get a single parcel by ID.
     *
     * GET /store_parcels/parcel/{parcelId}
     *
     * @PathVariable - Extracts {parcelId} from URL
     */
    @GetMapping("/parcel/{parcelId}")
    public ResponseEntity<ParcelResponseDTO> getParcel(
            @PathVariable String parcelId) {

        return parcelService.getParcelById(parcelId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Browser-friendly GET endpoint for testing.
     *
     * GET /store_parcels/browse?store=ST001&status=READY_FOR_PICKUP&page=1&size=10
     *
     * All parameters are optional:
     * - store: Filter by store ID (e.g., ST001)
     * - status: Filter by status (e.g., READY_FOR_PICKUP)
     * - page: Page number (default 1)
     * - size: Results per page (default 10, min 10, max 100)
     *
     * Examples:
     * - /store_parcels/browse                     → All parcels (first 10)
     * - /store_parcels/browse?store=ST001         → Parcels in store ST001
     * - /store_parcels/browse?status=PENDING      → Pending parcels
     * - /store_parcels/browse?store=ST001&page=2  → Page 2 of ST001
     */
    @GetMapping("/browse")
    public ResponseEntity<Map<String, Object>> browseParcels(
            @RequestParam(required = false) String store,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Ensure valid pagination
        if (size < 10) size = 10;
        if (size > 100) size = 100;
        if (page < 1) page = 1;

        // Build search request
        SearchRequestDTO request = new SearchRequestDTO();
        request.setPagination(new com.kantic.storeParcelsWS.dto.PaginationDTO(page, size));

        // Add criteria if provided
        java.util.List<com.kantic.storeParcelsWS.dto.CriteriaDTO> criterias = new java.util.ArrayList<>();

        if (store != null && !store.isEmpty()) {
            criterias.add(new com.kantic.storeParcelsWS.dto.CriteriaDTO("deliveryStoreId", "EQ", store));
        }
        if (status != null && !status.isEmpty()) {
            criterias.add(new com.kantic.storeParcelsWS.dto.CriteriaDTO("parcelStatus", "EQ", status));
        }
        request.setCriterias(criterias);

        // Execute search
        long startTime = System.currentTimeMillis();
        Page<ParcelResponseDTO> results = parcelService.searchParcels(request);
        long processingTime = System.currentTimeMillis() - startTime;

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("parcels", results.getContent());
        response.put("page", Map.of(
            "current", results.getNumber() + 1,
            "size", results.getSize(),
            "totalPages", results.getTotalPages(),
            "totalElements", results.getTotalElements()
        ));
        response.put("processingTimeMs", processingTime);
        response.put("filters", Map.of(
            "store", store != null ? store : "all",
            "status", status != null ? status : "all"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing parcel.
     *
     * PUT /store_parcels/parcel/{parcelId}
     *
     * Updates parcel fields based on the request body.
     * Status transitions are validated against the state machine.
     *
     * @param parcelId The parcel ID to update
     * @param request The update request with fields to modify
     * @return 200 OK with updated parcel, 400 if invalid transition, 404 if not found
     */
    @PutMapping("/parcel/{parcelId}")
    public ResponseEntity<ParcelResponseDTO> updateParcel(
            @PathVariable String parcelId,
            @Valid @RequestBody UpdateParcelRequestDTO request) {

        log.info("PUT /store_parcels/parcel/{} - Update request received", parcelId);

        ParcelResponseDTO updated = parcelService.updateParcel(parcelId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Soft delete a parcel.
     *
     * DELETE /store_parcels/parcel/{parcelId}
     *
     * Only parcels with CANCELLED or RETURNED status can be deleted.
     * Deleted parcels are excluded from search results.
     *
     * @param parcelId The parcel ID to delete
     * @return 204 No Content on success, 400 if not deletable, 404 if not found
     */
    @DeleteMapping("/parcel/{parcelId}")
    public ResponseEntity<Void> deleteParcel(@PathVariable String parcelId) {

        log.info("DELETE /store_parcels/parcel/{} - Delete request received", parcelId);

        parcelService.deleteParcel(parcelId);
        return ResponseEntity.noContent().build();
    }
}
