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
package com.embabel.agent.api.common.autonomy;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.Budget;

import java.util.Map;

public class AgentClient2JavaTest {

    private final AgentClient2 agentClient;

    private final AgentInvocation<TravelPlan> invocation;

    public AgentClient2JavaTest(AgentPlatform agentPlatform) {
        this.agentClient = AgentClient2.createClient(agentPlatform);
//        custom client using builder
//        this.agentClient = AgentClient2.builder(agentPlatform)
//                .options().verbosity().showPrompts(true)
//                .options().verbosity().showLlmResponses(true)
//                .options().budget().tokens(Budget.DEFAULT_TOKEN_LIMIT * 3)
//                .buildClient();

        this.invocation = AgentClient2.createInvocation(agentPlatform, TravelPlan.class);
    }

    public void inputVarargs() {
        var travelers = new Travelers();
        var travelBrief = new JourneyTravelBrief();
        TravelPlan travelPlan = this.agentClient
                .input(travelBrief, travelers)
                .invoke(TravelPlan.class);
    }

    public void inputMap() {
        var travelers = new Travelers();
        var travelBrief = new JourneyTravelBrief();
        TravelPlan travelPlan = this.agentClient
                .input(Map.of("id", travelBrief, "travelers", travelers))
                .invoke(TravelPlan.class);
    }

    public void invocation() {
        var travelers = new Travelers();
        var travelBrief = new JourneyTravelBrief();
        var travelPlan = invocation.invoke(travelBrief, travelers);

    }

}
