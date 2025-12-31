# Partie 3

# Store Parcel API - Part 3: Contrôleur MCP

## Extension du Training - Model Context Protocol dans Spring Boot

**Prérequis:** Avoir complété les Parts 1 et 2

**Objectif:** Ajouter un contrôleur MCP (Model Context Protocol) dans le même projet Spring Boot permettant à Claude et autres LLMs d'interagir avec l'API Store Parcels via des outils structurés.


---

## 1. Introduction au MCP

### 1.1 Qu'est-ce que MCP ?

**Model Context Protocol (MCP)** est un standard ouvert développé par Anthropic permettant aux LLMs d'interagir avec des systèmes externes de manière structurée et sécurisée.

**Concepts clés:**

* **Tools**: Fonctions que le LLM peut appeler (comme `search_parcels`, `create_parcel`)
* **Resources**: Données que le LLM peut lire (comme une liste de stores)
* **Prompts**: Templates de prompts prédéfinis (optionnel)

**Architecture (projet unique):**

```mermaidjs

flowchart LR
    Claude["🤖 Claude Code<br/>(MCP Client)"]
    MCP["📦 MCP Controller<br/>(Spring Boot)"]
    Service["🔧 ParcelService"]
    DB[(MongoDB)]

    Claude <-->|"MCP Protocol<br/>(stdio/HTTP)"| MCP
    MCP --> Service
    Service --> DB

    style Claude fill:#f3e5f5,stroke:#7b1fa2
    style MCP fill:#e3f2fd,stroke:#1976d2
    style Service fill:#fff3e0,stroke:#f57c00
    style DB fill:#e8f5e9,stroke:#388e3c
```

### 1.2 Pourquoi un Contrôleur MCP intégré ?

* **Un seul projet**: Pas de projet séparé à maintenir
* **Réutilisation**: Utilise les mêmes services que l'API REST
* **Simplicité**: Même stack Java/Spring Boot
* **Cohérence**: Mêmes validations et règles métier


---

## 2. Architecture du Contrôleur MCP

### 2.1 Stack Technique (même que Part 1 & 2)

| Composant | Technologie |
|----|----|
| Language | Java 21 |
| Framework | Spring Boot 3.4.4 |
| Database | MongoDB 6.0+ |
| Build | Maven |
| Container | Docker |

### 2.2 Structure Additionnelle

```
src/main/java/com/kantic/storeparcels/
├── controller/
│   ├── ParcelController.java     # API REST (existant)
│   └── McpController.java        # NEW: Contrôleur MCP
├── mcp/
│   ├── McpRequest.java           # DTO requête MCP
│   ├── McpResponse.java          # DTO réponse MCP
│   ├── McpTool.java              # Définition d'un tool
│   └── tools/
│       ├── SearchParcelsTool.java
│       ├── GetParcelTool.java
│       ├── CreateParcelTool.java
│       ├── UpdateParcelStatusTool.java
│       └── DeleteParcelTool.java
├── service/
│   └── ParcelService.java        # (existant, réutilisé)
└── ...
```


---

## 3. DTOs MCP

### 3.1 McpRequest

```java

package com.kantic.storeparcels.mcp;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record McpRequest(
    @NotBlank String method,
    Map<String, Object> params
) {}
```

### 3.2 McpResponse

```java

package com.kantic.storeparcels.mcp;

import java.util.List;

public record McpResponse(
    List<Content> content,
    boolean isError
) {
    public record Content(
        String type,
        String text
    ) {}

    public static McpResponse success(String text) {
        return new McpResponse(
            List.of(new Content("text", text)),
            false
        );
    }

    public static McpResponse error(String text) {
        return new McpResponse(
            List.of(new Content("text", text)),
            true
        );
    }
}
```

### 3.3 McpTool

```java

package com.kantic.storeparcels.mcp;

import java.util.Map;

public record McpTool(
    String name,
    String description,
    Map<String, Object> inputSchema
) {}
```

### 3.4 McpToolsResponse

