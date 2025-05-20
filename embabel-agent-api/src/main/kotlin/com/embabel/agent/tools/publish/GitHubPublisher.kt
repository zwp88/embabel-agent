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
package com.embabel.agent.tools.publish

import org.kohsuke.github.GHBranch
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import java.util.*

class GitHubPublisher(
    githubToken: String,
    private val owner: String = "embabel",
    private val repo: String = "publications",
    private val commitMessage: String = "Published by Embabel Agent",
) : Publisher {

    private val github = GitHubBuilder().withOAuthToken(githubToken).build()

    override fun publish(publicationRequest: PublicationRequest): PublicationResponse {
        val repo = github.getRepository("$owner/$repo")
        val branch = repo.getBranch("main")
        val publicationResults = publicationRequest.publications.map { request ->
            val pubr = publish(repo, branch, request)
            if (pubr == null) {
                throw IllegalStateException("Failed to publish ${request.fileName}")
            }
            PublishedLocation(pubr)
        }
        return PublicationResponse(publicationResults)
    }

    private fun publish(
        repo: GHRepository,
        branch: GHBranch,
        request: FilePublication
    ): String? {
        return try {
            // Check if file exists
            val existingContent = try {
                repo.getFileContent(request.fileName, branch.name)
            } catch (e: Exception) {
                null
            }

            val encodedContent = Base64.getEncoder().encodeToString(request.content.toByteArray())

            if (existingContent != null) {
                // Update existing file
                repo.createContent()
                    .path(request.fileName)
                    .content(encodedContent)
                    .message(commitMessage)
                    .sha(existingContent.sha)
                    .branch(branch.name)
                    .commit()
                TODO()
            } else {
                // Create new file
                repo.createContent()
                    .path(request.fileName)
                    .content(encodedContent)
                    .message(commitMessage)
                    .branch(branch.name)
                    .commit()
                TODO()
            }

            TODO()
        } catch (e: Exception) {
            println("Error publishing file: ${e.message}")
            e.printStackTrace()
            TODO()
        }
    }

}
