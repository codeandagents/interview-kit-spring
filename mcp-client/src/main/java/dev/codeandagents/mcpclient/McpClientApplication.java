package dev.codeandagents.mcpclient;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
    }

    @Bean
    CommandLineRunner listTools(SyncMcpToolCallbackProvider tools) {
        return args -> {
            for (var tool : tools.getToolCallbacks()) {
                System.out.println("tool: " + tool.getToolDefinition().name());
            }
        };
    }
}
