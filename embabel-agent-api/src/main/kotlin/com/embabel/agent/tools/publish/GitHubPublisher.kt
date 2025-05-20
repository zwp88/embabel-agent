package com.embabel.agent.tools.publish

import org.kohsuke.github.GHBranch
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import java.util.*

class GitHubPublisher(
    private val githubToken: String,
    private val owner: String,
    private val repo: String,
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