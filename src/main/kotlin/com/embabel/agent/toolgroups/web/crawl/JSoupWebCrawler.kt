package com.embabel.agent.toolgroups.web.crawl

import jinjava.org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

class JSoupWebCrawler(private val maxDepth: Int) : WebCrawler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val visitedUrls: MutableSet<String> = HashSet()

    override fun crawl(url: String, depth: Int, pageHandler: PageHandler) {
        return crawl(url = url, depth = 1, desiredDepth = depth, pageHandler = pageHandler)
    }

    private fun crawl(url: String, depth: Int, desiredDepth: Int, pageHandler: PageHandler) {

        if (depth > desiredDepth.coerceAtMost(maxDepth) || visitedUrls.contains(url)) {
            return
        }

        try {
            logger.info(
                "Extracting content from $url at depth $depth: maxDepth=$maxDepth"
            )
            val document = Jsoup.connect(url).get()
            val text = document.html()
            if (text.indexOf("JavaScript") != -1) {
                logger.info(
                    """
                    |Cannot convert element
                    |With text:
                    |May be unable to spider $url because it contains JavaScript"
                    """.trimMargin()
                )
            }
            pageHandler.handle(JsoupPage(url = url, text = text))
            visitedUrls.add(url)

            val links = document.select("a[href]")
            for (link in links) {
                val nextUrl = link.absUrl("href")
                crawl(nextUrl, depth = depth + 1, desiredDepth = desiredDepth, pageHandler)
            }
        } catch (e: IOException) {
            logger.info("Error while crawling: {}. Continuing...", e.message)
        }
    }

    private data class JsoupPage(override val url: String, override val text: String) : Page
}
