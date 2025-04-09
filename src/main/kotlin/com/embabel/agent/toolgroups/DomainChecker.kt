package com.embabel.agent.toolgroups


import com.embabel.common.util.kotlin.loggerFor
import com.embabel.agent.toolgroups.web.crawl.ContentSavingPageHandler
import com.embabel.agent.toolgroups.web.crawl.WebCrawler
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.net.Socket

class WebScraperTools(private val webcrawler: WebCrawler) {

    @Tool(description = "Scrape web pages")
    fun scrape(url: String): String {
        loggerFor<WebScraperTools>().info("Scraping URL: {}", url)
        val ph = ContentSavingPageHandler()
        webcrawler.extractData(url, ph)
        return ph.content()
    }

}

@Service
class DomainChecker {

    /**
     * Check domain availability using DNS lookup approach
     * Returns true if domain seems to be available (not registered)
     */
    fun isDomainAvailableByDns(domain: String): Boolean {
        return try {
            // Try to resolve the domain - if it fails, domain might be available
            InetAddress.getByName("$domain.")
            false // Domain exists (resolved successfully)
        } catch (e: Exception) {
            true // Domain likely available (couldn't resolve)
        }
    }

    /**
     * Alternative approach using WHOIS port connection
     */
    fun isDomainAvailableByWhois(domain: String): Boolean {
        val tld = domain.substringAfterLast(".")
        val host = when (tld) {
            "com", "net" -> "whois.verisign-grs.com"
            "org" -> "whois.pir.org"
            "io" -> "whois.nic.io"
            // Add more TLDs as needed
            else -> return false // Unknown TLD
        }

        return try {
            Socket(host, 43).use { socket ->
                // Send query
                val out = socket.getOutputStream()
                out.write("$domain\r\n".toByteArray())
                out.flush()

                // Read response
                val response = socket.getInputStream().bufferedReader().readText()

                // Check if response contains "No match" or similar phrases
                response.contains("No match") ||
                        response.contains("NOT FOUND") ||
                        !response.contains("Domain Name:")
            }
        } catch (e: Exception) {
            false // Error in checking, assume domain is taken
        }
    }
}