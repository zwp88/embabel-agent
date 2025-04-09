package com.embabel.agent

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
// TODO shouldn't do this magically but should bring them in
@ConfigurationPropertiesScan(basePackages = [ "com.embabel.agent", "com.embabel.common.ai.model"])
@ComponentScan (basePackages = [
    "com.embabel.agent",
    "com.embabel.common.ai.model",
"com.embabel.examples",
])
class AgentApplication

fun main(args: Array<String>) {
    runApplication<AgentApplication>(*args)
}

