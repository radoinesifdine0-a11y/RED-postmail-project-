package com.kantic.storeParcelsWS.service;

import com.kantic.storeParcelsWS.dto.CustomerResponseDTO;
import com.kantic.storeParcelsWS.dto.ParcelResponseDTO;
import com.kantic.storeParcelsWS.dto.SearchRequestDTO;
import com.kantic.storeParcelsWS.model.Customer;
import com.kantic.storeParcelsWS.model.Parcel;
import com.kantic.storeParcelsWS.repository.ParcelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service implementation for parcel operations.
 *
 * @Service - Marks this as a Spring-managed bean
 * @RequiredArgsConstructor - Lombok generates constructor for final fields
 * @Slf4j - Lombok creates a logger named "log"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelServiceImpl implements ParcelService {

    private final ParcelRepository parcelRepository;

    @Override
    public Page<ParcelResponseDTO> searchParcels(SearchRequestDTO request) {
        log.debug("Searching parcels with {} criteria", request.getCriterias().size());

        // Execute search through repository
        Page<Parcel> parcels = parcelRepository.searchParcels(request);

        // Convert entities to DTOs
        // Page.map() transforms each element while preserving pagination info
        return parcels.map(this::toResponseDTO);
    }

    @Override
    public Optional<ParcelResponseDTO> getParcelById(String parcelId) {
        log.debug("Getting parcel by ID: {}", parcelId);

        return parcelRepository.findByParcelId(parcelId)
                .map(this::toResponseDTO);
    }

    /**
     * Convert Parcel entity to response DTO.
     *
     * Why convert?
     * - Hide internal fields (MongoDB _id)
     * - Control what data is exposed
     * - Decouple API contract from database schema
     */
    private ParcelResponseDTO toResponseDTO(Parcel parcel) {
        return ParcelResponseDTO.builder()
                .parcelId(parcel.getParcelId())
                .customerOrderId(parcel.getOrderId())
                .deliveryStoreId(parcel.getPickupStoreId())
                .parcelStatus(parcel.getStatus() != null ? parcel.getStatus().name() : null)
                .isPaid(parcel.getIsPaid())
                .expirationDate(parcel.getExpirationDate())
                .isExpired(parcel.getIsExpired())
                .customer(toCustomerDTO(parcel.getCustomer()))
                .build();
    }

    /**
     * Convert Customer entity to response DTO.
     */
    private CustomerResponseDTO toCustomerDTO(Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerResponseDTO.builder()
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .build();
    }
}
