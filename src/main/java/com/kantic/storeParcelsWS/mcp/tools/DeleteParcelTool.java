package com.kantic.storeParcelsWS.mcp.tools;

import com.kantic.storeParcelsWS.exception.ParcelNotDeletableException;
import com.kantic.storeParcelsWS.exception.ParcelNotFoundException;
import com.kantic.storeParcelsWS.mcp.McpTool;
import com.kantic.storeParcelsWS.service.ParcelService;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool for deleting (soft delete) a parcel.
 */
public class DeleteParcelTool implements McpToolHandler {

    private final ParcelService parcelService;

    public DeleteParcelTool(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @Override
    public String getToolName() {
        return "delete_parcel";
    }

    @Override
    public McpTool getToolDefinition() {
        return new McpTool(
            "delete_parcel",
            """
                Delete (soft delete) a parcel from the system.

                IMPORTANT: Only parcels with status CANCELLED or RETURNED can be deleted.
                Active parcels cannot be deleted - they must first be cancelled or returned.
                """,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "parcel_id", Map.of(
                        "type", "string",
                        "description", "The parcel ID to delete"
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

        try {
            parcelService.deleteParcel(parcelId);
            return "**Parcel " + parcelId + " has been deleted successfully.**";
        } catch (ParcelNotFoundException e) {
            return "Error: Parcel " + parcelId + " not found.";
        } catch (ParcelNotDeletableException e) {
            return String.format("""
                **Cannot Delete Parcel**

                Parcel %s has status **%s** and cannot be deleted.

                Only parcels with these statuses can be deleted:
                - CANCELLED
                - RETURNED

                You must first change the parcel status to CANCELLED before deleting.
                """,
                parcelId,
                e.getStatus()
            );
        }
    }
}
