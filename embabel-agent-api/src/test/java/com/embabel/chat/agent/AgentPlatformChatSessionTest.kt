package com.embabel.chat.agent

import com.embabel.agent.api.common.DynamicExecutionResult
import com.embabel.agent.api.common.chooseAndAccomplishGoal
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.support.LocalPerson
import com.embabel.chat.AssistantMessage
import com.embabel.chat.MessageSavingMessageListener
import com.embabel.chat.UserMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AgentPlatformChatSessionTest {

    @Test
    fun testOne() {
        val mockAgentPlatform = mockk<AgentPlatform>()
        val intent = slot<String>()
        val der = mockk<DynamicExecutionResult>()
        val output = LocalPerson("Gordon")
        every { der.output } returns output
        mockkStatic("com.embabel.agent.api.common.ChooseAndRunKt")

        every { any<AgentPlatform>().chooseAndAccomplishGoal(capture(intent), any()) } returns der
        val chatSession = AgentPlatformChatSession(mockAgentPlatform, {})
        val userMessage = UserMessage("Hello, world!")
        val l = MessageSavingMessageListener()
        chatSession.send(userMessage, l)
        assertEquals(1, l.messages().size)
        assertTrue(l.messages()[0] is AssistantMessage)
        assertTrue(
            l.messages()[0].content.contains(output.name),
            "Expected message to contain the name of the output person: got ${l.messages()[0].content}",
        )
    }

}