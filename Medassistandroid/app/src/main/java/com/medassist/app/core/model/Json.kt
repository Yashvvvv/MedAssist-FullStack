package com.medassist.app.core.model

import java.math.BigDecimal
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive

/*
 * core/ is pure Kotlin (no android.* imports): it could move to a KMP module unchanged.
 */

/** One Json config for the whole app: tolerant of new server fields, omits nulls we send. */
@OptIn(ExperimentalSerializationApi::class)
val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

/**
 * Money and quantities as exact decimals. The server sends `105.00`; a Double would turn
 * that into 104.99999 after arithmetic, and a pharmacy bill must match to the paisa.
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal =
        if (decoder is JsonDecoder) BigDecimal(decoder.decodeJsonElement().jsonPrimitive.content)
        else BigDecimal(decoder.decodeString())

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        if (encoder is JsonEncoder) encoder.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
        else encoder.encodeString(value.toPlainString())
    }
}

typealias Money = @kotlinx.serialization.Serializable(with = BigDecimalSerializer::class) BigDecimal
