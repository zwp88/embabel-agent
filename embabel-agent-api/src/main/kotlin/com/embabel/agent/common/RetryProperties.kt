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
    val maxAttempts: Int
    fun retryTemplate(): RetryTemplate
}

/**
 * Extended by configuration for retry
 */
interface RetryProperties : RetryTemplateProvider {
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
