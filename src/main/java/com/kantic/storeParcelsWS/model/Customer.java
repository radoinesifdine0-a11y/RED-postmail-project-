package com.kantic.storeParcelsWS.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded document representing customer information.
 *
 * This is NOT a separate collection - it's embedded inside the Parcel document.
 * In MongoDB, this appears as:
 * {
 *   "parcelId": "P000001",
 *   "customer": {
 *     "firstName": "John",
 *     "lastName": "Doe",
 *     "email": "john@example.com",
 *     "phoneNumber": "+33612345678"
 *   }
 * }
 *
 * Lombok annotations:
 * @Data - Generates getters, setters, toString, equals, hashCode
 * @Builder - Enables fluent builder pattern: Customer.builder().firstName("John").build()
 * @NoArgsConstructor - Generates empty constructor (required by Spring)
 * @AllArgsConstructor - Generates constructor with all fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
