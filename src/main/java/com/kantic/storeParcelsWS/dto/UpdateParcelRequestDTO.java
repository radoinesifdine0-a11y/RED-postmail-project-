package com.kantic.storeParcelsWS.dto;

import jakarta.validation.Valid;
import java.time.Instant;

/**
 * DTO for updating a parcel.
 * All fields are optional - only non-null fields will be updated.
 *
 * Non-modifiable fields (not included):
 * - parcelId
 * - customerOrderId (orderId)
 * - deliveryStoreId (pickupStoreId)
 * - customer.firstName
 * - customer.lastName
 */
public record UpdateParcelRequestDTO(
    String parcelStatus,                        // Optional, enum value as string
    Boolean isPaid,                             // Optional
    Instant expirationDate,                     // Optional
    @Valid CustomerUpdateDTO customer,          // Optional
    @Valid StorageLocationUpdateDTO storageLocation  // Optional
) {
    /**
     * DTO for updating customer contact information.
     * Only email and phone can be updated (not name).
     */
    public record CustomerUpdateDTO(
        String email,                           // Optional
        String phoneNumber                      // Optional
    ) {}

    /**
     * DTO for updating storage location.
     */
    public record StorageLocationUpdateDTO(
        String locationCode                     // Maps to storeStorageLocationCode
    ) {}
}
