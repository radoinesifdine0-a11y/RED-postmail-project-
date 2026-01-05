package com.kantic.storeParcelsWS.mcp.tools;

import com.kantic.storeParcelsWS.mcp.McpTool;
import java.util.Map;

/**
 * Interface for MCP tool handlers.
 * Each tool implementation must provide:
 * - Tool name for routing
 * - Tool definition with schema for LLM
 * - Execute method for actual processing
 */
public interface McpToolHandler {

    /**
     * Get the tool name used for routing requests.
     */
    String getToolName();

    /**
     * Get the full tool definition including description and input schema.
     */
    McpTool getToolDefinition();

    /**
     * Execute the tool with the provided parameters.
     *
     * @param params Map of parameter names to values
     * @return Human-readable result string
     * @throws Exception if execution fails
     */
    String execute(Map<String, Object> params) throws Exception;
}
