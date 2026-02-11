package com.wib.ti.cookiecutter.kafka

import com.google.protobuf.Timestamp
import com.solution_management.v1.Error
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Properties

class DeduplicationTransformerTest {

    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, ByteArray>
    private lateinit var outputTopic: TestOutputTopic<String, ByteArray>

    companion object {
        private const val INPUT_TOPIC = "input-topic"
        private const val OUTPUT_TOPIC = "output-topic"
        private const val STORE_NAME = "dedup-store"
    }

    @BeforeEach
    fun setUp() {
        val builder = StreamsBuilder()

        val valueSerde = protobufSerde(Error.parser())

        builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), Serdes.ByteArray()))
            .mapValues { bytes -> Error.parseFrom(bytes) }
            .dedup(
                streamsBuilder = builder,
                storeName = STORE_NAME,
                keySerde = Serdes.String(),
                valueSerde = valueSerde,
                isDuplicate = { newValue, storedValue -> newValue == storedValue },
            )
            .mapValues { msg -> msg.toByteArray() }
            .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.ByteArray()))

        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "dedup-test"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:9092"

        testDriver = TopologyTestDriver(builder.build(), props)

        inputTopic = testDriver.createInputTopic(
            INPUT_TOPIC,
            Serdes.String().serializer(),
            Serdes.ByteArray().serializer(),
        )
        outputTopic = testDriver.createOutputTopic(
            OUTPUT_TOPIC,
            Serdes.String().deserializer(),
            Serdes.ByteArray().deserializer(),
        )
    }

    @AfterEach
    fun tearDown() {
        testDriver.close()
    }

    private fun makeError(message: String, epochSeconds: Long = 0): Error {
        return Error.newBuilder()
            .setMessage(message)
            .setTimestamp(Timestamp.newBuilder().setSeconds(epochSeconds).build())
            .build()
    }

    @Test
    fun `first event passes through`() {
        val error = makeError("pod not found")
        inputTopic.pipeInput("key1", error.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(1, results.size)
        assertEquals(error, Error.parseFrom(results[0].value()))
    }

    @Test
    fun `duplicate event is suppressed`() {
        val error = makeError("pod not found")
        inputTopic.pipeInput("key1", error.toByteArray())
        inputTopic.pipeInput("key1", error.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(1, results.size)
    }

    @Test
    fun `different event for same key passes through`() {
        val error1 = makeError("pod not found")
        val error2 = makeError("deployment failed")
        inputTopic.pipeInput("key1", error1.toByteArray())
        inputTopic.pipeInput("key1", error2.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(2, results.size)
        assertEquals(error1, Error.parseFrom(results[0].value()))
        assertEquals(error2, Error.parseFrom(results[1].value()))
    }

    @Test
    fun `same event for different keys passes through`() {
        val error = makeError("pod not found")
        inputTopic.pipeInput("key1", error.toByteArray())
        inputTopic.pipeInput("key2", error.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(2, results.size)
    }

    @Test
    fun `updated event replaces stored and new duplicate is suppressed`() {
        val error1 = makeError("pod not found")
        val error2 = makeError("deployment failed")
        inputTopic.pipeInput("key1", error1.toByteArray())
        inputTopic.pipeInput("key1", error2.toByteArray())
        inputTopic.pipeInput("key1", error2.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(2, results.size)
        assertEquals(error1, Error.parseFrom(results[0].value()))
        assertEquals(error2, Error.parseFrom(results[1].value()))
    }

    @Test
    fun `different timestamps make events non-duplicate`() {
        val error1 = makeError("pod not found", epochSeconds = 1000)
        val error2 = makeError("pod not found", epochSeconds = 2000)
        inputTopic.pipeInput("key1", error1.toByteArray())
        inputTopic.pipeInput("key1", error2.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(2, results.size)
    }
}
