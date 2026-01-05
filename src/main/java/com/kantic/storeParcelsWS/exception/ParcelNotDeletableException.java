package com.kantic.storeParcelsWS.exception;

import com.kantic.storeParcelsWS.model.ParcelStatus;

/**
 * Exception thrown when attempting to delete a parcel that cannot be deleted.
 * Only parcels with CANCELLED or RETURNED status can be deleted.
 */
public class ParcelNotDeletableException extends RuntimeException {

    private final String parcelId;
    private final ParcelStatus status;

    public ParcelNotDeletableException(String parcelId, ParcelStatus status) {
        super(String.format("Parcel %s with status %s cannot be deleted", parcelId, status));
        this.parcelId = parcelId;
        this.status = status;
    }

    public String getParcelId() {
        return parcelId;
    }

    public ParcelStatus getStatus() {
        return status;
    }
}
