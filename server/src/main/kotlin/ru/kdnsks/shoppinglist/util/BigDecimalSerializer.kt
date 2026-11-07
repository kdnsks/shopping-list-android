package ru.kdnsks.shoppinglist.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal

/**
 * Денежные суммы и количества передаются строкой ("89.90"), чтобы избежать ошибок
 * округления чисел с плавающей точкой. На вход принимается и строка, и число.
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toPlainString())

    override fun deserialize(decoder: Decoder): BigDecimal {
        val raw = if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            (element as? JsonPrimitive)?.content ?: throw SerializationException("Ожидалось число")
        } else decoder.decodeString()
        return raw.replace(',', '.').trim().toBigDecimalOrNull()
            ?: throw SerializationException("Некорректное число: $raw")
    }
}
