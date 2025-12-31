package com.kantic.storeParcelsWS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Customer information in API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
