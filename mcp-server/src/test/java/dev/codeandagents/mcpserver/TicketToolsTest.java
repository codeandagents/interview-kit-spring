package dev.codeandagents.mcpserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TicketToolsTest {

    @LocalServerPort
    int port;

    private McpSyncClient client() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port).build();
        McpSyncClient client = McpClient.sync(transport).build();
        client.initialize();
        return client;
    }

    @Test
    void listsBothTools() {
        try (McpSyncClient client = client()) {
            var names = client.listTools().tools().stream().map(McpSchema.Tool::name).toList();
            assertThat(names).containsExactlyInAnyOrder("get_ticket", "search_tickets");
        }
    }

    @Test
    void readOnlyToolsSayTheyAreReadOnly() {
        try (McpSyncClient client = client()) {
            for (McpSchema.Tool tool : client.listTools().tools()) {
                assertThat(tool.annotations().readOnlyHint()).as(tool.name()).isTrue();
                assertThat(tool.annotations().destructiveHint()).as(tool.name()).isFalse();
            }
        }
    }

    @Test
    void ticketToolPublishesAnOutputSchema() {
        try (McpSyncClient client = client()) {
            var tool = client.listTools().tools().stream().filter(t -> t.name().equals("get_ticket")).findFirst().orElseThrow();
            assertThat(tool.outputSchema()).isNotNull();
        }
    }

    @Test
    void knownTicketComesBack() {
        try (McpSyncClient client = client()) {
            var result = client.callTool(new McpSchema.CallToolRequest("get_ticket", Map.of("ticketId", "T-1")));
            assertThat(result.isError()).isFalse();
            assertThat(result.content().toString()).contains("Login page returns 500");
        }
    }

    @Test
    void unknownTicketIsAnErrorTheModelCanRead() {
        try (McpSyncClient client = client()) {
            var result = client.callTool(new McpSchema.CallToolRequest("get_ticket", Map.of("ticketId", "T-9")));
            assertThat(result.isError()).isTrue();
            assertThat(result.content().toString()).contains("Known ids");
        }
    }
}
