package com.kantic.storeParcelsWS.service;

import com.kantic.storeParcelsWS.dto.CreateParcelRequestDTO;
import com.kantic.storeParcelsWS.dto.ParcelResponseDTO;
import com.kantic.storeParcelsWS.dto.SearchRequestDTO;
import com.kantic.storeParcelsWS.dto.UpdateParcelRequestDTO;
import com.kantic.storeParcelsWS.model.Parcel;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Service interface for parcel operations.
 *
 * Why use an interface?
 * - Allows different implementations (real, mock for testing)
 * - Follows dependency inversion principle
 * - Makes code easier to test
 */
public interface ParcelService {

    /**
     * Search for parcels based on criteria.
     *
     * @param request Search criteria, sorting, pagination
     * @return Page of matching parcels
     */
    Page<ParcelResponseDTO> searchParcels(SearchRequestDTO request);

    /**
     * Get a single parcel by its ID.
     *
     * @param parcelId The business parcel ID (e.g., "P000001")
     * @return The parcel if found
     */
    Optional<ParcelResponseDTO> getParcelById(String parcelId);

    /**
     * Update an existing parcel.
     *
     * @param parcelId The business parcel ID
     * @param request The update request with fields to modify
     * @return The updated parcel
     * @throws com.kantic.storeParcelsWS.exception.ParcelNotFoundException if parcel not found
     * @throws com.kantic.storeParcelsWS.exception.InvalidStatusTransitionException if status transition is invalid
     */
    ParcelResponseDTO updateParcel(String parcelId, UpdateParcelRequestDTO request);

    /**
     * Soft delete a parcel.
     * Only parcels with CANCELLED or RETURNED status can be deleted.
     *
     * @param parcelId The business parcel ID
     * @throws com.kantic.storeParcelsWS.exception.ParcelNotFoundException if parcel not found
     * @throws com.kantic.storeParcelsWS.exception.ParcelNotDeletableException if parcel cannot be deleted
     */
    void deleteParcel(String parcelId);

    /**
     * Create a new parcel.
     *
     * @param request The create request with parcel details
     * @return The created parcel
     * @throws IllegalArgumentException if parcel ID already exists
     */
    ParcelResponseDTO createParcel(CreateParcelRequestDTO request);
}
