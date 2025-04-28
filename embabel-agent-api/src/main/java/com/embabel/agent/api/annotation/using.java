package com.embabel.agent.api.annotation;

import com.embabel.agent.api.common.PromptRunner;
import com.embabel.common.ai.model.BuildableLlmOptions;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.ai.prompt.PromptContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * Java syntax sugar for running prompts.
 * We use lower case to create a DSL style.
 */
public class using implements PromptRunner {

    public static using DEFAULT_LLM = new using();

    public static using llm(@NonNull LlmOptions llmOptions) {
        return new using(llmOptions);
    }

    private final LlmOptions llmOptions;

    private final List<ToolCallback> toolCallbacks = List.of();

    private final List<PromptContributor> promptContributors = List.of();

    /**
     * Constructor for WithLlm.
     *
     * @param llmOptions the LLM options to use
     */
    private using(@NonNull LlmOptions llmOptions) {
        this.llmOptions = llmOptions;
    }

    private using() {
        this(new BuildableLlmOptions());
    }

    public using withToolCallbacks(@NonNull List<ToolCallback> toolCallbacks) {
        this.toolCallbacks.addAll(toolCallbacks);
        return this;
    }

    public using withPromptContributors(@NonNull List<PromptContributor> promptContributors) {
        this.promptContributors.addAll(promptContributors);
        return this;
    }

    @Override
    public <T> T createObject(@NotNull String prompt, @NotNull Class<T> outputClass) {
        return PromptsKt.using(llmOptions, toolCallbacks, promptContributors)
                .createObject(prompt, outputClass);
    }

    @Override
    public <T> @Nullable T createObjectIfPossible(@NotNull String prompt, @NotNull Class<T> outputClass) {
        return PromptsKt.using(llmOptions, toolCallbacks, promptContributors)
                .createObjectIfPossible(prompt, outputClass);
    }

    @Override
    @NotNull
    public String generateText(@NotNull String prompt) {
        return PromptsKt.using(llmOptions, toolCallbacks, promptContributors).generateText(prompt);
    }

    @Override
    @Nullable
    public LlmOptions getLlm() {
        return llmOptions;
    }

    @Override
    @NotNull
    public List<ToolCallback> getToolCallbacks() {
        return toolCallbacks;
    }

    @Override
    @NotNull
    public List<PromptContributor> getPromptContributors() {
        return promptContributors;
    }
}
