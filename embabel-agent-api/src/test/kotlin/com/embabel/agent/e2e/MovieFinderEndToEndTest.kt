package com.embabel.agent.e2e

import com.embabel.agent.api.annotation.support.AgentMetadataReader
import com.embabel.agent.core.Agent
import com.embabel.agent.core.AgentProcessStatusCode
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.testing.createAgentPlatform
import com.embabel.examples.simple.movie.*
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MovieFinderEndToEndTest {

    @Test
    fun `test run process`() {
        val ap = createAgentPlatform()
        val mockOmdbClient = mockk<OmdbClient>()
        val movieBuffRepository = InMemoryMovieBuffRepository()
        val mockStreamingAvailabilityClient = mockk<StreamingAvailabilityClient>()
        val mf = MovieFinder(
            mockOmdbClient, mockStreamingAvailabilityClient, movieBuffRepository,
            MovieFinderConfig()
        )
        val movieAgent = AgentMetadataReader().createAgentMetadata(mf) as Agent
        val result = ap.runAgentWithInput(
            agent = movieAgent,
            input = UserInput("Rod wants to watch a movie with his friends"),
        )
        assertEquals(AgentProcessStatusCode.COMPLETED, result.status, "Unexpected status: ${result.failureInfo}")
    }
}