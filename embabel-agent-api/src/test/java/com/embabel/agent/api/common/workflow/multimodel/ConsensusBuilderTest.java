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
package com.embabel.agent.api.common.workflow.multimodel;

import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.testing.integration.IntegrationTestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ConsensusBuilderTest {

    record Age(int years) {
    }

    @Test
    void testConsensusOfOneSimpleGenerator() {
        var agent = ConsensusBuilder
                .returning(Age.class)
                .withGenerators(List.of(() -> new Age(42)))
                .withConsensusBy(context -> {
                    return new Age(context.getInput().getResults().stream().findFirst().get().years);
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
        assertEquals(42, age.years(), "Expected consensus age to come from input 42");
    }

    @Test
    void testConsensusOfOne() {
        var agent = ConsensusBuilder
                .returning(Age.class)
                .withGeneratorTransforms(List.of(tac -> new Age(42)))
                .withConsensusBy(context -> {
                    return new Age(context.getInput().getResults().stream().findFirst().get().years);
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
        assertEquals(42, age.years(), "Expected consensus age to come from input 42");
    }

    @Test
    void testConsensusOfTwo() {
        var agent = ConsensusBuilder
                .returning(Age.class)
                .withGeneratorTransforms(List.of(
                        tac -> new Age(42),
                        tac -> new Age(44)
                ))
                .withConsensusBy(context ->
                        new Age(
                                context.getInput().getResults().stream()
                                        .mapToInt(a -> ((Age) a).years)
                                        .sum() / context.getInput().getResults().size()
                        )
                )
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
        assertEquals(43, age.years(), "Expected consensus age to come from input 43");
    }

}