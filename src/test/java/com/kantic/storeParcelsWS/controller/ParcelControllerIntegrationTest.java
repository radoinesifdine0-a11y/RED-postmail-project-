package com.kantic.storeParcelsWS.controller;

import com.kantic.storeParcelsWS.model.Customer;
import com.kantic.storeParcelsWS.model.Parcel;
import com.kantic.storeParcelsWS.model.ParcelStatus;
import com.kantic.storeParcelsWS.model.StorageLocation;
import com.kantic.storeParcelsWS.repository.ParcelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ParcelController with real MongoDB via Testcontainers.
 * These tests are skipped in CI (GitHub Actions) where MongoDB runs as a service container.
 * In CI, the unit tests provide sufficient coverage.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "Testcontainers not needed in CI - MongoDB service container is used")
class ParcelControllerIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParcelRepository parcelRepository;

    @BeforeEach
    void setup() {
        parcelRepository.deleteAll();
    }

    @Test
    void updateParcel_shouldReturn200AndUpdatedParcel() throws Exception {
        // Given
        Parcel parcel = createAndSaveParcel(ParcelStatus.READY_FOR_PICKUP);

        String requestBody = """
            {
                "parcelStatus": "PICKED_UP",
                "isPaid": true
            }
            """;

        // When/Then
        mockMvc.perform(put("/store_parcels/parcel/" + parcel.getParcelId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parcelStatus").value("PICKED_UP"))
            .andExpect(jsonPath("$.isPaid").value(true));
    }

    @Test
    void updateParcel_invalidTransition_shouldReturn400() throws Exception {
        // Given
        Parcel parcel = createAndSaveParcel(ParcelStatus.DELIVERED);

        String requestBody = """
            {
                "parcelStatus": "CANCELLED"
            }
            """;

        // When/Then
        mockMvc.perform(put("/store_parcels/parcel/" + parcel.getParcelId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid Status Transition"))
            .andExpect(jsonPath("$.currentStatus").value("DELIVERED"))
            .andExpect(jsonPath("$.targetStatus").value("CANCELLED"))
            .andExpect(jsonPath("$.allowedTransitions").isArray());
    }

    @Test
    void updateParcel_notFound_shouldReturn404() throws Exception {
        String requestBody = """
            {
                "isPaid": true
            }
            """;

        mockMvc.perform(put("/store_parcels/parcel/P999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Parcel Not Found"))
            .andExpect(jsonPath("$.parcelId").value("P999999"));
    }

    @Test
    void updateParcel_shouldUpdateCustomerEmail() throws Exception {
        // Given
        Parcel parcel = createAndSaveParcel(ParcelStatus.READY_FOR_PICKUP);

        String requestBody = """
            {
                "customer": {
                    "email": "newemail@example.com"
                }
            }
            """;

        // When/Then
        mockMvc.perform(put("/store_parcels/parcel/" + parcel.getParcelId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customer.email").value("newemail@example.com"));
    }

    @Test
    void deleteParcel_cancelledParcel_shouldReturn204() throws Exception {
        // Given
        Parcel parcel = createAndSaveParcel(ParcelStatus.CANCELLED);

        // When/Then
        mockMvc.perform(delete("/store_parcels/parcel/" + parcel.getParcelId()))
            .andExpect(status().isNoContent());

        // Verify soft delete
        Parcel deleted = parcelRepository.findByParcelId(parcel.getParcelId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getDeleted()).isTrue();
    }

    @Test
    void deleteParcel_returnedParcel_shouldReturn204() throws Exception {
        // Given
        Parcel parcel = createAndSaveParcel(ParcelStatus.RETURNED);

        // When/Then
        mockMvc.perform(delete("/store_parcels/parcel/" + parcel.getParcelId()))
            .andExpect(status().isNoContent());

        // Verify soft delete
        Parcel deleted = parcelRepository.findByParcelId(parcel.getParcelId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getDeleted()).isTrue();
    }

    @Test
    void deleteParcel_activeParcel_shouldReturn400() throws Exception {
        // Given
        Parcel parcel = createAndSaveParcel(ParcelStatus.READY_FOR_PICKUP);

        // When/Then
        mockMvc.perform(delete("/store_parcels/parcel/" + parcel.getParcelId()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Parcel Not Deletable"))
            .andExpect(jsonPath("$.currentStatus").value("READY_FOR_PICKUP"))
            .andExpect(jsonPath("$.deletableStatuses").isArray());
    }

    @Test
    void deleteParcel_deliveredParcel_shouldReturn400() throws Exception {
        // Given
        Parcel parcel = createAndSaveParcel(ParcelStatus.DELIVERED);

        // When/Then
        mockMvc.perform(delete("/store_parcels/parcel/" + parcel.getParcelId()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Parcel Not Deletable"))
            .andExpect(jsonPath("$.currentStatus").value("DELIVERED"));
    }

    @Test
    void deleteParcel_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/store_parcels/parcel/P999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Parcel Not Found"));
    }

    @Test
    void searchParcels_shouldExcludeDeletedParcels() throws Exception {
        // Given - create two parcels, delete one
        Parcel activeParcel = createAndSaveParcel(ParcelStatus.READY_FOR_PICKUP);
        Parcel deletedParcel = createAndSaveParcel(ParcelStatus.CANCELLED);

        // Soft delete the second parcel
        deletedParcel.setDeleted(true);
        parcelRepository.save(deletedParcel);

        String requestBody = """
            {
                "criterias": [
                    {
                        "field": "deliveryStoreId",
                        "operator": "EQ",
                        "value": "ST001"
                    }
                ],
                "pagination": {
                    "page": 1,
                    "pageSize": 20
                }
            }
            """;

        // When/Then - should only return the active parcel
        mockMvc.perform(post("/store_parcels/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$._embedded.storeParcels[0].parcelId").value(activeParcel.getParcelId()));
    }

    private Parcel createAndSaveParcel(ParcelStatus status) {
        String parcelId = "P" + System.currentTimeMillis() + Math.random();
        Parcel parcel = Parcel.builder()
            .parcelId(parcelId)
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
        return parcelRepository.save(parcel);
    }
}
