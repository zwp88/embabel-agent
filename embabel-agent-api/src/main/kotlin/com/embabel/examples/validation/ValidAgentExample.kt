package com.embabel.examples.validation

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent

// Uncomment if you want to test Invalid Agent
//@Agent(description = "A valid agent that demonstrates proper action chaining")
class ValidAgentExample {

    @Action
    fun process(input: InputData): ProcessedData {
        return ProcessedData(
            processedText = "Processed: ${input.text}"
        )
    }

    @Action
    fun validate(processed: ProcessedData): ValidatedData {
        return ValidatedData(
            validatedText = "Validated: ${processed.processedText}"
        )
    }

    @AchievesGoal(description = "Create final result from validated data")
    @Action
    fun complete(validated: ValidatedData): FinalResult {
        return FinalResult(
            result = "Final: ${validated.validatedText}"
        )
    }
} 
