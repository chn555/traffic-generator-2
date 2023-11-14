package com.wib.ti.cookiecutter.kafka

import com.wib.infrastructure.kafka.serialization.MessageWithCompanyIDProtobufSerde
import com.wib.traffic_inspection.pre_processor.PreProcessor.Call
import org.apache.kafka.streams.StreamsBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CookieCutterPipelineBuilder(
    private val config: CookieCutterConfiguration,
) {

    private val callSerde = MessageWithCompanyIDProtobufSerde<Call>(Call.parser())

    @Autowired
    fun buildPipeline(
        streamsBuilder: StreamsBuilder,
    ) {
//        Kafka Streams topology goes here

//        Processor/transformer suppliers should be autowired to this function
    }
}
