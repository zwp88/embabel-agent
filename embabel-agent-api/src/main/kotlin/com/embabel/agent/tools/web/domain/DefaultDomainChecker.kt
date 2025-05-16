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
package com.embabel.agent.tools.web.domain


import com.embabel.common.util.loggerFor
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.net.Socket

interface DomainChecker {
    fun isDomainAvailable(domain: String): Boolean
}

@Service
internal class DefaultDomainChecker : DomainChecker {

    private val logger = loggerFor<DefaultDomainChecker>()

    @Tool(description = "Check if a domain is available")
    override fun isDomainAvailable(
        @ToolParam(description = "the domain name, such as thing.com") domain: String,
    ): Boolean =
        isDomainAvailableByDns(domain) && isDomainAvailableByWhois(domain)


    /**
     * Check domain availability using DNS lookup approach
     * Returns true if domain seems to be available (not registered)
     */
    private fun isDomainAvailableByDns(domain: String): Boolean {
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
    private fun isDomainAvailableByWhois(domain: String): Boolean {
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
