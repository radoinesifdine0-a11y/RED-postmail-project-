package com.kantic.storeParcelsWS.mcp.tools;

import com.kantic.storeParcelsWS.dto.CriteriaDTO;
import com.kantic.storeParcelsWS.dto.PaginationDTO;
import com.kantic.storeParcelsWS.dto.ParcelResponseDTO;
import com.kantic.storeParcelsWS.dto.SearchRequestDTO;
import com.kantic.storeParcelsWS.mcp.McpTool;
import com.kantic.storeParcelsWS.service.ParcelService;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP Tool for searching parcels with various criteria.
 */
public class SearchParcelsTool implements McpToolHandler {

    private final ParcelService parcelService;

    public SearchParcelsTool(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @Override
    public String getToolName() {
        return "search_parcels";
    }

    @Override
    public McpTool getToolDefinition() {
        return new McpTool(
            "search_parcels",
            """
                Search for parcels in the store parcel system.

                Use this tool to:
                - Find parcels at a specific store (store_id)
                - Look up a customer's parcels by name
                - Filter by parcel status
                - Get details for a specific parcel or order

                At least one search criteria should be provided for best results.
                Results are paginated (max 50 per page).
                """,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "store_id", Map.of(
                        "type", "string",
                        "pattern", "^ST\\d{3}$",
                        "description", "Store ID (format: ST001-ST999)"
                    ),
                    "customer_first_name", Map.of(
                        "type", "string",
                        "description", "Customer's first name (partial match)"
                    ),
                    "customer_last_name", Map.of(
                        "type", "string",
                        "description", "Customer's last name (partial match)"
                    ),
                    "status", Map.of(
                        "type", "string",
                        "enum", List.of("PENDING", "IN_TRANSIT", "READY_FOR_PICKUP",
                            "PICKED_UP", "CANCELLED", "RETURNED", "EXPIRED", "DELIVERED"),
                        "description", "Parcel status filter"
                    ),
                    "parcel_id", Map.of(
                        "type", "string",
                        "description", "Exact parcel ID"
                    ),
                    "order_id", Map.of(
                        "type", "string",
                        "description", "Customer order ID"
                    ),
                    "page", Map.of(
                        "type", "integer",
                        "minimum", 1,
                        "default", 1,
                        "description", "Page number (1-indexed)"
                    ),
                    "page_size", Map.of(
                        "type", "integer",
                        "minimum", 10,
                        "maximum", 50,
                        "default", 20,
                        "description", "Results per page"
                    )
                )
            )
        );
    }

    @Override
    public String execute(Map<String, Object> params) throws Exception {
        List<CriteriaDTO> criterias = new ArrayList<>();

        if (params.containsKey("store_id")) {
            criterias.add(new CriteriaDTO("deliveryStoreId", "EQ", params.get("store_id")));
        }
        if (params.containsKey("customer_first_name")) {
            criterias.add(new CriteriaDTO("customer.firstName", "LIKE", params.get("customer_first_name")));
        }
        if (params.containsKey("customer_last_name")) {
            criterias.add(new CriteriaDTO("customer.lastName", "LIKE", params.get("customer_last_name")));
        }
        if (params.containsKey("status")) {
            criterias.add(new CriteriaDTO("parcelStatus", "EQ", params.get("status")));
        }
        if (params.containsKey("parcel_id")) {
            criterias.add(new CriteriaDTO("parcelId", "EQ", params.get("parcel_id")));
        }
        if (params.containsKey("order_id")) {
            criterias.add(new CriteriaDTO("customerOrderId", "EQ", params.get("order_id")));
        }

        int page = params.containsKey("page") ? ((Number) params.get("page")).intValue() : 1;
        int pageSize = params.containsKey("page_size") ? ((Number) params.get("page_size")).intValue() : 20;

        SearchRequestDTO request = new SearchRequestDTO();
        request.setCriterias(criterias);
        request.setPagination(new PaginationDTO(page, pageSize));

        Page<ParcelResponseDTO> result = parcelService.searchParcels(request);

        return formatSearchResults(result);
    }

    private String formatSearchResults(Page<ParcelResponseDTO> result) {
        if (result.isEmpty()) {
            return "No parcels found matching the search criteria.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Found %d parcels (showing %d on page %d of %d):\n\n",
            result.getTotalElements(),
            result.getContent().size(),
            result.getNumber() + 1,
            result.getTotalPages()));

        for (ParcelResponseDTO parcel : result.getContent()) {
            sb.append(String.format("""
                **%s** | %s | %s
                  Customer: %s %s
                  Store: %s | Paid: %s | Expires: %s

                """,
                parcel.getParcelId(),
                parcel.getParcelStatus(),
                parcel.getCustomerOrderId(),
                parcel.getCustomer() != null ? parcel.getCustomer().getFirstName() : "N/A",
                parcel.getCustomer() != null ? parcel.getCustomer().getLastName() : "",
                parcel.getDeliveryStoreId(),
                parcel.getIsPaid() != null && parcel.getIsPaid() ? "Yes" : "No",
                parcel.getExpirationDate() != null ? parcel.getExpirationDate().toString() : "N/A"
            ));
        }

        return sb.toString();
    }
}
