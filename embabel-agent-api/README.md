# Embabel Agent Framework

API project.

## Consuming MCP Servers

The Embabel Agent Framework provides built-in support for consuming Model Context Protocol (MCP) servers, allowing you to extend your applications with powerful AI capabilities through standardized interfaces.

### What is MCP?

Model Context Protocol (MCP) is an open protocol that standardizes how applications provide context and extra functionality to large language models. Introduced by Anthropic, MCP has emerged as the de facto standard for connecting AI agents to tools, functioning as a client-server protocol where:

- **Clients** (like Embabel Agent) send requests to servers
- **Servers** process those requests to deliver necessary context to the AI model

MCP simplifies integration between AI applications and external tools, transforming an "M×N problem" into an "M+N problem" through standardization - similar to what USB did for hardware peripherals.

### Configuring MCP in Embabel Agent

To configure MCP servers in your Embabel Agent application, add the following to your `application.yml`:

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        name: embabel
        version: 1.0.0
        request-timeout: 30s
        type: SYNC
        stdio:
          connections:
            docker-mcp:
              command: docker
              args:
                - run
                - -i
                - --rm
                - alpine/socat
                - STDIO
                - TCP:host.docker.internal:8811
```

This configuration sets up an MCP client that connects to a Docker-based MCP server. The connection uses STDIO transport through Docker's socat utility to connect to a TCP endpoint.

### Docker Desktop MCP Integration

Docker has embraced MCP with their Docker MCP Catalog and Toolkit, which provides:

1. **Centralized Discovery** - A trusted hub for discovering MCP tools integrated into Docker Hub
2. **Containerized Deployment** - Run MCP servers as containers without complex setup
3. **Secure Credential Management** - Centralized, encrypted credential handling
4. **Built-in Security** - Sandbox isolation and permissions management

The Docker MCP ecosystem includes over 100 verified tools from partners like Stripe, Elastic, Neo4j, and more, all accessible through Docker's infrastructure.

### Learn More

- [Docker MCP Documentation](https://docs.docker.com/desktop/features/gordon/mcp/)
- [Docker MCP Servers Repository](https://github.com/docker/mcp-servers)
- [Introducing Docker MCP Catalog and Toolkit](https://www.docker.com/blog/introducing-docker-mcp-catalog-and-toolkit/)
- [MCP Introduction and Overview](https://www.philschmid.de/mcp-introduction)

--------------------
(c) Embabel Software Inc 2024-2025.
