package com.kantic.storeParcelsWS.mcp;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * MCP request DTO for tool invocation.
 */
public record McpRequest(
    @NotBlank String method,
    Map<String, Object> params
) {}
