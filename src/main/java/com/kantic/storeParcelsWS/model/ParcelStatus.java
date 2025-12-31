package com.kantic.storeParcelsWS.model;

/**
 * Enum representing all possible parcel statuses.
 *
 * The parcel lifecycle typically follows:
 * PENDING → IN_TRANSIT → READY_FOR_PICKUP → PICKED_UP → DELIVERED
 *
 * Alternative paths:
 * - Any status can transition to CANCELLED
 * - READY_FOR_PICKUP can become EXPIRED if not picked up in time
 * - EXPIRED parcels can be RETURNED to sender
 */
public enum ParcelStatus {

    PENDING,           // Order created, not yet shipped
    IN_TRANSIT,        // Being shipped to the store
    READY_FOR_PICKUP,  // At the store, waiting for customer
    PICKED_UP,         // Customer has collected it
    DELIVERED,         // Delivery confirmed (terminal state)
    EXPIRED,           // Pickup deadline passed
    RETURNED,          // Sent back to sender (terminal state)
    CANCELLED          // Order cancelled (terminal state)
}
