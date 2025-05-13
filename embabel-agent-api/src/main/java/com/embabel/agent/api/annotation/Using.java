/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.agent.api.annotation;

import com.embabel.agent.api.common.PromptRunner;
import com.embabel.agent.core.ToolConsumer;
import com.embabel.agent.spi.ToolGroupResolver;
import com.embabel.common.ai.model.BuildableLlmOptions;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.ai.prompt.PromptContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Java syntax sugar for running prompts.
 * We use lower case to create a DSL style.
 */
public class Using implements PromptRunner {

    public static Using DEFAULT_LLM = new Using();

    public static Using llm(@NonNull LlmOptions llmOptions) {
        return new Using(llmOptions);
    }

    private final LlmOptions llmOptions;

    private final Set<String> toolGroups = new HashSet<>();

    private final List<ToolCallback> toolCallbacks = new ArrayList<>();

    private final List<PromptContributor> promptContributors = new ArrayList<>();

    private Boolean generateExamples = null;

    /**
     * Constructor for WithLlm.
     *
     * @param llmOptions the LLM options to use
     */
    private Using(@NonNull LlmOptions llmOptions) {
        this.llmOptions = llmOptions;
    }

    private Using() {
        this(new BuildableLlmOptions());
    }

    public Using withToolGroups(@NonNull List<String> toolGroups) {
        this.toolGroups.addAll(toolGroups);
        return this;
    }

    public Using withToolCallbacks(@NonNull List<ToolCallback> toolCallbacks) {
        this.toolCallbacks.addAll(toolCallbacks);
        return this;
    }

    public Using withPromptContributors(@NonNull List<PromptContributor> promptContributors) {
        this.promptContributors.addAll(promptContributors);
        return this;
    }

    public Using withGenerateExamples(boolean generateExamples) {
        this.generateExamples = generateExamples;
        return this;
    }

    @Override
    public <T> T createObject(@NotNull String prompt, @NotNull Class<T> outputClass) {
        return PromptsKt.using(llmOptions, toolGroups, toolCallbacks, promptContributors, generateExamples)
                .createObject(prompt, outputClass);
    }

    @Override
    public <T> @Nullable T createObjectIfPossible(@NotNull String prompt, @NotNull Class<T> outputClass) {
        return PromptsKt.using(llmOptions, toolGroups, toolCallbacks, promptContributors, generateExamples)
                .createObjectIfPossible(prompt, outputClass);
    }

    @Override
    @NotNull
    public String generateText(@NotNull String prompt) {
        return PromptsKt.using(llmOptions, toolGroups, toolCallbacks, promptContributors, generateExamples).generateText(prompt);
    }

    @Override
    public boolean evaluateCondition(@NotNull String condition, @NotNull String context, double confidenceThreshold) {
        return PromptsKt.using(llmOptions, toolGroups, toolCallbacks, promptContributors, generateExamples).evaluateCondition(condition, context, confidenceThreshold);
    }

    @Override
    @Nullable
    public LlmOptions getLlm() {
        return llmOptions;
    }

    @Override
    public @NotNull Set<String> getToolGroups() {
        return toolGroups;
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

    @Override
    @NotNull
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public @NotNull List<ToolCallback> resolveToolCallbacks(@NotNull ToolGroupResolver toolGroupResolver) {
        return ToolConsumer.Companion.resolveToolCallbacks(this, toolGroupResolver);
    }

    @Override
    public Boolean getGenerateExamples() {
        return generateExamples;
    }
}
