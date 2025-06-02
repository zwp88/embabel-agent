package com.embabel.examples.validation

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent

// Uncomment if you want to test Invalid Agent
//@Agent(description = "An invalid agent that demonstrates broken action chaining")
class InvalidAgent {

    @Action
    fun process(input: InputData): ProcessedData {
        return ProcessedData(
            processedText = "Processed: ${input.text}"
        )
    }

    @Action
    fun validate(processed: ProcessedData, extraData: ExtraData): ValidatedData {
        // This action requires ExtraData that isn't provided by any previous action
        return ValidatedData(
            validatedText = "Validated: ${processed.processedText} with ${extraData.value}"
        )
    }

    @AchievesGoal(description = "Create final result from validated data")
    @Action
    fun complete(validated: ValidatedData, missingData: MissingData): FinalResult {
        // This action requires MissingData that isn't provided by any previous action
        return FinalResult(
            result = "Final: ${validated.validatedText} and ${missingData.value}"
        )
    }
}

data class InputData(
    val text: String
)

data class ProcessedData(
    val processedText: String
)

data class ExtraData(
    val value: String
)

data class ValidatedData(
    val validatedText: String
)

data class MissingData(
    val value: String
)

data class FinalResult(
    val result: String
)
