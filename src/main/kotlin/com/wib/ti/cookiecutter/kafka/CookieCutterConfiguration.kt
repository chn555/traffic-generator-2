package com.wib.ti.cookiecutter.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Validated
@ConfigurationProperties(prefix = "cookie-cutter.kafka")
@ConstructorBinding
data class CookieCutterConfiguration(
    @field:NotEmpty
    @field:NotNull
    val inputTopic: String,
    @field:NotEmpty
    @field:NotNull
    val outputTopic: String,


)
