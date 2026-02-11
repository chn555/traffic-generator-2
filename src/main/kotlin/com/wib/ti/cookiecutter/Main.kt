package com.wib.ti.cookiecutter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CookieCutterApplication

fun main(args: Array<String>) {
    runApplication<CookieCutterApplication>(*args) {}
        .registerShutdownHook()
}

