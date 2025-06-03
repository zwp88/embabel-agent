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
package com.embabel.agent.api.annotation.support

import com.embabel.agent.validation.DefaultAgentValidationManager
import com.embabel.agent.validation.GoapPathToCompletionValidator

/**
 * Factory for creating standard AgentMetadataReader instances.
 *
 * This factory provides a default implementation of AgentMetadataReader with common dependencies:
 * - DefaultActionMethodManager for action method handling
 * - MethodDefinedOperationNameGenerator for operation naming
 * - DefaultAgentValidationManager with GoapPathToCompletionValidator for validation
 *
 * This implementation is suitable for most use cases and provides a consistent way to create
 * AgentMetadataReader instances across the application.
 */
object DefaultAgentMetadataReader {
    fun create(): AgentMetadataReader {
        return AgentMetadataReader(
            actionMethodManager = DefaultActionMethodManager(),
            nameGenerator = MethodDefinedOperationNameGenerator(),
            agentValidationManager = DefaultAgentValidationManager(listOf(GoapPathToCompletionValidator()))
        )
    }
}
