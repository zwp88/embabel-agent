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
