package dev.codeandagents.agent;

import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class TicketTools {

    private static final Map<String, String> TICKETS = Map.of(
            "T-1", "Login page returns 500 [open]",
            "T-2", "Invoice PDF is blank [closed]");

    @Tool(description = "Look up one support ticket by id, for example T-1. Returns title and status.")
    public String getTicket(@ToolParam(description = "Ticket id, for example T-1") String ticketId) {
        String ticket = TICKETS.get(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("No ticket " + ticketId + ". Known ids: T-1, T-2");
        }
        return ticketId + ": " + ticket;
    }

    /** The tenant comes from the application through ToolContext; the model never sees it. */
    @Tool(description = "Count the open tickets of the current customer.")
    public String openTicketCount(ToolContext context) {
        String tenant = (String) context.getContext().get("tenantId");
        return "tenant " + tenant + " has 1 open ticket";
    }
}
