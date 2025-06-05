# Embabel Agent Examples

This module provides a suite of example agents and supporting code to demonstrate how to use the [Embabel Agent](https://github.com/embabel/embabel-agent) framework for building agentic flows in both Java and Kotlin. The examples are organized by language and domain, allowing you to quickly learn, test, and extend agent functionality.

---

## Directory Structure

- **examples-common/**  
  Shared utilities and code used across multiple examples.

- **examples-dependencies/**  
  Maven dependency management for example submodules.

- **examples-java/**  
  Java-based agent examples.

- **examples-kotlin/**  
  Kotlin-based agent examples and DSL demonstrations.

- **pom.xml**  
  Maven configuration for building and managing all examples.

---

## Getting Started

### Prerequisites

- Java 21 or newer
- Maven 3.9+
- [Embabel Agent Parent Project](https://github.com/embabel/embabel-agent) checked out and built locally

### Building the Examples

To build all examples:

```bash
mvn clean install
```

Or, to build a specific example directory:

```bash
cd examples-kotlin
mvn clean install
```

---

## Running Example Agents

Several startup scripts are provided in the `embabel-agent-api/scripts` directory to make it easy to run the shell with the example agents enabled. These scripts set up environment variables and Maven profiles to activate different agent examples.

### Usage via Provided Scripts

#### Unix/Linux/macOS

```bash
cd ../embabel-agent-api/scripts
./shell.sh
```

#### Windows (CMD)

```cmd
cd ..\embabel-agent-api\scripts
shell.cmd
```

#### Windows (PowerShell)

```powershell
cd ..\embabel-agent-api\scripts
.\shell.ps1
```

#### Docker-Desktop Profile (CMD)

```cmd
cd ..\embabel-agent-api\scripts
shell_docker.cmd
```

Each script activates relevant Spring profiles (such as `shell`, `starwars`, `severance`, or `docker-desktop`) and the Maven profile `agent-examples-kotlin` to include the example agents in the shell.

---

## Example Projects

- **Dogfood Agent (Kotlin):** Demonstrates a simple agent workflow in Kotlin.
- **Horoscope Agent (Java & Kotlin):** Sample agent providing horoscope data.
- **Movie Agent (Kotlin):** Movie recommendation agent.
- **Travel Agent (Kotlin):** Travel advice and planning agent.

These examples cover prompt engineering, tool integration, and orchestration patterns using Embabel Agent.

---

## Maven Profiles

Example agents are included as Maven profiles for flexible activation. For example:

```bash
mvn spring-boot:run -Pagent-examples-kotlin
```

Profiles can be combined with Spring profiles to control which agents and features are active.

---

## Contributing

Contributions are welcome! Please see the [main repository’s contributing guidelines](https://github.com/embabel/embabel-agent/blob/main/CONTRIBUTING.md).

---

## License

This module is part of the Embabel Agent project and is licensed under the Apache License 2.0. See the [LICENSE](../LICENSE) file for details.

---