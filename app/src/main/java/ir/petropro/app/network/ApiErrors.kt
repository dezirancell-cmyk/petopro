package ir.petropro.app.network

import kotlinx.serialization.json.Json
import retrofit2.Response

object ApiErrors {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /** بدنه‌ی خطای JSON را (اگر باشد) پارس می‌کند؛ در غیر این صورت null */
    fun parse(raw: String?): ErrorBody? {
        if (raw.isNullOrBlank()) return null
        return try {
            json.decodeFromString(ErrorBody.serializer(), raw)
        } catch (e: Exception) {
            null
        }
    }

    fun parse(resp: Response<*>): ErrorBody? = parse(try { resp.errorBody()?.string() } catch (e: Exception) { null })

    fun message(resp: Response<*>): String? = parse(resp)?.error
}
