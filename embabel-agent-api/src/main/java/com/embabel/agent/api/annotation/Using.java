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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * Java syntax sugar for create a PromptRunner.
 */
public record Using(
        @NonNull LlmOptions llmOptions,
        @NonNull Set<String> toolGroups,
        @NonNull List<ToolCallback> toolCallbacks,
        @NonNull List<Object> toolObjects,
        @NonNull List<PromptContributor> promptContributors,
        Boolean generateExamples
) implements PromptRunner {

    public static final Using DEFAULT_LLM = new Using(
            new BuildableLlmOptions(),
            Collections.emptySet(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            null
    );

    @NonNull
    public static Using llm(@NonNull LlmOptions llmOptions) {
        return new Using(
                llmOptions,
                Collections.emptySet(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null
        );
    }

    @NonNull
    public Using withToolGroups(@NonNull List<String> newToolGroups) {
        Set<String> merged = new HashSet<>(toolGroups);
        merged.addAll(newToolGroups);
        return new Using(llmOptions, Collections.unmodifiableSet(merged), toolCallbacks, toolObjects, promptContributors, generateExamples);
    }

    @NonNull
    public Using withToolGroup(@NonNull String newToolGroup) {
        return withToolGroups(List.of(newToolGroup));
    }

    @NonNull
    public Using withToolCallbacks(@NonNull List<ToolCallback> newToolCallbacks) {
        List<ToolCallback> merged = new ArrayList<>(toolCallbacks);
        merged.addAll(newToolCallbacks);
        return new Using(llmOptions, toolGroups, Collections.unmodifiableList(merged), toolObjects, promptContributors, generateExamples);
    }

    @NonNull
    public Using withToolObjects(@NonNull List<Object> newToolObjects) {
        List<Object> merged = new ArrayList<>(toolObjects);
        merged.addAll(newToolObjects);
        return new Using(llmOptions, toolGroups, toolCallbacks, Collections.unmodifiableList(merged), promptContributors, generateExamples);
    }

    @Override
    @NonNull
    public PromptRunner withToolObject(@NonNull Object toolObject) {
        return withToolObjects(List.of(toolObject));
    }

    @NonNull
    public Using withPromptContributors(@NonNull List<PromptContributor> newPromptContributors) {
        List<PromptContributor> merged = new ArrayList<>(promptContributors);
        merged.addAll(newPromptContributors);
        return new Using(llmOptions, toolGroups, toolCallbacks, toolObjects, Collections.unmodifiableList(merged), generateExamples);
    }

    @NonNull
    public Using withPromptContributor(@NonNull PromptContributor newPromptContributor) {
        return withPromptContributors(List.of(newPromptContributor));
    }

    @NonNull
    public Using withGenerateExamples(boolean generateExamples) {
        return new Using(llmOptions, toolGroups, toolCallbacks, toolObjects, promptContributors, generateExamples);
    }

    @Override
    public <T> T createObject(@NonNull String prompt, @NonNull Class<T> outputClass) {
        return PromptsKt.using(llmOptions, toolGroups, toolCallbacks, toolObjects, promptContributors, generateExamples)
                .createObject(prompt, outputClass);
    }

    @Override
    public <T> @Nullable T createObjectIfPossible(@NonNull String prompt, @NonNull Class<T> outputClass) {
        return PromptsKt.using(llmOptions, toolGroups, toolCallbacks, toolObjects, promptContributors, generateExamples)
                .createObjectIfPossible(prompt, outputClass);
    }

    @Override
    @NonNull
    public String generateText(@NonNull String prompt) {
        return PromptsKt.using(llmOptions, toolGroups, toolCallbacks, toolObjects, promptContributors, generateExamples).generateText(prompt);
    }

    @Override
    public boolean evaluateCondition(@NonNull String condition, @NonNull String context, double confidenceThreshold) {
        return PromptsKt.using(llmOptions, toolGroups, toolCallbacks, toolObjects, promptContributors, generateExamples).evaluateCondition(condition, context, confidenceThreshold);
    }

    @Override
    @Nullable
    public LlmOptions getLlm() {
        return llmOptions;
    }

    @Override
    public @NonNull Set<String> getToolGroups() {
        return toolGroups;
    }

    @Override
    @NonNull
    public List<ToolCallback> getToolCallbacks() {
        return toolCallbacks;
    }

    @Override
    @NonNull
    public List<PromptContributor> getPromptContributors() {
        return promptContributors;
    }

    @Override
    @NonNull
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public @NonNull List<ToolCallback> resolveToolCallbacks(@NonNull ToolGroupResolver toolGroupResolver) {
        return ToolConsumer.Companion.resolveToolCallbacks(this, toolGroupResolver);
    }

    @Override
    @NonNull
    public List<Object> getToolObjects() {
        return toolObjects;
    }

    @Override
    public Boolean getGenerateExamples() {
        return generateExamples;
    }
}
