package com.wib.ti.cookiecutter.kafka

import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.KafkaStreamsConfiguration
import java.util.*

@Configuration
@EnableKafka
@EnableKafkaStreams
class KafkaConfigurer(private val kafkaProperties: KafkaProperties) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
    fun config(kafkaProperties: KafkaProperties): KafkaStreamsConfiguration? {
        var props = kafkaProperties.buildStreamsProperties()

        logger.debug("Kafka properties: $props")

        val config: MutableMap<String, Any> = HashMap()

        config[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers
        config[StreamsConfig.APPLICATION_ID_CONFIG] = kafkaProperties.clientId
        config[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass
        config[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass
        config[StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG] =
            LogAndContinueExceptionHandler::class.java
        config[StreamsConfig.STATE_DIR_CONFIG] = System.getProperty("java.io.tmpdir") + "rocksdb " + UUID.randomUUID()

        if (isSSLEnabled(kafkaProperties)) setSSLConfig(config, kafkaProperties)

        return KafkaStreamsConfiguration(config)
    }

    private fun isSSLEnabled(kafkaProperties: KafkaProperties) =
        !kafkaProperties.security.protocol.isNullOrEmpty() && kafkaProperties.ssl.trustStoreLocation != null &&
            kafkaProperties.ssl.trustStoreLocation != null

    private fun setSSLConfig(
        config: MutableMap<String, Any>,
        kafkaProperties: KafkaProperties
    ) {
        // SSL support
        config[StreamsConfig.SECURITY_PROTOCOL_CONFIG] = kafkaProperties.security.protocol
        config[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = kafkaProperties.ssl.trustStoreType
        config[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] =
            kafkaProperties.ssl.trustStoreLocation.file.absolutePath
        config[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = kafkaProperties.ssl.keyStoreLocation.file.absolutePath
        config[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = kafkaProperties.ssl.keyStorePassword
        config[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = kafkaProperties.ssl.keyStoreType
        config[SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG] = ""
    }
}
