package com.wib.ti.cookiecutter.kafka

import com.solution_management.v1.KubernetesControlRequest
import com.solution_management.v1.KubernetesGetRequest
import com.solution_management.v1.KubernetesLogsRequest
import com.solution_management.v1.KubectlOutputFormat
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

        val valueSerde = protobufSerde(KubernetesControlRequest.parser())

        builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), Serdes.ByteArray()))
            .mapValues { bytes -> KubernetesControlRequest.parseFrom(bytes) }
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

    private fun makeGetRequest(type: String, name: String): KubernetesControlRequest {
        return KubernetesControlRequest.newBuilder()
            .setGet(
                KubernetesGetRequest.newBuilder()
                    .setType(type)
                    .setName(name)
                    .setOutputFormat(KubectlOutputFormat.OUTPUT_FORMAT_JSON)
                    .build()
            )
            .build()
    }

    private fun makeLogsRequest(resource: String, container: String, tailLines: Long): KubernetesControlRequest {
        return KubernetesControlRequest.newBuilder()
            .setLogs(
                KubernetesLogsRequest.newBuilder()
                    .setResource(resource)
                    .setContainer(container)
                    .setTailLines(tailLines)
                    .build()
            )
            .build()
    }

    @Test
    fun `first event passes through`() {
        val request = makeGetRequest("Pod", "my-pod")
        inputTopic.pipeInput("key1", request.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(1, results.size)
        assertEquals(request, KubernetesControlRequest.parseFrom(results[0].value()))
    }

    @Test
    fun `duplicate event is suppressed`() {
        val request = makeGetRequest("Pod", "my-pod")
        inputTopic.pipeInput("key1", request.toByteArray())
        inputTopic.pipeInput("key1", request.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(1, results.size)
    }

    @Test
    fun `different event for same key passes through`() {
        val request1 = makeGetRequest("Pod", "my-pod")
        val request2 = makeGetRequest("Deployment", "my-deploy")
        inputTopic.pipeInput("key1", request1.toByteArray())
        inputTopic.pipeInput("key1", request2.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(2, results.size)
        assertEquals(request1, KubernetesControlRequest.parseFrom(results[0].value()))
        assertEquals(request2, KubernetesControlRequest.parseFrom(results[1].value()))
    }

    @Test
    fun `same event for different keys passes through`() {
        val request = makeGetRequest("Pod", "my-pod")
        inputTopic.pipeInput("key1", request.toByteArray())
        inputTopic.pipeInput("key2", request.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(2, results.size)
    }

    @Test
    fun `updated event replaces stored and new duplicate is suppressed`() {
        val request1 = makeGetRequest("Pod", "my-pod")
        val request2 = makeGetRequest("Deployment", "my-deploy")
        inputTopic.pipeInput("key1", request1.toByteArray())
        inputTopic.pipeInput("key1", request2.toByteArray())
        inputTopic.pipeInput("key1", request2.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(2, results.size)
        assertEquals(request1, KubernetesControlRequest.parseFrom(results[0].value()))
        assertEquals(request2, KubernetesControlRequest.parseFrom(results[1].value()))
    }

    @Test
    fun `different action types are not duplicates`() {
        val getRequest = makeGetRequest("Pod", "my-pod")
        val logsRequest = makeLogsRequest("pod/my-pod", "main", 100)
        inputTopic.pipeInput("key1", getRequest.toByteArray())
        inputTopic.pipeInput("key1", logsRequest.toByteArray())

        val results = outputTopic.readRecordsToList()
        assertEquals(2, results.size)
    }
}
