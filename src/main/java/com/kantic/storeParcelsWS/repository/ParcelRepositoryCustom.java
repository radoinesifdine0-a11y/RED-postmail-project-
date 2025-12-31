package com.kantic.storeParcelsWS.repository;

import com.kantic.storeParcelsWS.dto.SearchRequestDTO;
import com.kantic.storeParcelsWS.model.Parcel;
import org.springframework.data.domain.Page;

/**
 * Custom repository interface for complex queries.
 *
 * Why do we need this?
 * - Spring Data can auto-generate simple queries (findByParcelId)
 * - But for multi-criteria search with dynamic filters, we need custom code
 *
 * The implementation is in ParcelRepositoryCustomImpl.java
 */
public interface ParcelRepositoryCustom {

    /**
     * Search parcels with dynamic criteria, sorting, and pagination.
     *
     * @param request Contains search criteria, sort options, and pagination
     * @return Page of matching parcels
     */
    Page<Parcel> searchParcels(SearchRequestDTO request);
}
