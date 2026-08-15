package com.kamsiob.meedwell.core.subsonic

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Tolerant readers for every scalar the Subsonic API hands back.
 *
 * This is the first code written in the project, before any interface, on
 * purpose. Bandcamp's Subsonic support is an open beta and its responses do not
 * match the schema it claims to implement. A strict parser breaks on the first
 * album, and it breaks in the field rather than here.
 *
 * The rule is applied to every numeric field, not only the ones currently known
 * to misbehave. Verification on 15 August 2026 found no floats at all on the
 * test account, which is exactly why the rule stays: three albums prove very
 * little, and the server changes underneath us without warning.
 *
 * What each of these absorbs:
 *   - a number arriving as a JSON string, "185" for 185
 *   - an integer field arriving as a float, 185.0 for 185
 *   - null where the schema promises a value
 *   - the field being absent entirely
 *   - a bare value where an array was expected, and the reverse
 *
 * What it deliberately does not do is guess. An unparseable value becomes the
 * declared default and the record survives; it never becomes a wrong number.
 */

/** Reads an Int from a JSON number, float, or string. Null and nonsense become 0. */
internal object TolerantInt : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TolerantInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeInt()
        return element.asNumberOrNull()?.toInt() ?: 0
    }

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

/** Reads a Long the same way. Durations and file sizes both come through here. */
internal object TolerantLong : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TolerantLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeLong()
        return element.asNumberOrNull()?.toLong() ?: 0L
    }

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}

/**
 * Reads a Boolean from a real boolean, from "true" or "false" as a string, and
 * from 1 or 0 as a number. Anything else is false.
 */
internal object TolerantBoolean : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TolerantBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeBoolean()
        val primitive = element as? JsonPrimitive ?: return false
        if (primitive is JsonNull) return false
        primitive.content.toBooleanStrictOrNull()?.let { return it }
        return primitive.content.toDoubleOrNull()?.let { it != 0.0 } ?: false
    }

    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
}

/**
 * Reads a String from a string, a number, or a boolean. Bandcamp returns album
 * and artist ids as strings ("a:3375168501") but music folder ids as bare
 * numbers, so an id field genuinely arrives as both shapes across endpoints.
 */
internal object TolerantString : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TolerantString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeString()
        val primitive = element as? JsonPrimitive ?: return ""
        if (primitive is JsonNull) return ""
        return primitive.content
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

/**
 * Reads a list that may arrive as a list, as a single bare object, or as null.
 *
 * Subsonic's XML heritage leaks into its JSON: a container with one child is
 * sometimes serialised as that child rather than as a one-element array. An
 * album with a single track is the case that bites, and it bites on real
 * collections rather than on edge cases.
 */
internal class TolerantList<T>(private val itemSerializer: KSerializer<T>) : KSerializer<List<T>> {
    override val descriptor: SerialDescriptor = kotlinx.serialization.builtins.ListSerializer(itemSerializer).descriptor

    override fun deserialize(decoder: Decoder): List<T> {
        val jsonDecoder = decoder as? JsonDecoder ?: return emptyList()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> emptyList()
            is JsonArray -> element.mapNotNull { item ->
                runCatching { jsonDecoder.json.decodeFromJsonElement(itemSerializer, item) }.getOrNull()
            }
            is JsonObject -> listOfNotNull(
                runCatching { jsonDecoder.json.decodeFromJsonElement(itemSerializer, element) }.getOrNull()
            )
            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<T>) {
        kotlinx.serialization.builtins.ListSerializer(itemSerializer).serialize(encoder, value)
    }
}

/**
 * Pulls a number out of a primitive whatever notation it arrived in, and
 * returns null rather than throwing when it is not a number at all.
 */
private fun kotlinx.serialization.json.JsonElement.asNumberOrNull(): Double? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    return primitive.content.toDoubleOrNull()
}
