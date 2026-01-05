package com.kantic.storeParcelsWS.model;

import java.util.Map;
import java.util.Set;

/**
 * Enum representing all possible parcel statuses with state machine logic.
 *
 * The parcel lifecycle typically follows:
 * PENDING → IN_TRANSIT → READY_FOR_PICKUP → PICKED_UP → DELIVERED
 *
 * Alternative paths:
 * - Most statuses can transition to CANCELLED
 * - READY_FOR_PICKUP can become EXPIRED if not picked up in time
 * - EXPIRED parcels can be RETURNED to sender
 *
 * Terminal states: DELIVERED, RETURNED, CANCELLED
 */
public enum ParcelStatus {

    PENDING,           // Order created, not yet shipped
    IN_TRANSIT,        // Being shipped to the store
    READY_FOR_PICKUP,  // At the store, waiting for customer
    PICKED_UP,         // Customer has collected it
    DELIVERED,         // Delivery confirmed (terminal state)
    EXPIRED,           // Pickup deadline passed
    RETURNED,          // Sent back to sender (terminal state)
    CANCELLED;         // Order cancelled (terminal state)

    /**
     * Defines allowed state transitions for the parcel state machine.
     * Terminal states (DELIVERED, RETURNED, CANCELLED) have no transitions.
     */
    private static final Map<ParcelStatus, Set<ParcelStatus>> ALLOWED_TRANSITIONS = Map.of(
        PENDING, Set.of(IN_TRANSIT, CANCELLED),
        IN_TRANSIT, Set.of(READY_FOR_PICKUP, CANCELLED),
        READY_FOR_PICKUP, Set.of(PICKED_UP, EXPIRED, CANCELLED),
        PICKED_UP, Set.of(DELIVERED, CANCELLED),
        EXPIRED, Set.of(RETURNED, CANCELLED)
        // DELIVERED, RETURNED, CANCELLED are terminal states - no transitions allowed
    );

    /**
     * Check if transition from current status to target status is allowed.
     *
     * @param target the target status to transition to
     * @return true if transition is allowed, false otherwise
     */
    public boolean canTransitionTo(ParcelStatus target) {
        Set<ParcelStatus> allowed = ALLOWED_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    /**
     * Get all allowed transitions from current status.
     *
     * @return set of statuses that can be transitioned to, empty for terminal states
     */
    public Set<ParcelStatus> getAllowedTransitions() {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of());
    }

    /**
     * Check if this status is a terminal state (no further transitions allowed).
     *
     * @return true if terminal state
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED;
    }

    /**
     * Check if a parcel with this status can be soft deleted.
     * Only CANCELLED and RETURNED parcels can be deleted.
     *
     * @return true if parcel can be deleted
     */
    public boolean isDeletable() {
        return this == CANCELLED || this == RETURNED;
    }
}
