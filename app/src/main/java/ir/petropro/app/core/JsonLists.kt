package ir.petropro.app.core

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object JsonLists {
    private val serializer = ListSerializer(String.serializer())

    fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            Json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
