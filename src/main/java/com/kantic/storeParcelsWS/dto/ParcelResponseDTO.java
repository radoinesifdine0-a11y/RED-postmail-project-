package com.kantic.storeParcelsWS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Parcel information returned in API responses.
 *
 * This is different from the Parcel entity:
 * - Entity: Has all fields including internal ones
 * - DTO: Only fields we want to expose to the API consumer
 *
 * Using DTOs gives us control over what data leaves our system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelResponseDTO {

    private String parcelId;
    private String customerOrderId;
    private String deliveryStoreId;
    private String parcelStatus;
    private Boolean isPaid;
    private CustomerResponseDTO customer;
    private Instant expirationDate;
    private Boolean isExpired;
}
