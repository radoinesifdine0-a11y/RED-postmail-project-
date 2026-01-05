package com.kantic.storeParcelsWS.service;

import com.kantic.storeParcelsWS.dto.ParcelResponseDTO;
import com.kantic.storeParcelsWS.dto.UpdateParcelRequestDTO;
import com.kantic.storeParcelsWS.exception.InvalidStatusTransitionException;
import com.kantic.storeParcelsWS.exception.ParcelNotDeletableException;
import com.kantic.storeParcelsWS.exception.ParcelNotFoundException;
import com.kantic.storeParcelsWS.model.Customer;
import com.kantic.storeParcelsWS.model.Parcel;
import com.kantic.storeParcelsWS.model.ParcelStatus;
import com.kantic.storeParcelsWS.model.StorageLocation;
import com.kantic.storeParcelsWS.repository.ParcelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ParcelServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ParcelServiceImplTest {

    @Mock
    private ParcelRepository parcelRepository;

    @InjectMocks
    private ParcelServiceImpl parcelService;

    private Parcel testParcel;

    @BeforeEach
    void setUp() {
        testParcel = createParcelWithStatus(ParcelStatus.READY_FOR_PICKUP);
    }

    @Test
    void updateParcel_shouldUpdateStatusAndTimestamp() {
        // Given
        when(parcelRepository.findByParcelIdAndDeletedFalse("P000001"))
            .thenReturn(Optional.of(testParcel));
        when(parcelRepository.save(any(Parcel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateParcelRequestDTO request = new UpdateParcelRequestDTO(
            "PICKED_UP", null, null, null, null
        );

        // When
        ParcelResponseDTO result = parcelService.updateParcel("P000001", request);

        // Then
        assertThat(result.getParcelStatus()).isEqualTo("PICKED_UP");
        verify(parcelRepository).save(argThat(p ->
            p.getStatus() == ParcelStatus.PICKED_UP &&
            p.getStatusUpdateDate() != null
        ));
    }

    @Test
    void updateParcel_shouldRejectInvalidTransition() {
        // Given
        Parcel deliveredParcel = createParcelWithStatus(ParcelStatus.DELIVERED);
        when(parcelRepository.findByParcelIdAndDeletedFalse("P000001"))
            .thenReturn(Optional.of(deliveredParcel));

        UpdateParcelRequestDTO request = new UpdateParcelRequestDTO(
            "CANCELLED", null, null, null, null
        );

        // When/Then
        assertThatThrownBy(() -> parcelService.updateParcel("P000001", request))
            .isInstanceOf(InvalidStatusTransitionException.class)
            .hasMessageContaining("Cannot transition from DELIVERED to CANCELLED");

        verify(parcelRepository, never()).save(any());
    }

    @Test
    void updateParcel_shouldUpdateIsPaid() {
        // Given
        when(parcelRepository.findByParcelIdAndDeletedFalse("P000001"))
            .thenReturn(Optional.of(testParcel));
        when(parcelRepository.save(any(Parcel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateParcelRequestDTO request = new UpdateParcelRequestDTO(
            null, true, null, null, null
        );

        // When
        ParcelResponseDTO result = parcelService.updateParcel("P000001", request);

        // Then
        assertThat(result.getIsPaid()).isTrue();
    }

    @Test
    void updateParcel_shouldUpdateExpirationDateAndRecalculateIsExpired() {
        // Given
        when(parcelRepository.findByParcelIdAndDeletedFalse("P000001"))
            .thenReturn(Optional.of(testParcel));
        when(parcelRepository.save(any(Parcel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
        UpdateParcelRequestDTO request = new UpdateParcelRequestDTO(
            null, null, pastDate, null, null
        );

        // When
        ParcelResponseDTO result = parcelService.updateParcel("P000001", request);

        // Then
        assertThat(result.getIsExpired()).isTrue();
        assertThat(result.getExpirationDate()).isEqualTo(pastDate);
    }

    @Test
    void updateParcel_shouldUpdateCustomerEmail() {
        // Given
        when(parcelRepository.findByParcelIdAndDeletedFalse("P000001"))
            .thenReturn(Optional.of(testParcel));
        when(parcelRepository.save(any(Parcel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateParcelRequestDTO.CustomerUpdateDTO customerUpdate =
            new UpdateParcelRequestDTO.CustomerUpdateDTO("newemail@example.com", null);
        UpdateParcelRequestDTO request = new UpdateParcelRequestDTO(
            null, null, null, customerUpdate, null
        );

        // When
        ParcelResponseDTO result = parcelService.updateParcel("P000001", request);

        // Then
        assertThat(result.getCustomer().getEmail()).isEqualTo("newemail@example.com");
    }

    @Test
    void updateParcel_shouldThrowNotFoundForNonExistentParcel() {
        // Given
        when(parcelRepository.findByParcelIdAndDeletedFalse("P999999"))
            .thenReturn(Optional.empty());

        UpdateParcelRequestDTO request = new UpdateParcelRequestDTO(
            null, true, null, null, null
        );

        // When/Then
        assertThatThrownBy(() -> parcelService.updateParcel("P999999", request))
            .isInstanceOf(ParcelNotFoundException.class)
            .hasMessageContaining("P999999");
    }

    @Test
    void deleteParcel_shouldSoftDeleteCancelledParcel() {
        // Given
        Parcel cancelledParcel = createParcelWithStatus(ParcelStatus.CANCELLED);
        when(parcelRepository.findByParcelIdAndDeletedFalse("P000001"))
            .thenReturn(Optional.of(cancelledParcel));
        when(parcelRepository.save(any(Parcel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        parcelService.deleteParcel("P000001");

        // Then
        verify(parcelRepository).save(argThat(p -> p.getDeleted()));
    }

    @Test
    void deleteParcel_shouldSoftDeleteReturnedParcel() {
        // Given
        Parcel returnedParcel = createParcelWithStatus(ParcelStatus.RETURNED);
        when(parcelRepository.findByParcelIdAndDeletedFalse("P000001"))
            .thenReturn(Optional.of(returnedParcel));
        when(parcelRepository.save(any(Parcel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        parcelService.deleteParcel("P000001");

        // Then
        verify(parcelRepository).save(argThat(p -> p.getDeleted()));
    }

    @Test
    void deleteParcel_shouldRejectActiveParcel() {
        // Given
        Parcel activeParcel = createParcelWithStatus(ParcelStatus.READY_FOR_PICKUP);
        when(parcelRepository.findByParcelIdAndDeletedFalse("P000001"))
            .thenReturn(Optional.of(activeParcel));

        // When/Then
        assertThatThrownBy(() -> parcelService.deleteParcel("P000001"))
            .isInstanceOf(ParcelNotDeletableException.class)
            .hasMessageContaining("READY_FOR_PICKUP");

        verify(parcelRepository, never()).save(any());
    }

    @Test
    void deleteParcel_shouldRejectDeliveredParcel() {
        // Given
        Parcel deliveredParcel = createParcelWithStatus(ParcelStatus.DELIVERED);
        when(parcelRepository.findByParcelIdAndDeletedFalse("P000001"))
            .thenReturn(Optional.of(deliveredParcel));

        // When/Then
        assertThatThrownBy(() -> parcelService.deleteParcel("P000001"))
            .isInstanceOf(ParcelNotDeletableException.class)
            .hasMessageContaining("DELIVERED");

        verify(parcelRepository, never()).save(any());
    }

    @Test
    void deleteParcel_shouldThrowNotFoundForNonExistentParcel() {
        // Given
        when(parcelRepository.findByParcelIdAndDeletedFalse("P999999"))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> parcelService.deleteParcel("P999999"))
            .isInstanceOf(ParcelNotFoundException.class)
            .hasMessageContaining("P999999");
    }

    private Parcel createParcelWithStatus(ParcelStatus status) {
        return Parcel.builder()
            .parcelId("P000001")
            .orderId("ORD123456")
            .pickupStoreId("ST001")
            .status(status)
            .statusUpdateDate(Instant.now())
            .expirationDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .isExpired(false)
            .isPaid(false)
            .deleted(false)
            .customer(Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("+33612345678")
                .build())
            .storageLocation(StorageLocation.builder()
                .storeId("ST001")
                .locationCode("RACK-A-1")
                .build())
            .build();
    }
}
