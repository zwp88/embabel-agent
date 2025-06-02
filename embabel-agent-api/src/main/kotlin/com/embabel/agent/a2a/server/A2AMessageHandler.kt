package com.embabel.agent.a2a.server

import com.embabel.agent.a2a.spec.*
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
class A2AMessageHandler(
    private val objectMapper: ObjectMapper,
) {

    private val logger = LoggerFactory.getLogger(A2AMessageHandler::class.java)

    fun handleJsonRpc(
        request: JSONRPCRequest
    ): JSONRPCResponse {
        logger.info("Received JSONRPC message {}: {}", request.method, request)
        return when (request.method) {
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
    }

    private fun handleMessageSend(
        request: JSONRPCRequest,
        params: MessageSendParams,
    ): JSONRPCResponse {
        val message = params.message
        val text = "Echo: ${message.kind}"
        val resultMessage = Message(
            role = "agent",
            messageId = UUID.randomUUID().toString(),
            parts = listOf(
                TextPart(text = text),
            ),
        )
        val task = Task(
            id = message.taskId ?: UUID.randomUUID().toString(),
            contextId = message.contextId ?: ("ctx_" + UUID.randomUUID().toString()),
            status = TaskStatus(
                state = TaskState.completed,
                message = resultMessage,
            ),
            history = listOf(message),
            artifacts = listOf(
                Artifact(
                    parts = listOf(TextPart(text = text)),
                )
            ),
            metadata = null,
        )

        val result = request.successResponseWith(result = task)

//        JSONRPCSuccessResponse(id = message.messageId, result = task)
        logger.info("Handled message send request: {}", result)
        return result
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