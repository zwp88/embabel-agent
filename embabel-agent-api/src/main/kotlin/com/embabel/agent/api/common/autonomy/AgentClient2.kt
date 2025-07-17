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
import org.springframework.stereotype.Controller
import java.util.concurrent.CompletableFuture

interface AgentClient2 {

    fun input(obj: Any, vararg objs: Any): RunnableAgentClient

    fun input(inputMapping: Map<String, *>): RunnableAgentClient


    interface RunnableAgentClient {

        fun <T> run(resultType: Class<T>): T

        fun <T> runAsync(resultType: Class<T>): CompletableFuture<T>

        fun startProcess(): AgentProcess

        fun startProcessAsync(): CompletableFuture<AgentProcess>

        fun options(processOptions: ProcessOptions): RunnableAgentClient

        fun options(): ProcessOptionsSpec

    }

    interface ProcessOptionsSpec {

        fun contextId(contextId: ContextId): RunnableAgentClient

        fun blackboard(blackboard: Blackboard): RunnableAgentClient

        fun test(test: Boolean): RunnableAgentClient

        fun verbosity(): VerbositySpec

        fun verbosity(verbosity: Verbosity): RunnableAgentClient

        fun allowGoalChange(allowGoalChange: Boolean): RunnableAgentClient

        fun budget(): BudgetSpec

        fun budget(budget: Budget): RunnableAgentClient

        fun control(processControl: ProcessControl): RunnableAgentClient
    }

    interface VerbositySpec {
        fun showPrompts(showPrompts: Boolean): RunnableAgentClient

        fun showLlmResponses(showLlmResponses: Boolean): RunnableAgentClient

        fun debug(debug: Boolean): RunnableAgentClient

        fun showPlanning(showPlanning: Boolean): RunnableAgentClient
    }

    interface BudgetSpec {
        fun cost(cost: Double): RunnableAgentClient

        fun actions(actions: Int): RunnableAgentClient

        fun tokens(tokens: Int): RunnableAgentClient
    }



    companion object {

        fun of(agentPlatform: AgentPlatform): AgentClient2 {
            TODO()
        }
    }

}

inline fun <reified T : Any> AgentClient2.RunnableAgentClient.run(): T =
    TODO()

inline fun <reified T: Any> AgentClient2.RunnableAgentClient.runAsync(): CompletableFuture<T> =
    TODO()




@Controller
class UsageController(val agentPlatform: AgentPlatform) {

    val agentClient: AgentClient2 = AgentClient2.of(agentPlatform)

    fun planJourney() {
        val travelBrief = JourneyTravelBrief()
        val travelers = Travelers()

        // basic Kotlin
        val travelPlanKotlin: TravelPlan = agentClient
            .input(travelBrief, travelers) // varargs
            .run()

        // basic Java
        val travelPlanJava: TravelPlan = agentClient
            .input(travelBrief, travelers) // varargs
            .run(TravelPlan::class.java)

        // options with ProcessOptions and named params, for use in Kotlin
        val travelPlanOptionsKotlin: TravelPlan = agentClient
            .input(travelBrief, travelers) // varargs
            .options(processOptions = ProcessOptions(
                verbosity = Verbosity(
                    showPrompts = true,
                    showLlmResponses = true,
                ),
                budget = Budget(
                    tokens = Budget.DEFAULT_TOKEN_LIMIT * 3,
                )
            ))
            .run();

        // options with builder, for use in Kotlin/Java
        val travelPlanOptionsJava: TravelPlan = agentClient
            .input(travelBrief, travelers) // varargs
            .options().verbosity().showPrompts(true)
            .options().verbosity().showLlmResponses(true)
            .options().budget().tokens(Budget.DEFAULT_TOKEN_LIMIT * 3)
            .run();

        // async, with map input
        val asyncTravelPlan: CompletableFuture<TravelPlan> = agentClient
            .input(mapOf("id" to travelBrief, "travelers" to travelers)) // map
            .runAsync() // or runAsync(TravelPlan.class) in Java

        // process
        val agentProcess: AgentProcess = agentClient
            .input(travelBrief, travelers)
            .startProcess()
    }
}

class JourneyTravelBrief
class Travelers
class TravelPlan