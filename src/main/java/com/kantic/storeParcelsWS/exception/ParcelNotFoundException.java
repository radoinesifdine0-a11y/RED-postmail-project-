package com.kantic.storeParcelsWS.exception;

/**
 * Exception thrown when a parcel is not found by its ID.
 */
public class ParcelNotFoundException extends RuntimeException {

    private final String parcelId;

    public ParcelNotFoundException(String parcelId) {
        super("Parcel not found: " + parcelId);
        this.parcelId = parcelId;
    }

    public String getParcelId() {
        return parcelId;
    }
}
