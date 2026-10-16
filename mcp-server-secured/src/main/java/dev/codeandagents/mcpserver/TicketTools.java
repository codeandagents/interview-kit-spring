package dev.codeandagents.mcpserver;

import java.util.List;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class TicketTools {

    record Ticket(String title, String status) {}

    private static final Map<String, Ticket> TICKETS = Map.of(
            "T-1", new Ticket("Login page returns 500", "open"),
            "T-2", new Ticket("Invoice PDF is blank", "closed"));

    @McpTool(name = "get_ticket",
            description = "Look up one support ticket by id, for example T-1. Returns title and status.")
    public Ticket getTicket(@McpToolParam(description = "Ticket id, for example T-1") String ticketId) {
        Ticket ticket = TICKETS.get(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("No ticket " + ticketId + ". Known ids: T-1, T-2");
        }
        return ticket;
    }

    @McpTool(name = "search_tickets", description = "List ticket ids by status: open or closed.")
    public List<String> searchTickets(
            @McpToolParam(description = "open or closed", required = false) String status) {
        String wanted = status == null ? "open" : status;
        return TICKETS.entrySet().stream()
                .filter(e -> e.getValue().status().equals(wanted))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
