package com.kantic.storeParcelsWS.exception;

import com.kantic.storeParcelsWS.model.ParcelStatus;

import java.util.Set;

/**
 * Exception thrown when an invalid status transition is attempted.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private final ParcelStatus currentStatus;
    private final ParcelStatus targetStatus;

    public InvalidStatusTransitionException(ParcelStatus current, ParcelStatus target) {
        super(String.format("Cannot transition from %s to %s", current, target));
        this.currentStatus = current;
        this.targetStatus = target;
    }

    public ParcelStatus getCurrentStatus() {
        return currentStatus;
    }

    public ParcelStatus getTargetStatus() {
        return targetStatus;
    }

    public Set<ParcelStatus> getAllowedTransitions() {
        return currentStatus.getAllowedTransitions();
    }
}
