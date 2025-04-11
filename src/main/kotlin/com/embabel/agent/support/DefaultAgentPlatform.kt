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
package com.embabel.agent.support

//import com.embabel.ScriptEvaluationService
import com.embabel.agent.*
import com.embabel.agent.domain.special.Extractable
import com.embabel.agent.domain.special.ExtractableCompanion
import com.embabel.agent.event.AgentProcessCreationEvent
import com.embabel.agent.event.AgenticEventListener
import com.embabel.agent.primitive.LlmOptions
import com.embabel.agent.spi.GoalRanker
import com.embabel.agent.spi.ToolGroupResolver
import com.embabel.agent.testing.FakeLlmTransformer
import com.embabel.common.ai.model.ModelProvider
import com.embabel.common.textio.template.TemplateRenderer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Service

/**
 * Agent platform properties
 * @param autoRegister whether to auto register beans with
 * @Agentic annotation
 */
@ConfigurationProperties("embabel.agent-platform")
data class DefaultAgentPlatformProperties(
    override val goalConfidenceCutOff: ZeroToOne = 0.6,
    override val autoRegister: Boolean = true,
) : AutoRegisteringAgentPlatformProperties

@Service
class DefaultAgentPlatform(
    private val templateRenderer: TemplateRenderer,
//    private val scriptEvaluationService: ScriptEvaluationService,
    private val llmTransformer: LlmTransformer,
    override val goalRanker: GoalRanker,
    override val toolGroupResolver: ToolGroupResolver,
    private val modelProvider: ModelProvider,
    eventListeners: List<AgenticEventListener>,
    private val processIdGenerator: ProcessIdGenerator,
    override val properties: DefaultAgentPlatformProperties,
) : AgentPlatform {

    private val logger = LoggerFactory.getLogger(DefaultAgentPlatform::class.java)

    private val yom = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private val agents = mutableMapOf<String, Agent>()

    override val name: String = javaClass.name

    override val eventListener: AgenticEventListener = AgenticEventListener.from(
        eventListeners,
    )

    private val platformServices = PlatformServices(
        templateRenderer = templateRenderer,
//        scriptEvaluationService = scriptEvaluationService,
        llmTransformer = llmTransformer,
        agentPlatform = this,
        modelProvider = modelProvider,
        eventListener = eventListener,
    )

    init {
        logger.debug("{}: event listener: {}", name, eventListener)
    }

    override fun agents(): List<Agent> =
        agents.values.toList()

    override fun deploy(agent: Agent): DefaultAgentPlatform {
        logger.info("Deploying agent {}", agent.name)
        agents[agent.name] = agent
        updateDomainTypeActions()
        return this
    }

    override fun deploy(action: Action): DefaultAgentPlatform {
        logger.info("Deploying action {}", action.name)
        val agent = Agent(
            name = action.name,
            description = action.description,
            actions = listOf(action),
            goals = emptySet(),
        )
        return deploy(agent)
    }

    override fun deploy(goal: Goal): AgentPlatform {
        logger.info("Deploying goal {}", goal.name)
        val agent = Agent(
            name = "goal-${goal.name}",
            description = goal.description,
            actions = emptyList(),
            goals = setOf(goal),
        )
        return deploy(agent)
    }

    fun deploy(path: String): DefaultAgentPlatform {
        val resource = PathMatchingResourcePatternResolver().getResource(path)
        return deploy(resource)
    }

    fun deploy(resource: Resource): DefaultAgentPlatform {
        logger.info("Loading agent from {}", resource)
        val agent = yom.readValue<Agent>(resource.inputStream, Agent::class.java)
        return deploy(agent)
    }

    private fun updateDomainTypeActions() {
        updateDomainTypeExtractionActions()
    }

    private fun updateDomainTypeExtractionActions() {
        val extractables = domainTypes
            .filter { Extractable::class.java.isAssignableFrom(it) }
            .map { it as Class<out Extractable> }
        logger.debug(
            "Registering extraction actions for {} types: {}",
            extractables.size,
            extractables.map { it.simpleName },
        )
        extractables
            .forEach { type ->
                val companion = type.getDeclaredField("Companion").get(null) as ExtractableCompanion
                val extractionAction = companion.extractionAction(LlmOptions())
                if (!actions.any { it.name == extractionAction.name }) {
                    logger.info("Registering extraction action {}", extractionAction.name)
                    deploy(extractionAction)
                } else {
                    logger.debug("Extraction action {} already registered", extractionAction.name)
                }
            }
    }

    /**
     * Deploy all agents from the given path
     */
    fun deployAgents(agentPath: String = "classpath:agents/*.yml"): DefaultAgentPlatform {
        val resolver: ResourcePatternResolver = PathMatchingResourcePatternResolver()
        val resources = resolver.getResources(agentPath)
        resources.map { resource ->
            deploy(resource)
        }
        return this
    }

    private fun createBlackboard(): Blackboard {
        return InMemoryBlackboard()
    }

    override fun runAgentFrom(
        agent: Agent,
        processOptions: ProcessOptions,
        bindings: Map<String, Any>,
    ): AgentProcessStatus {
        val blackboard = createBlackboard()
        blackboard.bindAll(bindings)
        val agentProcess = createAgentProcess(agent, processOptions, blackboard)
        return agentProcess.run()
    }

    private fun createAgentProcess(
        agent: Agent,
        processOptions: ProcessOptions,
        blackboard: Blackboard,
    ): AgentProcess {
        val platformServicesToUse = if (processOptions.test) {
            platformServices.copy(llmTransformer = FakeLlmTransformer.LoremIpsum)
        } else {
            platformServices
        }

        val agentProcess = SimpleAgentProcess(
            agent = agent,
            platformServices = platformServicesToUse,
            blackboard = blackboard,
            id = processIdGenerator.createProcessId(agent, processOptions),
            parentId = null,
            processOptions = processOptions,
        )
        logger.debug("🚀 Creating process {}", agentProcess.id)
        eventListener.onProcessEvent(AgentProcessCreationEvent(agentProcess))
        return agentProcess
    }

    override fun createChildProcess(
        agent: Agent,
        parentAgentProcess: AgentProcess
    ): AgentProcess {
        val childBlackboard = parentAgentProcess.processContext.blackboard.spawn()
        val processOptions = parentAgentProcess.processContext.processOptions
        val childAgentProcess = SimpleAgentProcess(
            agent = agent,
            platformServices = parentAgentProcess.processContext.platformServices,
            blackboard = childBlackboard,
            id = "${parentAgentProcess.agent.name} >> ${
                processIdGenerator.createProcessId(
                    agent,
                    processOptions,
                )
            }",
            parentId = parentAgentProcess.id,
            processOptions = processOptions,
        )
        logger.debug("👶 Creating child process {} from {}", childAgentProcess.id, parentAgentProcess.id)
        eventListener.onProcessEvent(AgentProcessCreationEvent(childAgentProcess))
        return childAgentProcess
    }
}
