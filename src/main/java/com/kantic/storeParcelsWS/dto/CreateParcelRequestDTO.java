package com.kantic.storeParcelsWS.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for creating a new parcel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateParcelRequestDTO {

    @NotBlank(message = "Parcel ID is required")
    private String parcelId;

    @NotBlank(message = "Customer order ID is required")
    private String customerOrderId;

    @NotBlank(message = "Delivery store ID is required")
    @Pattern(regexp = "^ST\\d{3}$", message = "Store ID must match format ST001-ST999")
    private String deliveryStoreId;

    @NotNull(message = "Customer information is required")
    @Valid
    private CustomerCreateDTO customer;

    private Boolean isPaid;

    private Instant expirationDate;

    /**
     * Nested DTO for customer creation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerCreateDTO {

        @NotBlank(message = "Customer first name is required")
        private String firstName;

        @NotBlank(message = "Customer last name is required")
        private String lastName;

        private String email;

        private String phoneNumber;
    }
}
