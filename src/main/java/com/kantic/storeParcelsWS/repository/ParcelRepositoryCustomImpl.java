package com.kantic.storeParcelsWS.repository;

import com.kantic.storeParcelsWS.dto.CriteriaDTO;
import com.kantic.storeParcelsWS.dto.SearchRequestDTO;
import com.kantic.storeParcelsWS.dto.SortDTO;
import com.kantic.storeParcelsWS.model.Parcel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Custom repository implementation for complex search queries.
 *
 * THIS IS WHERE PERFORMANCE MATTERS!
 * - We build MongoDB queries dynamically based on criteria
 * - We use database-level sorting (not in-memory)
 * - We use pagination to limit data transfer
 *
 * The class name MUST be: {RepositoryName}Impl
 * Spring Data automatically finds and uses this implementation.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ParcelRepositoryCustomImpl implements ParcelRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    /**
     * Field name mapping: API field names → MongoDB field names
     *
     * Why? The API uses clean names (deliveryStoreId),
     * but MongoDB might store them differently.
     */
    private static final Map<String, String> FIELD_MAPPING = Map.of(
        "parcelId", "parcelId",
        "customerOrderId", "orderId",
        "deliveryStoreId", "pickupStoreId",
        "parcelStatus", "status",
        "customer.firstName", "customer.firstName",
        "customer.lastName", "customer.lastName",
        "customer.email", "customer.email",
        "customer.phoneNumber", "customer.phoneNumber",
        "expirationDateTime", "expirationDate"
    );

    @Override
    public Page<Parcel> searchParcels(SearchRequestDTO request) {
        long startTime = System.currentTimeMillis();

        // Build the MongoDB query
        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = new ArrayList<>();

        // Add each search criterion
        for (CriteriaDTO criterion : request.getCriterias()) {
            Criteria fieldCriteria = buildCriteria(criterion);
            if (fieldCriteria != null) {
                criteriaList.add(fieldCriteria);
            }
        }

        // Combine criteria with AND logic
        if (!criteriaList.isEmpty()) {
            criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
            query.addCriteria(criteria);
        }

        // Add sorting
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            Sort sort = buildSort(request.getSorts());
            query.with(sort);
        }

        // Count total matching documents (for pagination info)
        long totalCount = mongoTemplate.count(query, Parcel.class);

        // Apply pagination
        int page = request.getPagination().getPage() - 1; // Convert to 0-indexed
        int pageSize = request.getPagination().getPageSize();
        query.skip((long) page * pageSize);
        query.limit(pageSize);

        // Execute query
        List<Parcel> parcels = mongoTemplate.find(query, Parcel.class);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Search query executed in {}ms, found {} results (total: {})",
                  duration, parcels.size(), totalCount);

        return new PageImpl<>(parcels, PageRequest.of(page, pageSize), totalCount);
    }

    /**
     * Build a MongoDB Criteria from a search criterion DTO.
     *
     * Supported operators:
     * - EQ: Exact match
     * - NE: Not equals
     * - IN: Value in array
     * - NIN: Value not in array
     * - LE: Less than or equal
     * - GE: Greater than or equal
     * - LIKE: Case-insensitive pattern match
     */
    private Criteria buildCriteria(CriteriaDTO criterion) {
        String field = mapFieldName(criterion.getField());
        String operator = criterion.getOperator().toUpperCase();
        Object value = criterion.getValue();

        return switch (operator) {
            case "EQ" -> Criteria.where(field).is(value);
            case "NE" -> Criteria.where(field).ne(value);
            case "IN" -> Criteria.where(field).in((List<?>) value);
            case "NIN" -> Criteria.where(field).nin((List<?>) value);
            case "LE" -> Criteria.where(field).lte(value);
            case "GE" -> Criteria.where(field).gte(value);
            case "LIKE" -> {
                // Case-insensitive regex pattern matching
                String pattern = Pattern.quote(value.toString());
                yield Criteria.where(field).regex(pattern, "i");
            }
            default -> {
                log.warn("Unknown operator: {}", operator);
                yield null;
            }
        };
    }

    /**
     * Build Sort object from list of sort DTOs.
     */
    private Sort buildSort(List<SortDTO> sorts) {
        List<Sort.Order> orders = new ArrayList<>();

        for (SortDTO sortDTO : sorts) {
            String field = mapFieldName(sortDTO.getField());
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDTO.getOrder())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, field));
        }

        return Sort.by(orders);
    }

    /**
     * Map API field name to MongoDB field name.
     */
    private String mapFieldName(String apiFieldName) {
        return FIELD_MAPPING.getOrDefault(apiFieldName, apiFieldName);
    }
}
