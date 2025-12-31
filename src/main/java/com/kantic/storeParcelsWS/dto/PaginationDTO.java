package com.kantic.storeParcelsWS.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pagination parameters for search requests.
 *
 * Example JSON:
 * {
 *   "page": 1,
 *   "pageSize": 20
 * }
 *
 * Note: page is 1-indexed (first page = 1)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDTO {

    @Min(value = 1, message = "Page must be at least 1")
    private int page = 1;

    @Min(value = 10, message = "Page size minimum is 10")
    @Max(value = 100, message = "Page size maximum is 100")
    private int pageSize = 20;
}
