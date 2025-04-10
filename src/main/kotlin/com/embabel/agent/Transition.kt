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
package com.embabel.agent

/**
 * Represents a transition from one state to another.
 * @param to The name of the state to transition to.
 * @param condition Name of the condition that must be met for the transition to occur. "true" is a special condition that always passes.
 * Otherwise, must be a well known condition.
 */
data class Transition(
    val to: String,
    val condition: String,
)
