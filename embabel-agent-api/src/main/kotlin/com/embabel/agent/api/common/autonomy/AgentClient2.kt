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
package com.embabel.agent.api.common.autonomy

import com.embabel.agent.core.*
import java.util.concurrent.CompletableFuture

interface AgentClient2 {

    fun input(obj: Any, vararg objs: Any): InvocableAgentClient

    fun input(inputMapping: Map<String, *>): InvocableAgentClient


    interface InvocableAgentClient {

        fun <T> invoke(resultType: Class<T>): T

        fun <T> invokeAsync(resultType: Class<T>): CompletableFuture<T>

    }


    companion object {

        @JvmStatic
        fun createClient(agentPlatform: AgentPlatform): AgentClient2 {
            return builder(agentPlatform).buildClient()
        }

        @JvmStatic
        fun <T> createInvocation(agentPlatform: AgentPlatform, resultType: Class<T>): AgentInvocation<T> {
            return builder(agentPlatform).buildInvocation(resultType)
        }

        inline fun <reified T: Any> createInvocation(agentPlatform: AgentPlatform): AgentInvocation<T> {
            return builder(agentPlatform).buildInvocation()
        }

        @JvmStatic
        fun builder(agentPlatform: AgentPlatform): AgentClient2Builder {
            TODO()
        }

    }

}

inline fun <reified T : Any> AgentClient2.InvocableAgentClient.invoke(): T =
    TODO()

inline fun <reified T: Any> AgentClient2.InvocableAgentClient.invokeAsync(): CompletableFuture<T> =
    TODO()

interface AgentInvocation<T> {

    fun invoke(obj: Any, vararg objs: Any): T

    fun invoke(map: Map<String, *>): T

    fun invokeAsync(obj: Any, vararg objs: Any): CompletableFuture<T>

    fun invokeAsync(map: Map<String, *>): CompletableFuture<T>
}


interface AgentClient2Builder {

    fun options(processOptions: ProcessOptions): AgentClient2Builder

    fun options(): ProcessOptionsBuilder

    fun buildClient(): AgentClient2

    fun <T> buildInvocation(resultType: Class<T>): AgentInvocation<T>

    interface ProcessOptionsBuilder {

        fun contextId(contextId: ContextId): AgentClient2Builder

        fun blackboard(blackboard: Blackboard): AgentClient2Builder

        fun test(test: Boolean): AgentClient2Builder

        fun verbosity(): VerbosityBuilder

        fun verbosity(verbosity: Verbosity): AgentClient2Builder

        fun allowGoalChange(allowGoalChange: Boolean): AgentClient2Builder

        fun budget(): BudgetBuilder

        fun budget(budget: Budget): AgentClient2Builder

        fun control(processControl: ProcessControl): AgentClient2Builder
    }

    interface VerbosityBuilder {
        fun showPrompts(showPrompts: Boolean): AgentClient2Builder

        fun showLlmResponses(showLlmResponses: Boolean): AgentClient2Builder

        fun debug(debug: Boolean): AgentClient2Builder

        fun showPlanning(showPlanning: Boolean): AgentClient2Builder
    }

    interface BudgetBuilder {
        fun cost(cost: Double): AgentClient2Builder

        fun actions(actions: Int): AgentClient2Builder

        fun tokens(tokens: Int): AgentClient2Builder
    }

}

inline fun <reified T : Any> AgentClient2Builder.buildInvocation(): AgentInvocation<T> =
    TODO()
