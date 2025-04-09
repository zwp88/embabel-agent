package com.embabel.agent.toolgroups.web.crawl

/**
 * Distill web content
 */
interface HtmlDistiller {
    fun distill(content: String): String
}