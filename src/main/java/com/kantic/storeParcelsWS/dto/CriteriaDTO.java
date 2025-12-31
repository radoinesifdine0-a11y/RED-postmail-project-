package com.kantic.storeParcelsWS.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single search criterion.
 *
 * Example JSON:
 * {
 *   "field": "deliveryStoreId",
 *   "operator": "EQ",
 *   "value": "ST001"
 * }
 *
 * Supported operators:
 * - EQ: Equals
 * - NE: Not equals
 * - IN: In array
 * - NIN: Not in array
 * - LE: Less than or equal
 * - GE: Greater than or equal
 * - LIKE: Pattern matching (case-insensitive)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaDTO {

    @NotBlank(message = "Field name is required")
    private String field;

    @NotBlank(message = "Operator is required")
    private String operator;

    @NotNull(message = "Value is required")
    private Object value;
}
