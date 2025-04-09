# Annotation-driven programming model

Annotation-driven programming model built on Spring
and inspired by Spring MVC.

Intended for use from Java, offering a familiar
model for Spring developers.

## Annotations

Classes can be annotated with `@Agentic`. This indicates the class contains agentic functions.

Functions can be annotated with the `@Action` or `@Condition` interfaces.

Agentic classes may choose to implement the `GoalContributor` interface.
Goals are immutable. The `goals` getter will be called once only.

### @Action methods

Signature choices:

- Transformer with regular Java or Kotlin implementation
- Prompt transformer
- Can have multiple arguments

TODO
Document `Prompt` and `PromptRunner`

### @Condition methods

TODO should support additional signatures such
as operations on a type or all of a type

### Tool callback methods

These follow the conventions of Spring AI and are annotated with `@Tool`.
They have access to instance state.

Tool methods are scoped to this type.
They will not be used on prompts outside it.
