package com.kantic.storeParcelsWS.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a sorting option.
 *
 * Example JSON:
 * {
 *   "index": "0",
 *   "field": "expirationDateTime",
 *   "order": "asc"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SortDTO {

    private String index;  // Priority order (for multiple sorts)

    @NotBlank(message = "Sort field is required")
    private String field;

    @NotBlank(message = "Sort order is required")
    private String order;  // "asc" or "desc"
}
