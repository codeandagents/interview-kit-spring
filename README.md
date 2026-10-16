# AI Engineer Interview Kit 2026: Spring code

The code behind the Spring episodes of **AI Engineer Interview Kit 2026** on the
[Code & Agents](https://www.youtube.com/@CodeAndAgents) YouTube channel. Every answer in those
videos that shows code was built, tested and run from this repo; the terminal output on screen is real.

Java 25, Spring Boot 4.1.1, Spring AI 2.0.1 (Java MCP SDK 2.0.0). Each folder is its own Maven project
with the wrapper, so run the commands from inside a folder.

| Folder | Episode | What's in it |
|---|---|---|
| [`mcp-server`](mcp-server) | 2. MCP servers with Spring Boot | A support-desk MCP server (`@McpTool`, streamable HTTP) and end-to-end tests with a real MCP client |
| [`mcp-server-secured`](mcp-server-secured) | 2. MCP servers with Spring Boot | The same server as an OAuth2 resource server; `stub_issuer.py` is a local stand-in issuer for the 401 check (it signs nothing) |
| [`mcp-client`](mcp-client) | 2. MCP servers with Spring Boot | A Spring AI app that connects to the server and lists its tools |
| [`agent`](agent) | 4. AI agents with Spring AI | A support agent with tools, memory and a tool-loop limit |

The Python episodes are in [interview-kit-python](https://github.com/codeandagents/interview-kit-python).

## Run it

```bash
cd mcp-server
./mvnw test              # Windows: .\mvnw.cmd test
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.port=8766"   # http://localhost:8766/mcp
```

With the server running, build and run the client from another terminal (it expects the server on 8766):

```bash
cd mcp-client
./mvnw -q package -DskipTests
java -jar target/mcp-client-0.0.1-SNAPSHOT.jar   # prints one "tool:" line per tool
```

For the secured server, start the stand-in issuer first (`python stub_issuer.py`, port 9000): the app
resolves the issuer when it starts and refuses to boot without it.

`mcp-client` and `mcp-server-secured` have no tests of their own: each needs another process running (the
server, the issuer), so you check them by running them as above. `mcp-server` and `agent` have the tests.

## API keys

Only `agent` talks to a model, and its tests don't need one (they answer from a scripted stand-in server). To run it against a real model, set
`SPRING_AI_ANTHROPIC_API_KEY`. Any provider works: swap the Anthropic starter for another Spring AI
starter (OpenAI, for example) and set that provider's key; the agent code doesn't change.

## License

[MIT](LICENSE). Use it, change it, ship it.
