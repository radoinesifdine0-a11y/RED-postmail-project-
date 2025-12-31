package com.kantic.storeParcelsWS.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Main entity representing a parcel in the store.
 *
 * @Document - Maps this class to MongoDB collection "parcels"
 * @CompoundIndexes - Defines indexes for fast queries (CRITICAL for P95 < 60ms!)
 *
 * MongoDB document structure:
 * {
 *   "_id": ObjectId("..."),
 *   "parcelId": "P000001",
 *   "customerOrderId": "ORD123456",
 *   "deliveryStoreId": "ST001",
 *   "parcelStatus": "READY_FOR_PICKUP",
 *   "parcelStatusUpdateDate": ISODate("2025-01-15T10:30:00Z"),
 *   "expirationDateTime": ISODate("2025-02-15T10:30:00Z"),
 *   "isExpired": false,
 *   "isPaid": true,
 *   "customer": { ... },
 *   "storeParcelStorageLocation": { ... }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "parcels")
@CompoundIndexes({
    // Index for searching by store + status + expiration (most common query)
    @CompoundIndex(
        name = "store_status_expiration_idx",
        def = "{'deliveryStoreId': 1, 'parcelStatus': 1, 'expirationDateTime': 1}"
    ),
    // Index for searching by customer name
    @CompoundIndex(
        name = "customer_name_idx",
        def = "{'customer.firstName': 1, 'customer.lastName': 1}"
    ),
    // Index for searching by store + customer name
    @CompoundIndex(
        name = "store_customer_idx",
        def = "{'deliveryStoreId': 1, 'customer.firstName': 1, 'customer.lastName': 1}"
    )
})
public class Parcel {

    /**
     * MongoDB internal ID (auto-generated ObjectId)
     * Different from parcelId which is our business identifier
     */
    @Id
    private String id;

    /**
     * Unique business identifier for the parcel (e.g., "P000001")
     * @Indexed(unique = true) - Creates a unique index, prevents duplicates
     */
    @Indexed(unique = true)
    private String parcelId;

    /**
     * Customer's order ID from the e-commerce system
     * @Field - Maps Java field name to MongoDB field name
     */
    @Field("customerOrderId")
    private String orderId;

    /**
     * Store where the parcel should be picked up (e.g., "ST001")
     */
    @Field("deliveryStoreId")
    private String pickupStoreId;

    /**
     * Current status of the parcel
     */
    @Field("parcelStatus")
    private ParcelStatus status;

    /**
     * When the status was last changed
     */
    @Field("parcelStatusUpdateDate")
    private Instant statusUpdateDate;

    /**
     * When the parcel expires (must be picked up before this date)
     */
    @Field("expirationDateTime")
    private Instant expirationDate;

    /**
     * Whether the parcel has expired
     */
    private Boolean isExpired;

    /**
     * Whether the parcel has been paid for
     */
    private Boolean isPaid;

    /**
     * Customer information (embedded document)
     */
    private Customer customer;

    /**
     * Storage location in the store (embedded document)
     */
    @Field("storeParcelStorageLocation")
    private StorageLocation storageLocation;
}
