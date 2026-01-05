package com.kantic.storeParcelsWS.controller;

import com.kantic.storeParcelsWS.mcp.*;
import com.kantic.storeParcelsWS.mcp.tools.*;
import com.kantic.storeParcelsWS.service.ParcelService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP (Model Context Protocol) Controller.
 *
 * Exposes MCP endpoints for LLM integration:
 * - GET  /mcp/tools           - List available tools
 * - POST /mcp/tools/call      - Execute a tool
 * - GET  /mcp/resources       - List available resources
 * - GET  /mcp/resources/read  - Read a resource
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {

    private final List<McpToolHandler> toolHandlers;

    public McpController(ParcelService parcelService) {
        this.toolHandlers = List.of(
            new SearchParcelsTool(parcelService),
            new GetParcelTool(parcelService),
            new CreateParcelTool(parcelService),
            new UpdateParcelStatusTool(parcelService),
            new DeleteParcelTool(parcelService)
        );
    }

    /**
     * List all available MCP tools.
     *
     * GET /mcp/tools
     */
    @GetMapping("/tools")
    public ResponseEntity<McpToolsResponse> listTools() {
        log.debug("Listing MCP tools");
        List<McpTool> tools = toolHandlers.stream()
            .map(McpToolHandler::getToolDefinition)
            .toList();
        return ResponseEntity.ok(new McpToolsResponse(tools));
    }

    /**
     * Execute an MCP tool.
     *
     * POST /mcp/tools/call
     */
    @PostMapping("/tools/call")
    public ResponseEntity<McpResponse> callTool(@Valid @RequestBody McpRequest request) {
        String toolName = request.method();
        Map<String, Object> params = request.params() != null ? request.params() : Map.of();

        log.info("MCP tool call: {} with params: {}", toolName, params);

        McpToolHandler handler = toolHandlers.stream()
            .filter(h -> h.getToolName().equals(toolName))
            .findFirst()
            .orElse(null);

        if (handler == null) {
            log.warn("Unknown MCP tool: {}", toolName);
            return ResponseEntity.badRequest()
                .body(McpResponse.error("Unknown tool: " + toolName));
        }

        try {
            String result = handler.execute(params);
            log.info("MCP tool {} executed successfully", toolName);
            return ResponseEntity.ok(McpResponse.success(result));
        } catch (Exception e) {
            log.error("MCP tool {} failed: {}", toolName, e.getMessage(), e);
            return ResponseEntity.ok(McpResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * List available MCP resources.
     *
     * GET /mcp/resources
     */
    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> listResources() {
        log.debug("Listing MCP resources");
        return ResponseEntity.ok(Map.of(
            "resources", List.of(
                Map.of(
                    "uri", "store-parcels://stores",
                    "name", "Available Stores",
                    "description", "List of all store locations (ST001-ST010)",
                    "mimeType", "application/json"
                ),
                Map.of(
                    "uri", "store-parcels://statuses",
                    "name", "Parcel Statuses",
                    "description", "Documentation of parcel statuses and allowed transitions",
                    "mimeType", "text/markdown"
                )
            )
        ));
    }

    /**
     * Read an MCP resource.
     *
     * GET /mcp/resources/read?uri=store-parcels://stores
     */
    @GetMapping("/resources/read")
    public ResponseEntity<Map<String, Object>> readResource(@RequestParam String uri) {
        log.debug("Reading MCP resource: {}", uri);
        return switch (uri) {
            case "store-parcels://stores" -> ResponseEntity.ok(Map.of(
                "uri", uri,
                "mimeType", "application/json",
                "text", getStoresJson()
            ));
            case "store-parcels://statuses" -> ResponseEntity.ok(Map.of(
                "uri", uri,
                "mimeType", "text/markdown",
                "text", getStatusesMarkdown()
            ));
            default -> {
                log.warn("Unknown MCP resource: {}", uri);
                yield ResponseEntity.badRequest().body(Map.of(
                    "error", "Unknown resource: " + uri
                ));
            }
        };
    }

    private String getStoresJson() {
        return """
            [
              {"id": "ST001", "name": "Paris Centre", "address": "123 Rue de Rivoli, 75001 Paris"},
              {"id": "ST002", "name": "Lyon Part-Dieu", "address": "45 Avenue de la Liberté, 69003 Lyon"},
              {"id": "ST003", "name": "Marseille Vieux Port", "address": "78 Quai du Port, 13002 Marseille"},
              {"id": "ST004", "name": "Bordeaux Sainte-Catherine", "address": "15 Rue Sainte-Catherine, 33000 Bordeaux"},
              {"id": "ST005", "name": "Lille Grand Place", "address": "3 Place du Général de Gaulle, 59800 Lille"},
              {"id": "ST006", "name": "Toulouse Capitole", "address": "22 Place du Capitole, 31000 Toulouse"},
              {"id": "ST007", "name": "Nice Promenade", "address": "88 Promenade des Anglais, 06000 Nice"},
              {"id": "ST008", "name": "Nantes Commerce", "address": "12 Place du Commerce, 44000 Nantes"},
              {"id": "ST009", "name": "Strasbourg Petite France", "address": "5 Rue du Bain aux Plantes, 67000 Strasbourg"},
              {"id": "ST010", "name": "Montpellier Comédie", "address": "8 Place de la Comédie, 34000 Montpellier"}
            ]
            """;
    }

    private String getStatusesMarkdown() {
        return """
            # Parcel Status Reference

            ## Available Statuses

            | Status | Description | Deletable |
            |--------|-------------|-----------|
            | PENDING | Parcel order created, not yet shipped | No |
            | IN_TRANSIT | Parcel is being shipped to the store | No |
            | READY_FOR_PICKUP | Parcel is at the store, waiting for customer | No |
            | PICKED_UP | Customer has received the parcel | No |
            | DELIVERED | Delivery confirmed (terminal) | No |
            | EXPIRED | Pickup deadline has passed | No |
            | RETURNED | Expired parcel returned to sender (terminal) | Yes |
            | CANCELLED | Parcel order was cancelled (terminal) | Yes |

            ## Status Transitions

            - PENDING -> IN_TRANSIT, CANCELLED
            - IN_TRANSIT -> READY_FOR_PICKUP, CANCELLED
            - READY_FOR_PICKUP -> PICKED_UP, EXPIRED, CANCELLED
            - PICKED_UP -> DELIVERED, CANCELLED
            - EXPIRED -> RETURNED, CANCELLED

            ## Terminal Statuses (no further changes)
            - DELIVERED, RETURNED, CANCELLED
            """;
    }
}
