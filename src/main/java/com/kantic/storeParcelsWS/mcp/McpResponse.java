package com.kantic.storeParcelsWS.mcp;

import java.util.List;

/**
 * MCP response DTO with content and error status.
 */
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
