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
package com.embabel.agent.api.common.workflow.control;

import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.testing.integration.IntegrationTestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.embabel.agent.testing.integration.IntegrationTestUtils.dummyAgentPlatform;
import static org.junit.jupiter.api.Assertions.*;


class ScatterGatherBuilderTest {

    class Birthday {
    }

    record Age(int years) {
    }

    @Test
    void testNoExportedActionsFromWorkflow() {
        var agent = ScatterGatherBuilder
                .returning(Age.class)
                .fromElements(Birthday.class)
                .generatedBy(List.of(
                        () -> new Birthday(),
                        () -> new Birthday()))
                .consolidatedBy(context -> {
                    return new Age(context.getInput().getResults().size());
                })
                .buildAgent("name", "description");
        assertFalse(agent.getActions().isEmpty(), "Should have actions");
        assertTrue(agent.getOpaque(), "Agent should be opaque");

        var ap = dummyAgentPlatform();
        ap.deploy(agent);
        assertTrue(agent.getActions().size() > 1,
                "Should have actions on the agent");
        assertEquals(0, ap.getActions().size());
    }

    @Test
    void testConsensusOfOneSimpleGenerator() {
        var agent = ScatterGatherBuilder
                .returning(Age.class)
                .fromElements(Birthday.class)
                .generatedBy(List.of(
                        () -> new Birthday(),
                        () -> new Birthday()))
                .consolidatedBy(context -> {
                    return new Age(context.getInput().getResults().size());
                })
                .buildAgent("name", "description");
        var ap = IntegrationTestUtils.dummyAgentPlatform();
        var result = ap.runAgentFrom(
                agent,
                ProcessOptions.getDEFAULT(),
                Map.of("it", new UserInput("input"))
        );
        assertEquals(AgentProcessStatusCode.COMPLETED, result.getStatus());
        assertTrue(result.lastResult() instanceof Age);
        var age = (Age) result.lastResult();
        assertEquals(2, age.years(), "Expected consensus age to come from the 2 birthdays");
    }

    @Test
    void testConsensusOfOne() {
        var agent = ScatterGatherBuilder
                .returning(Age.class)
                .fromElements(Birthday.class)
                .generatedBy(
                        List.of(() -> new Birthday()))
                .consolidatedBy(context -> {
                    return new Age(context.getInput().getResults().size());
                })
                .buildAgent("name", "description");
        var ap = IntegrationTestUtils.dummyAgentPlatform();
        var result = ap.runAgentFrom(
                agent,
                ProcessOptions.getDEFAULT(),
                Map.of("it", new UserInput("input"))
        );
        assertEquals(AgentProcessStatusCode.COMPLETED, result.getStatus());
        assertTrue(result.lastResult() instanceof Age);
        var age = (Age) result.lastResult();
        assertEquals(1, age.years(), "Expected consensus age to come from single birthday");
    }


}