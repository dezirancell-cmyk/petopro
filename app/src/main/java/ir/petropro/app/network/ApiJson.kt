@file:OptIn(ExperimentalSerializationApi::class)

package ir.petropro.app.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/** تنظیمات یکسان JSON برای شبکه، Outbox و تست‌ها */
object ApiJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }
}
