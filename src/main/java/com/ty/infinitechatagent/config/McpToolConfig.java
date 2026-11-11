package com.ty.infinitechatagent.config;

import java.util.Arrays;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
public class McpToolConfig {

    @Value("${bigmodel.api-key}")
    private String apiKey;

    @Bean
    public McpToolProvider mcpToolProvider() {

        McpTransport searchTransport = new StreamableHttpMcpTransport.Builder()
                .url("https://open.bigmodel.cn/api/mcp/web_search/mcp")  // 用能响应的那个端点，去掉 ?Authorization
                .customHeaders(ctx -> Map.of("Authorization", apiKey.trim()))
                .logRequests(true)
                .logResponses(true)
                .build();

        McpClient searchClient = new DefaultMcpClient.Builder()
                .key("BigModelSearchMcpClient")
                .transport(searchTransport)
                .build();

        return McpToolProvider.builder()
                .mcpClients(searchClient)
                .build();
    }
}