```java

package com.kantic.storeparcels.mcp;

import java.util.List;

public record McpToolsResponse(
    List<McpTool> tools
) {}
```


---

## 4. Contrôleur MCP Principal

### 4.1 McpController.java

```java

package com.kantic.storeparcels.controller;

import com.kantic.storeparcels.mcp.*;
import com.kantic.storeparcels.mcp.tools.*;
import com.kantic.storeparcels.service.ParcelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private final ParcelService parcelService;
    private final List<McpToolHandler> toolHandlers;

    public McpController(ParcelService parcelService) {
        this.parcelService = parcelService;
        this.toolHandlers = List.of(
            new SearchParcelsTool(parcelService),
            new GetParcelTool(parcelService),
            new CreateParcelTool(parcelService),
            new UpdateParcelStatusTool(parcelService),
            new DeleteParcelTool(parcelService)
        );
    }

    /**
     * Liste tous les tools MCP disponibles
     */
    @GetMapping("/tools")
    public ResponseEntity<McpToolsResponse> listTools() {
        List<McpTool> tools = toolHandlers.stream()
            .map(McpToolHandler::getToolDefinition)
            .toList();
        return ResponseEntity.ok(new McpToolsResponse(tools));
    }

    /**
     * Exécute un tool MCP
     */
    @PostMapping("/tools/call")
    public ResponseEntity<McpResponse> callTool(@Valid @RequestBody McpRequest request) {
        String toolName = request.method();
        Map<String, Object> params = request.params();

        McpToolHandler handler = toolHandlers.stream()
            .filter(h -> h.getToolName().equals(toolName))
            .findFirst()
            .orElse(null);

        if (handler == null) {
            return ResponseEntity.badRequest()
                .body(McpResponse.error("Unknown tool: " + toolName));
        }

        try {
            String result = handler.execute(params);
            return ResponseEntity.ok(McpResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(McpResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * Liste les resources MCP disponibles
     */
    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> listResources() {
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
     * Lecture d'une resource MCP
     */
    @GetMapping("/resources/read")
    public ResponseEntity<Map<String, Object>> readResource(@RequestParam String uri) {
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
            default -> ResponseEntity.badRequest().body(Map.of(
                "error", "Unknown resource: " + uri
            ));
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

            - PENDING → IN_TRANSIT, CANCELLED
            - IN_TRANSIT → READY_FOR_PICKUP, CANCELLED
            - READY_FOR_PICKUP → PICKED_UP, EXPIRED, CANCELLED
            - PICKED_UP → DELIVERED, RETURNED
            - EXPIRED → RETURNED, CANCELLED

            ## Terminal Statuses (no further changes)
            - DELIVERED, RETURNED, CANCELLED
            """;
    }
}
```


---

## 5. Interface Tool Handler

### 5.1 McpToolHandler.java

```java

package com.kantic.storeparcels.mcp.tools;

import com.kantic.storeparcels.mcp.McpTool;
import java.util.Map;

public interface McpToolHandler {

    String getToolName();

    McpTool getToolDefinition();

    String execute(Map<String, Object> params) throws Exception;
}
```


---

## 6. Tools à Implémenter

### 6.1 Tool: search_parcels

```java

package com.kantic.storeparcels.mcp.tools;

import com.kantic.storeparcels.dto.SearchRequestDTO;
import com.kantic.storeparcels.dto.CriteriaDTO;
import com.kantic.storeparcels.dto.PaginationDTO;
import com.kantic.storeparcels.mcp.McpTool;
import com.kantic.storeparcels.service.ParcelService;
import org.springframework.hateoas.PagedModel;

import java.util.*;

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

        var result = parcelService.searchParcels(request);

        return formatSearchResults(result);
    }

    private String formatSearchResults(PagedModel<?> result) {
        var metadata = result.getMetadata();
        var parcels = result.getContent();

        if (parcels.isEmpty()) {
            return "No parcels found matching the search criteria.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Found %d parcels (showing %d on page %d of %d):\n\n",
            metadata.getTotalElements(),
            parcels.size(),
            metadata.getNumber() + 1,
            metadata.getTotalPages()));

        for (var item : parcels) {
            // Format each parcel - adapt based on your actual DTO structure
            sb.append(item.toString()).append("\n\n");
        }

        return sb.toString();
    }
}
```

