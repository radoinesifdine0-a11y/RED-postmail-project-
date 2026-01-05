package com.kantic.storeParcelsWS.service;

import com.kantic.storeParcelsWS.dto.CreateParcelRequestDTO;
import com.kantic.storeParcelsWS.dto.CustomerResponseDTO;
import com.kantic.storeParcelsWS.dto.ParcelResponseDTO;
import com.kantic.storeParcelsWS.dto.SearchRequestDTO;
import com.kantic.storeParcelsWS.dto.UpdateParcelRequestDTO;
import com.kantic.storeParcelsWS.exception.InvalidStatusTransitionException;
import com.kantic.storeParcelsWS.exception.ParcelNotDeletableException;
import com.kantic.storeParcelsWS.exception.ParcelNotFoundException;
import com.kantic.storeParcelsWS.model.Customer;
import com.kantic.storeParcelsWS.model.Parcel;
import com.kantic.storeParcelsWS.model.ParcelStatus;
import com.kantic.storeParcelsWS.repository.ParcelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Override
    @Transactional
    public ParcelResponseDTO updateParcel(String parcelId, UpdateParcelRequestDTO request) {
        log.debug("Updating parcel: {}", parcelId);

        Parcel parcel = parcelRepository.findByParcelIdAndDeletedFalse(parcelId)
                .orElseThrow(() -> new ParcelNotFoundException(parcelId));

        // Update status with transition validation
        if (request.parcelStatus() != null) {
            ParcelStatus newStatus = ParcelStatus.valueOf(request.parcelStatus());
            if (!parcel.getStatus().canTransitionTo(newStatus)) {
                throw new InvalidStatusTransitionException(parcel.getStatus(), newStatus);
            }
            parcel.setStatus(newStatus);
            parcel.setStatusUpdateDate(Instant.now());
            log.info("Parcel {} status changed from {} to {}", parcelId, parcel.getStatus(), newStatus);
        }

        // Update isPaid
        if (request.isPaid() != null) {
            parcel.setIsPaid(request.isPaid());
        }

        // Update expirationDate and recalculate isExpired
        if (request.expirationDate() != null) {
            parcel.setExpirationDate(request.expirationDate());
            parcel.setIsExpired(request.expirationDate().isBefore(Instant.now()));
        }

        // Update customer contact info (only email and phone, not name)
        if (request.customer() != null) {
            Customer customer = parcel.getCustomer();
            if (customer != null) {
                if (request.customer().email() != null) {
                    customer.setEmail(request.customer().email());
                }
                if (request.customer().phoneNumber() != null) {
                    customer.setPhoneNumber(request.customer().phoneNumber());
                }
            }
        }

        // Update storage location
        if (request.storageLocation() != null && request.storageLocation().locationCode() != null) {
            if (parcel.getStorageLocation() != null) {
                parcel.getStorageLocation().setLocationCode(request.storageLocation().locationCode());
            }
        }

        Parcel saved = parcelRepository.save(parcel);
        log.info("Parcel {} updated successfully", parcelId);

        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void deleteParcel(String parcelId) {
        log.debug("Deleting parcel: {}", parcelId);

        Parcel parcel = parcelRepository.findByParcelIdAndDeletedFalse(parcelId)
                .orElseThrow(() -> new ParcelNotFoundException(parcelId));

        if (!parcel.getStatus().isDeletable()) {
            throw new ParcelNotDeletableException(parcelId, parcel.getStatus());
        }

        parcel.setDeleted(true);
        parcelRepository.save(parcel);

        log.info("Parcel {} soft deleted successfully", parcelId);
    }

    @Override
    @Transactional
    public ParcelResponseDTO createParcel(CreateParcelRequestDTO request) {
        log.debug("Creating parcel: {}", request.getParcelId());

        // Check if parcel ID already exists
        if (parcelRepository.existsByParcelId(request.getParcelId())) {
            throw new IllegalArgumentException("Parcel ID already exists: " + request.getParcelId());
        }

        // Build customer from request
        Customer customer = Customer.builder()
            .firstName(request.getCustomer().getFirstName())
            .lastName(request.getCustomer().getLastName())
            .email(request.getCustomer().getEmail())
            .phoneNumber(request.getCustomer().getPhoneNumber())
            .build();

        // Build parcel with PENDING status
        Instant expirationDate = request.getExpirationDate() != null
            ? request.getExpirationDate()
            : Instant.now().plus(14, java.time.temporal.ChronoUnit.DAYS);

        Parcel parcel = Parcel.builder()
            .parcelId(request.getParcelId())
            .orderId(request.getCustomerOrderId())
            .pickupStoreId(request.getDeliveryStoreId())
            .status(ParcelStatus.PENDING)
            .statusUpdateDate(Instant.now())
            .expirationDate(expirationDate)
            .isExpired(false)
            .isPaid(request.getIsPaid() != null ? request.getIsPaid() : false)
            .deleted(false)
            .customer(customer)
            .build();

        Parcel saved = parcelRepository.save(parcel);
        log.info("Parcel {} created successfully", request.getParcelId());

        return toResponseDTO(saved);
    }
}
