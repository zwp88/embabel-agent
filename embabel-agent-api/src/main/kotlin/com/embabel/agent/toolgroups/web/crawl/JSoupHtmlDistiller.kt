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
package com.embabel.agent.toolgroups.web.crawl

import jinjava.org.jsoup.Jsoup
import jinjava.org.jsoup.nodes.Document
import jinjava.org.jsoup.nodes.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class JSoupHtmlDistiller : HtmlDistiller {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun distill(html: String): String {
        val doc = Jsoup.parse(html)

        // Remove noise, find content, then clean it
        val distilled = doc
            .apply { removeNoiseElements() }
            .findMainContent()
            .let { cleanContent(it) }
        logger.info("Distilled content of length ${html.length} to ${distilled.length}")
        return distilled
    }

    private fun Document.removeNoiseElements() {
        // Navigation elements
        select("nav, header, footer, .nav, .header, .footer, .menu").remove()

        // Social media widgets
        select("[class*=social], [id*=social], .share, .sharing").remove()

        // Advertisements
        select("[class*=ad-], [id*=ad-], .advertisement, .banner").remove()

        // Comments sections
        select("[class*=comment], [id*=comment], .comments, #comments").remove()

        // Sidebars
        select("aside, .sidebar, [class*=sidebar], [id*=sidebar]").remove()

        // Remove empty paragraphs and divs
        select("p:empty, div:empty").remove()

        // Remove scripts, styles, and other non-content elements
        select("script, style, noscript, iframe, form").remove()
    }

    private fun Document.findMainContent(): Element {
        // Strategy 1: Look for article tag
        select("article").firstOrNull()?.let {
            return it
        }

        // Strategy 2: Look for common content container classes
        select(CONTENT_SELECTORS).firstOrNull()?.let {
            return it
        }

        // Strategy 3: Find the largest text block's container
        findLargestTextBlock()?.let {
            return it
        }

        // Fallback: Return the body
        return body()
    }

    private fun Document.findLargestTextBlock(): Element? {
        val textBlocks = select("div, section, main").filter { element ->
            // Count meaningful text content (excluding navigation, ads, etc.)
            val textContent = element.select("p, h1, h2, h3, h4, h5, h6")
                .joinToString(" ") { it.text() }
            textContent.length > 500 // Minimum threshold for main content
        }

        return textBlocks.maxByOrNull { element ->
            // Score based on text length and depth in DOM
            val textLength = element.text().length
            val depth = element.parents().size
            textLength.toDouble() / (depth + 1) // Prefer shallow elements with more text
        }
    }

    private fun cleanContent(element: Element): String {
        // Clone the element to avoid modifying the original
        val cleaned = element.clone()

        // Remove any remaining noise elements that might be nested
        cleaned.select("nav, header, footer, aside, [class*=social], [class*=ad-]").remove()

        // Preserve certain block elements with line breaks
        cleaned.select("p, br, div, h1, h2, h3, h4, h5, h6").forEach {
            it.after("\n")
        }

        // Get text content while preserving some structure
        return cleaned.text()
            .replace(Regex("\\s+"), " ")           // Replace multiple spaces with single space
            .replace(Regex("\\n\\s*\\n+"), "\n\n") // Normalize multiple newlines to double newline
            .replace(Regex("^\\s+|\\s+$"), "")     // Trim whitespace
            .replace(Regex("\\s*\\.\\s*"), ". ")   // Normalize sentence spacing
            .replace(Regex("\\s*,\\s*"), ", ")     // Normalize comma spacing
            .trim()
    }

    companion object {
        private val CONTENT_SELECTORS = """
            .content,
            .post-content,
            .entry-content,
            .article-content,
            .post-body,
            .entry,
            .post,
            .article,
            #content,
            #main-content,
            #post-content,
            main
        """.trimIndent()
    }
}
