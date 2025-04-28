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
