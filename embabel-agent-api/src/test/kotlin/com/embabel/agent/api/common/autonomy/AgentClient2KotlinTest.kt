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

import com.embabel.agent.api.common.autonomy.AgentClient2.Companion.builder
import com.embabel.agent.api.common.autonomy.AgentClient2.Companion.createClient
import com.embabel.agent.api.common.autonomy.AgentClient2.Companion.createInvocation
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.Budget
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.Verbosity

class AgentClient2KotlinTest(agentPlatform: AgentPlatform) {

    private val agentClient: AgentClient2 = createClient(agentPlatform);

//    custom client using builder
//    private val agentClient: AgentClient2 = builder(agentPlatform)
//        .options(processOptions = ProcessOptions(
//            verbosity = Verbosity(
//                showPrompts = true,
//                showLlmResponses = true,
//            ),
//            budget = Budget(
//                tokens = Budget.DEFAULT_TOKEN_LIMIT * 3,
//            )
//        ))
//        .buildClient()

    private val invocation: AgentInvocation<TravelPlan> = createInvocation(agentPlatform)


    fun inputVarargs() {
        val travelers = Travelers()
        val travelBrief = JourneyTravelBrief()
        val travelPlan = agentClient
            .input(travelBrief, travelers)
            .invoke<TravelPlan>()
    }

    fun inputMap() {
        val travelers = Travelers()
        val travelBrief = JourneyTravelBrief()
        val travelPlan = agentClient
            .input(mapOf("id" to travelBrief, "travelers" to travelers))
            .invoke<TravelPlan>()
    }

    fun invocation() {
        val travelers = Travelers()
        val travelBrief = JourneyTravelBrief()
        val travelPlan = invocation.invoke(travelBrief, travelers)

    }
}

class JourneyTravelBrief
class Travelers
class TravelPlan