### 6.2 Tool: get_parcel

```java

package com.kantic.storeparcels.mcp.tools;

import com.kantic.storeparcels.mcp.McpTool;
import com.kantic.storeparcels.service.ParcelService;
import java.util.*;

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

        var parcel = parcelService.getParcelById(parcelId);

        if (parcel == null) {
            return "Parcel " + parcelId + " not found.";
        }

        return formatParcelDetails(parcel);
    }

    private String formatParcelDetails(Object parcel) {
        // Adapt based on your actual Parcel entity/DTO
        return String.format("""
            **Parcel Details**

            %s
            """, parcel.toString());
    }
}
```

### 6.3 Tool: create_parcel

```java

package com.kantic.storeparcels.mcp.tools;

import com.kantic.storeparcels.dto.CreateParcelRequestDTO;
import com.kantic.storeparcels.dto.CustomerDTO;
import com.kantic.storeparcels.mcp.McpTool;
import com.kantic.storeparcels.service.ParcelService;
import java.time.LocalDateTime;
import java.util.*;

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
        CreateParcelRequestDTO request = new CreateParcelRequestDTO();
        request.setParcelId((String) params.get("parcel_id"));
        request.setCustomerOrderId((String) params.get("order_id"));
        request.setDeliveryStoreId((String) params.get("store_id"));
        request.setParcelStatus("PENDING");
        request.setExpirationDateTime(LocalDateTime.now().plusDays(14));
        request.setIsPaid(params.containsKey("is_paid") ? (Boolean) params.get("is_paid") : false);

        CustomerDTO customer = new CustomerDTO();
        customer.setFirstName((String) params.get("customer_first_name"));
        customer.setLastName((String) params.get("customer_last_name"));
        customer.setEmail((String) params.get("customer_email"));
        customer.setPhoneNumber((String) params.get("customer_phone"));
        request.setCustomer(customer);

        var parcel = parcelService.createParcel(request);

        return String.format("""
            ✅ **Parcel Created Successfully**

            **Parcel ID:** %s
            **Order ID:** %s
            **Store:** %s
            **Customer:** %s %s
            **Status:** PENDING
            **Expires:** %s

            The parcel is now registered in the system.
            """,
            parcel.getParcelId(),
            parcel.getCustomerOrderId(),
            parcel.getDeliveryStoreId(),
            customer.getFirstName(),
            customer.getLastName(),
            parcel.getExpirationDateTime()
        );
    }
}
```

### 6.4 Tool: update_parcel_status

```java

package com.kantic.storeparcels.mcp.tools;

import com.kantic.storeparcels.dto.UpdateParcelRequestDTO;
import com.kantic.storeparcels.exception.InvalidStatusTransitionException;
import com.kantic.storeparcels.mcp.McpTool;
import com.kantic.storeparcels.service.ParcelService;
import java.util.*;

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

                Valid status transitions (from Part 2 state machine):
                - PENDING → IN_TRANSIT, CANCELLED
                - IN_TRANSIT → READY_FOR_PICKUP, CANCELLED
                - READY_FOR_PICKUP → PICKED_UP, EXPIRED, CANCELLED
                - PICKED_UP → DELIVERED, RETURNED
                - EXPIRED → RETURNED, CANCELLED

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

        UpdateParcelRequestDTO request = new UpdateParcelRequestDTO();
        request.setParcelStatus(newStatus);

        if (params.containsKey("mark_as_paid")) {
            request.setIsPaid((Boolean) params.get("mark_as_paid"));
        }

        try {
            var parcel = parcelService.updateParcel(parcelId, request);

            return String.format("""
                ✅ **Parcel Updated Successfully**

                **Parcel ID:** %s
                **New Status:** %s
                %s
                """,
                parcel.getParcelId(),
                parcel.getParcelStatus(),
                params.containsKey("mark_as_paid")
                    ? "**Payment Status:** " + (parcel.getIsPaid() ? "Paid" : "Not paid")
                    : ""
            );
        } catch (InvalidStatusTransitionException e) {
            return String.format("""
                ❌ **Invalid Status Transition**

                Cannot change status from **%s** to **%s**.

                Allowed transitions from %s:
                %s
                """,
                e.getCurrentStatus(),
                e.getTargetStatus(),
                e.getCurrentStatus(),
                formatAllowedTransitions(e.getAllowedTransitions())
            );
        }
    }

    private String formatAllowedTransitions(List<String> transitions) {
        if (transitions == null || transitions.isEmpty()) {
            return "- None (terminal status)";
        }
        return transitions.stream()
            .map(t -> "- " + t)
            .reduce((a, b) -> a + "\n" + b)
            .orElse("- None");
    }
}
```

