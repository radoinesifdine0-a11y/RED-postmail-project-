package com.kantic.storeParcelsWS.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Main search request DTO containing all search parameters.
 *
 * Example JSON:
 * {
 *   "criterias": [
 *     {"field": "deliveryStoreId", "operator": "EQ", "value": "ST001"},
 *     {"field": "parcelStatus", "operator": "EQ", "value": "READY_FOR_PICKUP"}
 *   ],
 *   "sorts": [
 *     {"index": "0", "field": "expirationDateTime", "order": "asc"}
 *   ],
 *   "pagination": {
 *     "page": 1,
 *     "pageSize": 20
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDTO {

    /**
     * List of search criteria (combined with AND logic)
     */
    @Valid
    private List<CriteriaDTO> criterias = new ArrayList<>();

    /**
     * List of sort options (applied in order of index)
     */
    @Valid
    private List<SortDTO> sorts = new ArrayList<>();

    /**
     * Pagination parameters
     */
    @Valid
    @NotNull(message = "Pagination is required")
    private PaginationDTO pagination = new PaginationDTO();
}
