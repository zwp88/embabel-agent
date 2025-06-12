# Embabel Agent Auto-Configuration

## Objective
Bootstrap beans defined in @**Configuration** artifacts for Spring Boot Application.

## Auto-Configuration Types

1. **DefaultAgentAutoConfiguration** - bootstraps Agent Platform Configuration,  Tools Group Configuration, and RAG Service Configuration


## Usage

Required dependency:

**embabel-agent-api-starter**

Example:
Include

**@ImportAutoConfiguration(DefaultAgentAutoConfiguration.class)**

into Spring Boot Application