### 6.5 Tool: delete_parcel

```java

package com.kantic.storeparcels.mcp.tools;

import com.kantic.storeparcels.exception.ParcelNotDeletableException;
import com.kantic.storeparcels.mcp.McpTool;
import com.kantic.storeparcels.service.ParcelService;
import java.util.*;

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

        try {
            parcelService.deleteParcel(parcelId);
            return "✅ **Parcel " + parcelId + " has been deleted successfully.**";
        } catch (ParcelNotDeletableException e) {
            return String.format("""
                ❌ **Cannot Delete Parcel**

                Parcel %s has status **%s** and cannot be deleted.

                Only parcels with these statuses can be deleted:
                - CANCELLED
                - RETURNED

                You must first change the parcel status to CANCELLED before deleting.
                """,
                parcelId,
                e.getCurrentStatus()
            );
        }
    }
}
```


---

## 7. Configuration Claude Code

### 7.1 Option A: Wrapper Script (Recommandé)

Créer un script wrapper qui démarre l'API et expose MCP via HTTP :

**scripts/mcp-wrapper.sh:**

```bash
#!/bin/bash
# MCP Wrapper for Store Parcels API

API_URL="${STORE_API_URL:-http://localhost:8080}"

# Simple MCP-to-HTTP bridge using curl

while IFS= read -r line; do
    # Parse JSON-RPC request and convert to HTTP calls
    response=$(curl -s -X POST "$API_URL/mcp/tools/call" \
        -H "Content-Type: application/json" \
        -d "$line")
    echo "$response"
done
```

### 7.2 Option B: MCP via HTTP Direct

Claude Code peut appeler directement les endpoints HTTP :

**Configuration** `**.mcp.json**` **dans le projet:**

```json
{
  "mcpServers": {
    "store-parcels": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "description": "Store Parcels API MCP endpoints"
    }
  }
}
```

### 7.3 Test des Endpoints MCP

```bash
# Lister les tools disponibles

curl http://localhost:8080/mcp/tools

# Appeler search_parcels

curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "method": "search_parcels",
    "params": {
      "store_id": "ST001",
      "page": 1,
      "page_size": 10
    }
  }'

# Appeler get_parcel

curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "method": "get_parcel",
    "params": {
      "parcel_id": "P000001"
    }
  }'

# Lister les resources

curl http://localhost:8080/mcp/resources

# Lire une resource

curl "http://localhost:8080/mcp/resources/read?uri=store-parcels://stores"
```


---

## 8. Checkpoints Part 3

### Checkpoint 3.1: Controller MCP créé

**Validation:**

```bash
# L'application démarre sans erreur
./mvnw spring-boot:run

# L'endpoint /mcp/tools répond

curl http://localhost:8080/mcp/tools
# Devrait lister 5 tools
```

**Success Criteria:**

* McpController.java ajouté
* DTOs MCP créés
* Endpoint /mcp/tools fonctionnel

**Estimated Time:** 2-3 hours


---

