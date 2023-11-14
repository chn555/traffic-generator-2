package com.wib.ti.cookiecutter

import com.wib.infrastructure.kafka.health.KafkaStreamsHealthIndicator
import com.wib.infrastructure.kafka.health.StreamsExceptionHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
// Scan all components under com.wib, this allows us to include wib.infrastructure
@ConfigurationPropertiesScan
@Import(KafkaStreamsHealthIndicator::class, StreamsExceptionHandler::class)
class CookieCutterApplication

fun main(args: Array<String>) {
    runApplication<CookieCutterApplication>(*args) {}
        .registerShutdownHook()
}

