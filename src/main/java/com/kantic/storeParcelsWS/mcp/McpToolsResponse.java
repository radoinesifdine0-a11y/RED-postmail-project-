package com.kantic.storeParcelsWS.mcp;

import java.util.List;

/**
 * MCP response containing list of available tools.
 */
public record McpToolsResponse(
    List<McpTool> tools
) {}
