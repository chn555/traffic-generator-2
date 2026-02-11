package com.wib.ti.cookiecutter.kafka

import com.google.protobuf.Message
import com.google.protobuf.Parser
import com.solution_management.v1.Error
import kotlinx.serialization.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.streams.StreamsBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CookieCutterPipelineBuilder(
    private val config: CookieCutterConfiguration,
) {

    companion object {
        const val DEDUP_STORE_NAME = "dedup-store"
    }

    @Autowired
    fun buildPipeline(
        streamsBuilder: StreamsBuilder,
    ) {
        val valueSerde = protobufSerde(Error.parser())

        streamsBuilder.stream<String, Error>(config.inputTopic)
            .dedup(
                streamsBuilder = streamsBuilder,
                storeName = DEDUP_STORE_NAME,
                keySerde = Serdes.String(),
                valueSerde = valueSerde,
                isDuplicate = { newValue, storedValue -> newValue == storedValue },
            )
            .to(config.outputTopic)
    }
}

class ProtobufKafkaSerializer<T : Message> : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray {
        return data.toByteArray()
    }
}

class ProtobufKafkaDeserializer<T : Message>(private val parser: Parser<T>) : Deserializer<T> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun deserialize(topic: String?, data: ByteArray?): T {
        try {
            return parser.parseFrom(data)
        } catch (e: Exception) {
            logger.error("Can't deserialize server message data [ ${String(data!!)} ] from topic [ $topic ]", e)
            throw SerializationException(
                "Can't deserialize server message data [ ${String(data)} ] from topic [ $topic ]",
                e
            )
        }
    }
}

fun <T : Message> protobufSerde(parser: Parser<T>): Serde<T> {
    return Serdes.serdeFrom(ProtobufKafkaSerializer(), ProtobufKafkaDeserializer(parser))
}
