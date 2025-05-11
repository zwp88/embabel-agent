# Coding style

## General

The project uses Maven, Kotlin and Spring Boot.

Follow the style of the code you read. Favor clarity.

Don't bother putting in licence headers, as build will do that.

Don't comment obvious things inline; only things that may be non-obvious.

Use consistent naming in the Spring idiom.

Use the Spring idiom where possible.

Favor immutability.

Use the `Schema` and related annotations to add information to types passed over the wire in the REST application.
This will improve Swagger/OpenAPI documentation.

Unless there is a specific reason not to, use the latest GA version of all dependencies.

Use @Nested classes in tests. Use `test complicated thing` instead of @DisplayName for test cases.

In log statements, use placeholders for efficiency at all logging levels.
E.g. logger.info("{} {}", a, b) instead of logger.info("computed string").

## Kotlin

Use Kotlin coding conventions and consistent formatting.

## Java

Use modern Java features like var, records, and switch expressions.
Use multiline strings rather than concatenation.