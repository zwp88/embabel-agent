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

import com.embabel.agent.api.annotation.*
import com.embabel.agent.api.common.LlmOptions
import com.embabel.agent.api.common.OperationPayload
import com.embabel.agent.api.common.TransformationPayload
import com.embabel.agent.api.common.createObject
import com.embabel.agent.core.Goal
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.hitl.ConfirmationRequest
import com.embabel.agent.domain.special.UserInput
import org.springframework.ai.tool.annotation.Tool

data class Person(val name: String) {

    @Tool
    fun reverse() = name.reversed()

}

@Agentic
class NoMethods

@Agentic
class OneGoalOnly {

    val thing1 = Goal.createInstance(
        name = "thing1",
        description = "Thanks to Dr Seuss",
        type = Person::class.java,
    ).withValue(30.0)
}

@Agentic
class TwoGoalsOnly {

    val thing1 = Goal.createInstance(
        description = "Thanks to Dr Seuss",
        type = Person::class.java,
    )
    val thing2 = Goal.createInstance(
        description = "Thanks again to Dr Seuss",
        type = Person::class.java,
    )
}

@Agentic
class ActionGoal {

    @Action
    @AchievesGoal(description = "Creating a user")
    fun toPerson(userInput: UserInput): Person {
        return Person(userInput.content)
    }

}

@Agentic
class NoConditions {

    // A goal makes it legal
    val g = Goal.createInstance(
        name = "thing1",
        description = "Thanks to Dr Seuss",
        type = Person::class.java,
    ).withValue(30.0)

}

@Agentic
class OneConditionOnly {

    @Condition(cost = .5)
    fun condition1(processContext: ProcessContext): Boolean {
        return true
    }

}

@Agentic
class OneTransformerActionOnly {

    @Action(cost = 500.0)
    fun toPerson(userInput: UserInput): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OneTransformerActionTakingPayloadOnly {

    @Action(cost = 500.0)
    fun toPerson(
        userInput: UserInput,
        payload: TransformationPayload<UserInput, Person>,
    ): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OneTransformerActionTakingOperationPayload {

    @Action(cost = 500.0)
    fun toPerson(
        userInput: UserInput,
        payload: OperationPayload,
    ): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OneTransformerActionReferencingConditionByName {

    @Action(pre = ["condition1"])
    fun toPerson(userInput: UserInput): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OneTransformerActionWithCustomToolGroupOnly {

    @Action(cost = 500.0, toolGroups = ["magic"])
    fun toPerson(userInput: UserInput): Person {
        return Person(userInput.content)
    }

}

data class Task(
    val what: String,
)


@Agent(
    description = "one transformer action only",
)
class AgentWithOneTransformerActionWith2ArgsOnly {

    @Action(cost = 500.0)
    fun toPerson(userInput: UserInput, task: Task): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OneTransformerActionWith2ArgsAndCustomInputBindings {

    @Action
    fun toPerson(
        @RequireNameMatch userInput: UserInput,
        @RequireNameMatch task: Task,
    ): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OneTransformerActionWith2ArgsAndCustomOutputBinding {

    @Action(outputBinding = "person")
    fun toPerson(userInput: UserInput, task: Task): Person {
        return Person(userInput.content)
    }

}

@Agentic
class OnePromptActionOnly(
) {

    val promptRunner = using(
        // Java style usage
        llm = LlmOptions.DEFAULT.withTemperature(1.7).withModel("magical"),
    )

    @Action(cost = 500.0)
    fun toPersonWithPrompt(userInput: UserInput): Person {
        return promptRunner.createObject("Generated prompt for ${userInput.content}")
    }

}

@Agentic
class AwaitableOne(
) {

    @Action(cost = 500.0)
    fun waitForPersonConfirmation(userInput: UserInput): Person {
        return waitFor(
            ConfirmationRequest(
                payload = Person(userInput.content),
                message = "Is this dude the right person?",
            )
        )
    }

}

@Agentic
class Combined {

    val planner = Goal.createInstance(
        description = "Create a person",
        type = Person::class.java,
    ).withValue(30.0)

    // Can reuse this or inject
    val magicalLlm = using(
        // Java style usage
        llm = LlmOptions.DEFAULT.withTemperature(1.7).withModel("magical"),
    )

    @Condition(cost = .5)
    fun condition1(processContext: ProcessContext): Boolean {
        return true
    }

    @Action
    fun toPerson(userInput: UserInput): Person {
        return Person(userInput.content)
    }

    @Action(cost = 500.0)
    fun toPersonWithPrompt(userInput: UserInput): Person {
        return magicalLlm.createObject("Generated prompt for ${userInput.content}")
    }

    @Tool
    fun weatherService(location: String) =
        "The weather in $location is ${listOf("sunny", "raining", "foggy").random()}"


}

@Agentic
class OnePromptActionWithToolOnly(
) {

    @Action(cost = 500.0)
    fun toPersonWithPrompt(userInput: UserInput): Person {
        return usingDefaultLlm createObject
                "Generated prompt for ${userInput.content}"
    }

    @Tool
    fun thing(): String {
        return "foobar"
    }

}

@Agentic
class FromPersonUsesDomainObjectTools {

    @Action
    fun fromPerson(
        person: Person
    ): UserInput {
        return using().createObject("Create a UserInput")
    }
}

@Agentic
class OneTransformerActionWith2Tools {

    @Action
    fun toPerson(
        @RequireNameMatch userInput: UserInput,
        @RequireNameMatch task: Task,
    ): Person {
        return Person(userInput.content)
    }

    @Tool
    fun toolWithoutArg(): String = "foo"

    @Tool
    fun toolWithArg(location: String) = "bar"

}
