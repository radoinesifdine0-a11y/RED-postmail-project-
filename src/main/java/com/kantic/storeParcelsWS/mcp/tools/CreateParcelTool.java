package com.kantic.storeParcelsWS.mcp.tools;

import com.kantic.storeParcelsWS.dto.CreateParcelRequestDTO;
import com.kantic.storeParcelsWS.dto.ParcelResponseDTO;
import com.kantic.storeParcelsWS.mcp.McpTool;
import com.kantic.storeParcelsWS.service.ParcelService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * MCP Tool for creating a new parcel.
 */
public class CreateParcelTool implements McpToolHandler {

    private final ParcelService parcelService;

    public CreateParcelTool(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @Override
    public String getToolName() {
        return "create_parcel";
    }

    @Override
    public McpTool getToolDefinition() {
        return new McpTool(
            "create_parcel",
            """
                Create a new parcel in the store parcel system.

                Use this when a customer needs to have a new parcel registered for pickup.
                The parcel will be created with status PENDING by default.
                Expiration date defaults to 14 days from now if not specified.
                """,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "parcel_id", Map.of(
                        "type", "string",
                        "description", "Unique parcel ID (e.g., P999001)"
                    ),
                    "order_id", Map.of(
                        "type", "string",
                        "description", "Customer order ID"
                    ),
                    "store_id", Map.of(
                        "type", "string",
                        "pattern", "^ST\\d{3}$",
                        "description", "Delivery store ID (format: ST001)"
                    ),
                    "customer_first_name", Map.of(
                        "type", "string",
                        "description", "Customer's first name"
                    ),
                    "customer_last_name", Map.of(
                        "type", "string",
                        "description", "Customer's last name"
                    ),
                    "customer_email", Map.of(
                        "type", "string",
                        "format", "email",
                        "description", "Customer's email (optional)"
                    ),
                    "customer_phone", Map.of(
                        "type", "string",
                        "description", "Customer's phone number (optional)"
                    ),
                    "is_paid", Map.of(
                        "type", "boolean",
                        "default", false,
                        "description", "Whether the parcel is paid"
                    )
                ),
                "required", List.of("parcel_id", "order_id", "store_id",
                    "customer_first_name", "customer_last_name")
            )
        );
    }

    @Override
    public String execute(Map<String, Object> params) throws Exception {
        // Validate required fields
        String parcelId = (String) params.get("parcel_id");
        String orderId = (String) params.get("order_id");
        String storeId = (String) params.get("store_id");
        String firstName = (String) params.get("customer_first_name");
        String lastName = (String) params.get("customer_last_name");

        if (parcelId == null || orderId == null || storeId == null ||
            firstName == null || lastName == null) {
            return "Error: parcel_id, order_id, store_id, customer_first_name, and customer_last_name are required";
        }

        // Build customer DTO
        CreateParcelRequestDTO.CustomerCreateDTO customer = CreateParcelRequestDTO.CustomerCreateDTO.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email((String) params.get("customer_email"))
            .phoneNumber((String) params.get("customer_phone"))
            .build();

        // Build parcel request
        CreateParcelRequestDTO request = CreateParcelRequestDTO.builder()
            .parcelId(parcelId)
            .customerOrderId(orderId)
            .deliveryStoreId(storeId)
            .customer(customer)
            .isPaid(params.containsKey("is_paid") ? (Boolean) params.get("is_paid") : false)
            .expirationDate(Instant.now().plus(14, ChronoUnit.DAYS))
            .build();

        try {
            ParcelResponseDTO parcel = parcelService.createParcel(request);

            return String.format("""
                **Parcel Created Successfully**

                **Parcel ID:** %s
                **Order ID:** %s
                **Store:** %s
                **Customer:** %s %s
                **Status:** PENDING
                **Paid:** %s
                **Expires:** %s

                The parcel is now registered in the system.
                """,
                parcel.getParcelId(),
                parcel.getCustomerOrderId(),
                parcel.getDeliveryStoreId(),
                firstName,
                lastName,
                parcel.getIsPaid() != null && parcel.getIsPaid() ? "Yes" : "No",
                parcel.getExpirationDate() != null ? parcel.getExpirationDate().toString() : "14 days from now"
            );
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }
}
