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
package com.embabel.agent.api.common

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Scanning configuration
 * @param annotation whether to auto register beans with
 * @Agent and @Agentic annotation
 * @param publisher whether to auto register agents
 * from AgentPublisher implementations
 * @see com.embabel.agent.api.common.AgentPublisher
 * @see com.embabel.agent.api.annotation.Agent
 */
@ConfigurationProperties("embabel.agent-platform.scanning")
data class AgentScanningProperties(
    val annotation: Boolean = true,
    val publisher: Boolean = false,
)
