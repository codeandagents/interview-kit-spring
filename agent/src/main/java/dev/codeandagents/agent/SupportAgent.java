package dev.codeandagents.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class SupportAgent {

    record Triage(String severity, String owner) {}

    private final ChatClient chat;

    SupportAgent(ChatClient.Builder builder, ChatMemory memory, TicketTools tools) {
        this.chat = builder
                .defaultSystem("You are a support desk agent. Use the ticket tool to look tickets up.")
                .defaultTools(tools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }

    public String ask(String conversationId, String question) {
        return chat.prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    public Triage triage(String conversationId, String question) {
        return chat.prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(Triage.class);
    }

    public String askForTenant(String conversationId, String tenantId, String question) {
        return chat.prompt()
                .user(question)
                .toolContext(java.util.Map.of("tenantId", tenantId))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    /** The app, not Spring AI, decides whether the requested tool calls run (a place for human approval). */
    public ChatResponse proposeToolCalls(String conversationId, String question) {
        return chat.prompt()
                .user(question)
                .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .chatResponse();
    }
}
