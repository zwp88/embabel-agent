/*
                                * Copyright 2025 Embabel Software, Inc.
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
