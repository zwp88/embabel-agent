package com.embabel.agent.e2e.coding

import com.embabel.examples.dogfood.coding.Coder
import com.embabel.examples.dogfood.coding.CodeModificationRequest
import com.embabel.examples.dogfood.coding.CodingProperties
import com.embabel.examples.dogfood.coding.SoftwareProject
import com.embabel.agent.toolgroups.code.BuildResult
import com.embabel.agent.toolgroups.code.BuildStatus
import com.embabel.agent.api.common.ActionContext
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class FixBrokenBuildTest {
    private val logger = LoggerFactory.getLogger(FixBrokenBuildTest::class.java)

    private val projectRepository = mockk<com.embabel.examples.dogfood.coding.ProjectRepository>()
    private val codingProperties = CodingProperties(
        defaultLocation = "test-project",
        primaryCodingLlm = "test-llm",
        fixCodingLlm = "test-llm-fix"
    )
    private val coder = Coder(projectRepository, codingProperties)
    private val project = mockk<SoftwareProject>(relaxed = true)
    private val context = mockk<ActionContext>(relaxed = true)

    @Nested
    inner class `fixBrokenBuild parses and handles build errors` {
        @Test
        fun `should handle single compilation error`() {
            val buildResult = BuildResult(
                status = BuildStatus(success = false, relevantOutput = "[ERROR] /src/Main.kt: (10, 5) Unresolved reference: foo"),
                output = "[ERROR] /src/Main.kt: (10, 5) Unresolved reference: foo",
                duration = java.time.Duration.ofSeconds(2)
            )
            val request = CodeModificationRequest("Fix compilation error in Main.kt")
            every { context.promptRunner(any(), any()) } returns mockk {
                every { create(any<String>()) } returns "Fixed unresolved reference in Main.kt."
            }
            val report = coder.fixBrokenBuild(request, project, buildResult, context)
            assertTrue(report.text.contains("Fixed unresolved reference"))
        }

        @Test
        fun `should handle multiple errors and warnings`() {
            val buildResult = BuildResult(
                status = BuildStatus(
                    success = false,
                    relevantOutput = """
                        [WARNING] /src/Util.kt: (5, 15) Deprecated API usage
                        [ERROR] /src/Service.kt: (20, 10) Type mismatch: inferred type is String but Int was expected
                        [ERROR] /src/Repo.kt: (30, 1) Unresolved reference: bar
                    """.trimIndent()
                ),
                output = "[WARNING] /src/Util.kt: (5, 15) Deprecated API usage\n[ERROR] /src/Service.kt: (20, 10) Type mismatch: inferred type is String but Int was expected\n[ERROR] /src/Repo.kt: (30, 1) Unresolved reference: bar",
                duration = java.time.Duration.ofSeconds(3)
            )
            val request = CodeModificationRequest("Fix all build errors and warnings")
            every { context.promptRunner(any(), any()) } returns mockk {
                every { create(any<String>()) } returns "Fixed type mismatch and unresolved reference. Deprecated API updated."
            }
            val report = coder.fixBrokenBuild(request, project, buildResult, context)
            assertTrue(report.text.contains("Fixed type mismatch"))
            assertTrue(report.text.contains("Deprecated API updated"))
        }

        @Test
        fun `should handle empty error output`() {
            val buildResult = BuildResult(
                status = BuildStatus(success = false, relevantOutput = ""),
                output = "",
                duration = java.time.Duration.ofMillis(500)
            )
            val request = CodeModificationRequest("Try to fix build with no error output")
            every { context.promptRunner(any(), any()) } returns mockk {
                every { create(any<String>()) } returns "No errors found, nothing to fix."
            }
            val report = coder.fixBrokenBuild(request, project, buildResult, context)
            assertEquals("No errors found, nothing to fix.", report.text)
        }
    }
}