### Checkpoint 3.2: Tool search_parcels

**Validation:**

```bash

curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"method": "search_parcels", "params": {"store_id": "ST001"}}'
```

**Success Criteria:**

* Recherche avec critères fonctionne
* Pagination fonctionne
* Format de sortie lisible

**Estimated Time:** 3-4 hours


---

### Checkpoint 3.3: Tool get_parcel

**Validation:**

```bash

curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"method": "get_parcel", "params": {"parcel_id": "P000001"}}'
```

**Success Criteria:**

* Lookup par ID fonctionne
* Gestion du 404
* Format détaillé

**Estimated Time:** 2 hours


---

### Checkpoint 3.4: Tool create_parcel

**Validation:**

```bash

curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "method": "create_parcel",
    "params": {
      "parcel_id": "P999999",
      "order_id": "ORD999999",
      "store_id": "ST001",
      "customer_first_name": "Test",
      "customer_last_name": "User"
    }
  }'
```

**Success Criteria:**

* Création fonctionne
* Gestion du duplicate (409)
* Confirmation claire

**Estimated Time:** 2-3 hours


---

### Checkpoint 3.5: Tool update_parcel_status

**Validation:**

```bash
# Update valide

curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"method": "update_parcel_status", "params": {"parcel_id": "P000001", "new_status": "IN_TRANSIT"}}'

# Transition invalide (doit afficher les options)
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"method": "update_parcel_status", "params": {"parcel_id": "P000001", "new_status": "DELIVERED"}}'
```

**Success Criteria:**

* Update fonctionne
* Transitions invalides = message explicite
* Liste des transitions possibles

**Estimated Time:** 2-3 hours


---

### Checkpoint 3.6: Tool delete_parcel

**Validation:**

```bash
# Delete sur CANCELLED (devrait marcher)
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"method": "delete_parcel", "params": {"parcel_id": "P_CANCELLED"}}'

# Delete sur PENDING (devrait échouer)
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"method": "delete_parcel", "params": {"parcel_id": "P000002"}}'
```

**Success Criteria:**

* Delete sur CANCELLED/RETURNED OK
* Delete sur autres statuts = erreur explicite

**Estimated Time:** 2 hours


---

### Checkpoint 3.7: Resources MCP

**Validation:**

```bash

curl http://localhost:8080/mcp/resources

curl "http://localhost:8080/mcp/resources/read?uri=store-parcels://stores"
curl "http://localhost:8080/mcp/resources/read?uri=store-parcels://statuses"
```

**Success Criteria:**

* Resources listées
* Contenu JSON et Markdown accessible

**Estimated Time:** 1-2 hours


---

## 9. Livrables Part 3

- [ ] `McpController.java` - Contrôleur principal MCP
- [ ] `mcp/McpRequest.java` - DTO requête
- [ ] `mcp/McpResponse.java` - DTO réponse
- [ ] `mcp/McpTool.java` - Définition tool
- [ ] `mcp/tools/McpToolHandler.java` - Interface
- [ ] `mcp/tools/SearchParcelsTool.java`
- [ ] `mcp/tools/GetParcelTool.java`
- [ ] `mcp/tools/CreateParcelTool.java`
- [ ] `mcp/tools/UpdateParcelStatusTool.java`
- [ ] `mcp/tools/DeleteParcelTool.java`
- [ ] Tests des endpoints MCP
- [ ] `.mcp.json` (optionnel, pour Claude Code)


---

## 10. Critères d'Évaluation Part 3

| Critère | Poids |
|----|----|
| Tools fonctionnels et complets | 40% |
| Qualité des descriptions (LLM-friendly) | 20% |
| Gestion des erreurs | 15% |
| Resources informatifs | 10% |
| Réutilisation du ParcelService | 10% |
| Tests | 5% |


---

## 11. Ressources

* [MCP Specification](https://spec.modelcontextprotocol.io/)
* [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/3.4.4/reference/html/)
* [Building REST Services with Spring](https://spring.io/guides/tutorials/rest)