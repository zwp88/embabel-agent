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
package com.embabel.agent.domain.special


/**
 * Tag interface to indicate that an implementing type can be decomposed
 * into its fields.
 * Can be used for input or output.
 */
interface Megazord

/**
 * Tag interface to indicate that an implementing type should be built from the context from its bound fields.
 * Provides a strongly typed way to wait on combined results.
 * Makes a megazord!
 */
interface Aggregation : Megazord
