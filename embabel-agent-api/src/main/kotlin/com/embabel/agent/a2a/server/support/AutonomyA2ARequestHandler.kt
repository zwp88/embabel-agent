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
package com.embabel.agent.a2a.server.support

import com.embabel.agent.a2a.server.A2ARequestEvent
import com.embabel.agent.a2a.server.A2ARequestHandler
import com.embabel.agent.a2a.server.A2AResponseEvent
import com.embabel.agent.a2a.spec.*
import com.embabel.agent.a2a.spec.A2AErrorCodes.TASK_NOT_FOUND
import com.embabel.agent.api.common.autonomy.Autonomy
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.event.AgenticEventListener
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.*

/**
 * Handle A2A messages according to the A2A protocol.
 * Doesn't dictate mapping to URLs: a router or controller
 * in front of this class must handle that.
 */
@Service
@Profile("a2a")
class AutonomyA2ARequestHandler(
    private val autonomy: Autonomy,
    private val agenticEventListener: AgenticEventListener,
    private val objectMapper: ObjectMapper,
) : A2ARequestHandler {

    private val logger = LoggerFactory.getLogger(A2ARequestHandler::class.java)

    override fun handleJsonRpc(
        request: JSONRPCRequest
    ): JSONRPCResponse {
        logger.info("Received JSONRPC message {}: {}", request.method, request)
        agenticEventListener.onPlatformEvent(
            A2ARequestEvent(
                agentPlatform = autonomy.agentPlatform,
                request = request,
            )
        )
        val result = when (request.method) {
            "message/send" -> {
                val messageSendParams = objectMapper.convertValue(request.params, MessageSendParams::class.java)
                handleMessageSend(request, messageSendParams)
            }

            "message/stream" -> {
                TODO("Streaming is not supported")
            }

            "message/list" -> {
                TODO("Listing messages is not supported")
            }

            "message/pending" -> {
                TODO("pending messages is not supported")
            }

            "conversation/list" -> {
                TODO("Listing conversations is not supported")
            }

            "task/list" -> {
                val tqp = objectMapper.convertValue(request.params, TaskQueryParams::class.java)
                handleTasksGet(
                    request, tqp,
                )
            }

            "tasks/cancel" -> {
                val tip = objectMapper.convertValue(request.params, TaskIdParams::class.java)
                handleCancelTask(
                    request, tip,
                )
            }

            else -> {
                logger.warn("Unsupported method: {}", request.method)
                throw UnsupportedOperationException("Method ${request.method} is not supported")
            }
        }
        agenticEventListener.onPlatformEvent(
            A2AResponseEvent(
                agentPlatform = autonomy.agentPlatform,
                response = result,
            )
        )
        return result
    }

    private fun handleMessageSend(
        request: JSONRPCRequest,
        params: MessageSendParams,
    ): JSONRPCResponse {
        // TODO handle other message parts and handle errors
        val intent = params.message.parts.filterIsInstance<TextPart>().single().text
        logger.info("Handling message send request with intent: '{}'", intent)
        try {
            val dynamicExecutionResult = autonomy.chooseAndRunAgent(
                intent = intent,
                processOptions = ProcessOptions(),
            )
            val task = Task(
                id = params.message.taskId ?: UUID.randomUUID().toString(),
                contextId = params.message.contextId ?: ("ctx_" + UUID.randomUUID().toString()),
                status = TaskStatus(
                    state = TaskState.completed,
                ),
                history = listOf(params.message),
                artifacts = listOf(
                    Artifact(
                        parts = listOf(DataPart(data = mapOf("output" to dynamicExecutionResult.output))),
                    )
                ),
                metadata = null,
            )

            val jSONRPCResponse = request.successResponseWith(result = task)
            logger.info("Handled message send request, response={}", jSONRPCResponse)
            return jSONRPCResponse
        } catch (e: Exception) {
            logger.error("Error handling message send request", e)
            // TODO other kinds of errors
            return JSONRPCErrorResponse(
                id = params.message.taskId ?: UUID.randomUUID().toString(),
                error = JSONRPCError(
                    code = TASK_NOT_FOUND,
                    message = "Internal error: ${e.message}",
                    data = e.stackTraceToString()
                )
            )
        }
    }

    private fun handleTasksGet(
        request: JSONRPCRequest,
        params: TaskQueryParams,
    ): JSONRPCResponse {
        TODO()
    }

    private fun handleCancelTask(
        request: JSONRPCRequest,
        tip: TaskIdParams,
    ): JSONRPCResponse {
        TODO()
    }


}
