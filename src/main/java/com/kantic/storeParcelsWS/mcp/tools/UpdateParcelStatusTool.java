package com.kantic.storeParcelsWS.mcp.tools;

import com.kantic.storeParcelsWS.dto.ParcelResponseDTO;
import com.kantic.storeParcelsWS.dto.UpdateParcelRequestDTO;
import com.kantic.storeParcelsWS.exception.InvalidStatusTransitionException;
import com.kantic.storeParcelsWS.exception.ParcelNotFoundException;
import com.kantic.storeParcelsWS.mcp.McpTool;
import com.kantic.storeParcelsWS.model.ParcelStatus;
import com.kantic.storeParcelsWS.service.ParcelService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP Tool for updating a parcel's status.
 */
public class UpdateParcelStatusTool implements McpToolHandler {

    private final ParcelService parcelService;

    public UpdateParcelStatusTool(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @Override
    public String getToolName() {
        return "update_parcel_status";
    }

    @Override
    public McpTool getToolDefinition() {
        return new McpTool(
            "update_parcel_status",
            """
                Update the status of a parcel.

                Valid status transitions:
                - PENDING -> IN_TRANSIT, CANCELLED
                - IN_TRANSIT -> READY_FOR_PICKUP, CANCELLED
                - READY_FOR_PICKUP -> PICKED_UP, EXPIRED, CANCELLED
                - PICKED_UP -> DELIVERED, CANCELLED
                - EXPIRED -> RETURNED, CANCELLED

                Terminal statuses (no further changes): DELIVERED, RETURNED, CANCELLED
                """,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "parcel_id", Map.of(
                        "type", "string",
                        "description", "The parcel ID to update"
                    ),
                    "new_status", Map.of(
                        "type", "string",
                        "enum", List.of("IN_TRANSIT", "READY_FOR_PICKUP", "PICKED_UP",
                            "DELIVERED", "EXPIRED", "RETURNED", "CANCELLED"),
                        "description", "The new status"
                    ),
                    "mark_as_paid", Map.of(
                        "type", "boolean",
                        "description", "Mark the parcel as paid (optional)"
                    )
                ),
                "required", List.of("parcel_id", "new_status")
            )
        );
    }

    @Override
    public String execute(Map<String, Object> params) throws Exception {
        String parcelId = (String) params.get("parcel_id");
        String newStatus = (String) params.get("new_status");

        if (parcelId == null || newStatus == null) {
            return "Error: parcel_id and new_status are required";
        }

        // Build update request
        UpdateParcelRequestDTO request = new UpdateParcelRequestDTO(
            newStatus,
            params.containsKey("mark_as_paid") ? (Boolean) params.get("mark_as_paid") : null,
            null,
            null,
            null
        );

        try {
            ParcelResponseDTO parcel = parcelService.updateParcel(parcelId, request);

            StringBuilder result = new StringBuilder();
            result.append(String.format("""
                **Parcel Updated Successfully**

                **Parcel ID:** %s
                **New Status:** %s
                """,
                parcel.getParcelId(),
                parcel.getParcelStatus()
            ));

            if (params.containsKey("mark_as_paid")) {
                result.append(String.format("**Payment Status:** %s\n",
                    parcel.getIsPaid() != null && parcel.getIsPaid() ? "Paid" : "Not paid"));
            }

            return result.toString();

        } catch (ParcelNotFoundException e) {
            return "Error: Parcel " + parcelId + " not found.";
        } catch (InvalidStatusTransitionException e) {
            Set<ParcelStatus> allowed = e.getAllowedTransitions();
            String allowedStr = allowed.isEmpty()
                ? "None (terminal status)"
                : allowed.stream().map(Enum::name).collect(Collectors.joining(", "));

            return String.format("""
                **Invalid Status Transition**

                Cannot change status from **%s** to **%s**.

                Allowed transitions from %s:
                %s
                """,
                e.getCurrentStatus(),
                e.getTargetStatus(),
                e.getCurrentStatus(),
                allowedStr
            );
        }
    }
}
