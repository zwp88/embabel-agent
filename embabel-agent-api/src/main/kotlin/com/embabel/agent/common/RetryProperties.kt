package com.embabel.agent.common

import com.embabel.agent.config.models.DockerLocalModels
import com.embabel.common.util.loggerFor
import org.springframework.ai.retry.TransientAiException
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.retry.support.RetryTemplate
import java.time.Duration

interface RetryTemplateProvider {
    fun retryTemplate(): RetryTemplate
}

/**
 * Extended by configuration for retry
 */
interface RetryProperties : RetryTemplateProvider {
    val maxAttempts: Int
    val backoffMillis: Long
    val backoffMultiplier: Double
    val backoffMaxInterval: Long

    override fun retryTemplate(): RetryTemplate {
        return RetryTemplate.builder()
            .maxAttempts(maxAttempts)
            .retryOn(TransientAiException::class.java)
            .exponentialBackoff(
                Duration.ofMillis(backoffMillis),
                backoffMultiplier,
                Duration.ofMillis(backoffMaxInterval)
            )
            .withListener(object : RetryListener {
                override fun <T, E : Throwable> onError(
                    context: RetryContext,
                    callback: RetryCallback<T, E>,
                    throwable: Throwable
                ) {
                    loggerFor<DockerLocalModels>().debug("Retry error. Retry count: ${context.retryCount}", throwable)
                }
            })
            .build()
    }
}