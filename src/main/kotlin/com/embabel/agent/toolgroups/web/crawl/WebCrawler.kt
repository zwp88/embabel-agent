package com.embabel.agent.toolgroups.web.crawl

import java.util.concurrent.ConcurrentLinkedQueue

interface WebCrawler {

    fun crawl(url: String, depth: Int, pageHandler: PageHandler)

    /**
     * Extract data from the given page
     * @param url
     * @param pageHandler
     */
    fun extractData(url: String, pageHandler: PageHandler) {
        crawl(url = url, depth = 1, pageHandler = pageHandler)
    }
}

interface Page {
    val url: String
    val text: String
}

/**
 * Interface to extract data from a page. Can attempt to extract structured data as
 * well as text to index.
 */
interface PageHandler {

    fun handle(page: Page)
}

class ContentSavingPageHandler(
    private val htmlDistiller: HtmlDistiller = JSoupHtmlDistiller(),
) : PageHandler {
    private val contentPieces = ConcurrentLinkedQueue<String>()

    override fun handle(page: Page) {
        val text = htmlDistiller.distill(page.text)
        contentPieces.add("${page.url}\n${text}")
    }

    fun content(): String {
        return contentPieces.joinToString("\n\n")
    }
}