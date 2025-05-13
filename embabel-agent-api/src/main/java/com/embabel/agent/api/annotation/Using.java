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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Java syntax sugar for running prompts.
 * We use lower case to create a DSL style.
 */
public record Using(
        LlmOptions llmOptions,
        Set<String> toolGroups,
        List<ToolCallback> toolCallbacks,
        List<PromptContributor> promptContributors,
        Boolean generateExamples
) implements PromptRunner {

    public static final Using DEFAULT_LLM = new Using(
            new BuildableLlmOptions(),
            Collections.emptySet(),
            Collections.emptyList(),
            Collections.emptyList(),
            null
    );

    public static Using llm(@NonNull LlmOptions llmOptions) {
        return new Using(
                llmOptions,
                Collections.emptySet(),
                Collections.emptyList(),
                Collections.emptyList(),
                null
        );
    }

    public Using() {
        this(
                new BuildableLlmOptions(),
                Collections.emptySet(),
                Collections.emptyList(),
                Collections.emptyList(),
                null
        );
    }

    public Using withToolGroups(@NonNull List<String> newToolGroups) {
        Set<String> merged = new HashSet<>(toolGroups == null ? Collections.emptySet() : toolGroups);
        merged.addAll(newToolGroups);
        return new Using(llmOptions, Collections.unmodifiableSet(merged), toolCallbacks, promptContributors, generateExamples);
    }

    public Using withToolCallbacks(@NonNull List<ToolCallback> newToolCallbacks) {
        List<ToolCallback> merged = new ArrayList<>(toolCallbacks == null ? Collections.emptyList() : toolCallbacks);
        merged.addAll(newToolCallbacks);
        return new Using(llmOptions, toolGroups, Collections.unmodifiableList(merged), promptContributors, generateExamples);
    }

    public Using withPromptContributors(@NonNull List<PromptContributor> newPromptContributors) {
        List<PromptContributor> merged = new ArrayList<>(promptContributors == null ? Collections.emptyList() : promptContributors);
        merged.addAll(newPromptContributors);
        return new Using(llmOptions, toolGroups, toolCallbacks, Collections.unmodifiableList(merged), generateExamples);
    }

    public Using withGenerateExamples(boolean generateExamples) {
        return new Using(llmOptions, toolGroups, toolCallbacks, promptContributors, generateExamples);
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
