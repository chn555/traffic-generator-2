package com.wib.ti.cookiecutter.kafka

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.kstream.TransformerSupplier
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores

class DeduplicationTransformer<K, V>(
    private val storeName: String,
    private val valueSerde: Serde<V>,
    private val isDuplicate: (newValue: V, storedValue: V) -> Boolean,
) : Transformer<K, V, KeyValue<K, V>?> {

    private lateinit var store: KeyValueStore<K, ByteArray>

    @Suppress("UNCHECKED_CAST")
    override fun init(context: ProcessorContext) {
        store = context.getStateStore(storeName) as KeyValueStore<K, ByteArray>
    }

    override fun transform(key: K, value: V): KeyValue<K, V>? {
        val storedBytes = store.get(key)
        if (storedBytes != null) {
            val storedValue = valueSerde.deserializer().deserialize(null, storedBytes)
            if (isDuplicate(value, storedValue)) {
                return null
            }
        }
        store.put(key, valueSerde.serializer().serialize(null, value))
        return KeyValue(key, value)
    }

    override fun close() {
        // no-op
    }

}

fun <K, V> KStream<K, V>.dedup(
    streamsBuilder: StreamsBuilder,
    storeName: String,
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
    isDuplicate: (newValue: V, storedValue: V) -> Boolean,
): KStream<K, V> {
    val storeBuilder = Stores.keyValueStoreBuilder(
        Stores.persistentKeyValueStore(storeName),
        keySerde,
        Serdes.ByteArray(),
    )
    streamsBuilder.addStateStore(storeBuilder)

    return transform(
        TransformerSupplier {
            DeduplicationTransformer(
                storeName = storeName,
                valueSerde = valueSerde,
                isDuplicate = isDuplicate,
            )
        },
        storeName,
    )
}
