package dev.codeandagents.agent;

import static dev.codeandagents.agent.ScriptedAnthropic.text;
import static dev.codeandagents.agent.ScriptedAnthropic.toolUse;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class SupportAgentTest {

    static ScriptedAnthropic anthropic;

    @DynamicPropertySource
    static void anthropicUrl(DynamicPropertyRegistry registry) throws Exception {
        anthropic = new ScriptedAnthropic();
        registry.add("spring.ai.anthropic.base-url", anthropic::baseUrl);
    }

    @AfterAll
    static void stop() {
        anthropic.close();
    }

    @Autowired
    SupportAgent agent;

    @Test
    void toolErrorGoesBackToTheModelAndItRecovers() {
        int before = anthropic.count();
        anthropic.then(toolUse("t1", "getTicket", "{\"ticketId\":\"T-9\"}"))
                .then(toolUse("t2", "getTicket", "{\"ticketId\":\"T-1\"}"))
                .then(text("T-1 is open: the login page returns 500."));

        String answer = agent.ask("recover", "What is the status of T-9?");

        assertThat(answer).contains("T-1 is open");
        assertThat(anthropic.count() - before).isEqualTo(3);                 // model, model, model
        String secondRequest = anthropic.requests.get(before + 1);
        assertThat(secondRequest).contains("No ticket T-9. Known ids: T-1, T-2"); // the model saw the failure
        assertThat(secondRequest).contains("\"tools\"").contains("getTicket");   // tool definitions are sent
        assertThat(secondRequest).contains("support desk agent");                // so is the system prompt
    }

    @Test
    void memoryCarriesOverWithinAConversationOnly() {
        int before = anthropic.count();
        anthropic.then(text("Noted: your name is Priya.")).then(text("Priya.")).then(text("I don't know."));

        agent.ask("c-1", "My name is Priya.");
        agent.ask("c-1", "What is my name?");
        agent.ask("c-2", "What is my name?");

        assertThat(anthropic.requests.get(before + 1)).contains("My name is Priya.");  // same conversation
        assertThat(anthropic.requests.get(before + 2)).doesNotContain("Priya");        // another conversation
    }

    @Test
    void structuredOutputBecomesARecord() {
        int before = anthropic.count();
        anthropic.then(text("{\"severity\":\"high\",\"owner\":\"web-team\"}"));

        SupportAgent.Triage triage = agent.triage("triage", "Triage T-1");

        assertThat(triage).isEqualTo(new SupportAgent.Triage("high", "web-team"));
        assertThat(anthropic.requests.get(before)).contains("JSON");   // the format instructions were added
    }

    @Test
    void aRunawayToolLoopIsStoppedByTheBuiltInLimit() {
        int before = anthropic.count();
        anthropic.afterwards(n -> toolUse("loop-" + n, "getTicket", "{\"ticketId\":\"T-1\"}"));   // never stops

        String answer = agent.ask("endless", "Keep checking T-1.");

        assertThat(answer).contains("Tool call limit (40) exceeded for tool 'getTicket'");
        assertThat(anthropic.count() - before).isEqualTo(41);   // 40 executed tool rounds, then the limit
    }

    @Test
    void toolContextReachesTheToolButNotTheModel() {
        int before = anthropic.count();
        anthropic.then(toolUse("c1", "openTicketCount", "{}")).then(text("You have 1 open ticket."));

        String answer = agent.askForTenant("ctx", "tenant-secret-42", "How many open tickets do I have?");

        assertThat(answer).contains("1 open ticket");
        assertThat(anthropic.requests.get(before + 1)).contains("tenant tenant-secret-42 has 1 open ticket"); // the tool saw it, and its result went back
        assertThat(anthropic.requests.get(before)).doesNotContain("tenant-secret-42");                          // the first prompt never carried it
    }

    @Test
    void withInternalExecutionOffTheAppSeesTheToolCallAndNothingRuns() {
        int before = anthropic.count();
        anthropic.then(toolUse("c9", "getTicket", "{\"ticketId\":\"T-1\"}"));

        var response = agent.proposeToolCalls("approval", "Look up T-1");

        assertThat(response.hasToolCalls()).isTrue();
        assertThat(response.getResult().getOutput().getToolCalls().get(0).name()).isEqualTo("getTicket");
        assertThat(anthropic.count() - before).isEqualTo(1);   // one model call; the tool was not executed
    }

    @Autowired
    MeterRegistry meters;

    @Test
    void metersRecordTheModelCallsAndTokens() {
        anthropic.then(text("ok"));
        agent.ask("metrics", "hello");

        var names = meters.getMeters().stream().map(m -> m.getId().getName()).filter(n -> n.startsWith("gen_ai")).distinct().sorted().toList();
        System.out.println("gen_ai meters: " + names);
        assertThat(names).contains("gen_ai.client.operation", "gen_ai.client.token.usage");
        meters.find("gen_ai.client.token.usage").meters().forEach(m -> System.out.println(
                "token usage: " + m.getId().getTags().stream().filter(t -> t.getKey().equals("gen_ai.token.type")).findFirst().map(t -> t.getKey() + "=" + t.getValue()).orElse(m.getId().getTags().toString())));
    }
}
