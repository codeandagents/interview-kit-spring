package dev.codeandagents.agent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import com.sun.net.httpserver.HttpServer;

/** A local stand-in for the Anthropic Messages API: scripted replies, no model, no spend. */
final class ScriptedAnthropic implements AutoCloseable {

    private final HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();
    final List<String> requests = new ArrayList<>();
    private final Deque<String> script = new ArrayDeque<>();
    private IntFunction<String> fallback;

    ScriptedAnthropic() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int n = requestCount.incrementAndGet();
            String reply;
            synchronized (this) {
                requests.add(body);
                reply = script.isEmpty() ? fallback.apply(n) : script.poll();
            }
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    int count() {
        return requestCount.get();
    }

    ScriptedAnthropic then(String reply) {
        script.add(reply);
        return this;
    }

    ScriptedAnthropic afterwards(IntFunction<String> fallback) {
        this.fallback = fallback;
        return this;
    }

    static String toolUse(String callId, String tool, String inputJson) {
        return message("[{\"type\":\"tool_use\",\"id\":\"" + callId + "\",\"name\":\"" + tool + "\",\"input\":" + inputJson + "}]",
                "tool_use");
    }

    static String text(String text) {
        return message("[{\"type\":\"text\",\"text\":" + quote(text) + "}]", "end_turn");
    }

    private static String message(String content, String stopReason) {
        return "{\"id\":\"msg_stub\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-haiku-4-5\","
                + "\"content\":" + content + ",\"stop_reason\":\"" + stopReason + "\",\"stop_sequence\":null,"
                + "\"usage\":{\"input_tokens\":10,\"output_tokens\":5}}";
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
