package com.embabel.agent.dsl

import com.embabel.agent.core.Action
import com.embabel.agent.core.Blackboard
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.primitive.LlmOptions
import com.embabel.agent.event.AgenticEventListener
import org.springframework.ai.tool.ToolCallback

/**
 * Payload for any operation
 * @param processContext the process context
 * @param action the action being executed, if one is executing.
 * This is useful for getting tools etc.
 */
interface OperationPayload : Blackboard {
    val processContext: ProcessContext
    val action: Action?

    fun <O> OperationPayload.createObject(
        llm: LlmOptions = LlmOptions.Companion(),
        prompt: String,
        outputClass: Class<O>,
    ): O {
        return processContext.transform<Unit, O>(
            Unit,
            { prompt },
            // TODO fix callbacks
            llmOptions = llm,
//        toolCallbacks,
            outputClass = outputClass,
            agentProcess = processContext.agentProcess,
            action = this.action,
        )
    }
}

interface InputPayload<I> : OperationPayload {
    val input: I

    fun agentPlatform() = processContext.platformServices.agentPlatform

}

inline fun <reified O> OperationPayload.createObject(
    llm: LlmOptions = LlmOptions.Companion(),
    prompt: String,
): O {
    return createObject(
        prompt = prompt,
        outputClass = O::class.java,
        llm = llm,
    )
}


data class TransformationPayload<I, O>(
    override val input: I,
    override val processContext: ProcessContext,
    override val action: Action?,
    val inputClass: Class<I>,
    val outputClass: Class<O>,
) : InputPayload<I>, Blackboard by processContext.blackboard,
    AgenticEventListener by processContext.platformServices.eventListener {

    /**
     * Simple prompt transformation
     */
    fun <I, O> transform(
        input: I,
        prompt: (input: I) -> String,
        llmOptions: LlmOptions = LlmOptions.Companion(),
        toolCallbacks: List<ToolCallback> = emptyList(),
        outputClass: Class<O>,
    ): O = processContext.transform(
        input, prompt, llmOptions, toolCallbacks, outputClass,
        agentProcess = processContext.agentProcess,
        action = this.action,
    )

    fun <I, O> maybeTransform(
        input: I,
        prompt: (input: I) -> String,
        llmOptions: LlmOptions = LlmOptions.Companion(),
        toolCallbacks: List<ToolCallback> = emptyList(),
        outputClass: Class<O>,
    ): Result<O> = processContext.transformIfPossible(
        input, prompt, llmOptions, toolCallbacks, outputClass,
        agentProcess = processContext.agentProcess,
        action = this.action,
    )
}