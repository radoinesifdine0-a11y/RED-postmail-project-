package com.kantic.storeParcelsWS.mcp;

import java.util.Map;

/**
 * MCP tool definition with name, description, and input schema.
 */
public record McpTool(
    String name,
    String description,
    Map<String, Object> inputSchema
) {}
