package com.kantic.storeParcelsWS.service;

import com.kantic.storeParcelsWS.dto.ParcelResponseDTO;
import com.kantic.storeParcelsWS.dto.SearchRequestDTO;
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
}
