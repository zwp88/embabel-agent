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
package com.embabel.agent.primitive

import com.embabel.agent.*

// TODO Jinja utils
data class SendMessageActionRunner(
    val message: String,
) : ActionRunner {

    override fun referencedInputProperties(variable: String): Set<String> {
        TODO("Not yet implemented")
    }

    override fun execute(
        processContext: ProcessContext,
        outputTypes: Map<String, SchemaType>,
        action: Action,
    ): ActionStatus = ActionRunner.execute {
        TODO("Not yet implemented")
    }
}
