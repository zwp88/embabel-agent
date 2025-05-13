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

import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.core.Action
import org.springframework.ai.tool.ToolCallback
import java.lang.reflect.Method

/**
 * Creates and invokes actions from annotated methods.
 */
interface ActionMethodManager {

    /**
     * Create an Action from a method
     * @param method the method to create an action from
     * @param instance instance of Agent or AgentCapabilities-annotated class
     * @param toolCallbacksOnInstance tool callbacks to use from instance level
     */
    fun createAction(
        method: Method,
        instance: Any,
        toolCallbacksOnInstance: List<ToolCallback>,
    ): Action

    /**
     * Invoke the action method on the given instance.
     */
    fun <O> invokeActionMethod(
        method: Method,
        instance: Any,
        context: TransformationActionContext<List<Any>, O>,
    ): O
}
