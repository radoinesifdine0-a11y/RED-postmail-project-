package com.kantic.storeParcelsWS.mcp.tools;

import com.kantic.storeParcelsWS.dto.ParcelResponseDTO;
import com.kantic.storeParcelsWS.mcp.McpTool;
import com.kantic.storeParcelsWS.service.ParcelService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP Tool for getting detailed information about a specific parcel.
 */
public class GetParcelTool implements McpToolHandler {

    private final ParcelService parcelService;

    public GetParcelTool(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @Override
    public String getToolName() {
        return "get_parcel";
    }

    @Override
    public McpTool getToolDefinition() {
        return new McpTool(
            "get_parcel",
            """
                Get detailed information about a specific parcel by its ID.

                Returns complete parcel information including customer details,
                status, storage location, and expiration date.
                """,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "parcel_id", Map.of(
                        "type", "string",
                        "description", "The parcel ID (e.g., P000001)"
                    )
                ),
                "required", List.of("parcel_id")
            )
        );
    }

    @Override
    public String execute(Map<String, Object> params) throws Exception {
        String parcelId = (String) params.get("parcel_id");

        if (parcelId == null || parcelId.isBlank()) {
            return "Error: parcel_id is required";
        }

        Optional<ParcelResponseDTO> parcelOpt = parcelService.getParcelById(parcelId);

        if (parcelOpt.isEmpty()) {
            return "Parcel " + parcelId + " not found.";
        }

        return formatParcelDetails(parcelOpt.get());
    }

    private String formatParcelDetails(ParcelResponseDTO parcel) {
        return String.format("""
            **Parcel Details**

            **Parcel ID:** %s
            **Order ID:** %s
            **Status:** %s
            **Store:** %s
            **Paid:** %s
            **Expired:** %s
            **Expiration Date:** %s

            **Customer:**
            - Name: %s %s
            - Email: %s
            - Phone: %s
            """,
            parcel.getParcelId(),
            parcel.getCustomerOrderId(),
            parcel.getParcelStatus(),
            parcel.getDeliveryStoreId(),
            parcel.getIsPaid() != null && parcel.getIsPaid() ? "Yes" : "No",
            parcel.getIsExpired() != null && parcel.getIsExpired() ? "Yes" : "No",
            parcel.getExpirationDate() != null ? parcel.getExpirationDate().toString() : "N/A",
            parcel.getCustomer() != null ? parcel.getCustomer().getFirstName() : "N/A",
            parcel.getCustomer() != null ? parcel.getCustomer().getLastName() : "",
            parcel.getCustomer() != null && parcel.getCustomer().getEmail() != null ? parcel.getCustomer().getEmail() : "N/A",
            parcel.getCustomer() != null && parcel.getCustomer().getPhoneNumber() != null ? parcel.getCustomer().getPhoneNumber() : "N/A"
        );
    }
}
