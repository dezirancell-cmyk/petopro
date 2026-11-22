package ir.petropro.app.data

import ir.petropro.app.network.ApiErrors
import ir.petropro.app.network.ApiService
import ir.petropro.app.network.InjuryCreateRequest
import ir.petropro.app.network.InjuryMessageDto
import ir.petropro.app.network.InjuryMessageRequest
import ir.petropro.app.network.InjuryReportDto
import java.io.IOException
import java.util.UUID

sealed class Loaded<out T> {
    data class Data<T>(val value: T) : Loaded<T>()
    /** اینترنت نیست؛ این بخش آنلاین‌محور است و آفلاین وانمود نمی‌شود */
    data object Offline : Loaded<Nothing>()
    data class Failure(val message: String) : Loaded<Nothing>()
}

data class InjuryConversation(val report: InjuryReportDto?, val messages: List<InjuryMessageDto>)

/**
 * گزارش آسیب‌دیدگی عمداً آنلاین‌محور است (مربی باید پاسخ بدهد و پیام‌ها در سرور ساخته می‌شوند).
 * نکته: باز کردن گفتگو در سرور «خوانده‌شدن» پیام‌ها را ثبت می‌کند.
 */
class InjuryRepository(private val api: ApiService) {

    suspend fun list(): Loaded<List<InjuryReportDto>> = call {
        val r = api.injuryReports()
        if (r.isSuccessful) Loaded.Data(r.body()?.reports.orEmpty()) else Loaded.Failure(ApiErrors.message(r) ?: "خطا (${r.code()})")
    }

    suspend fun conversation(id: Int): Loaded<InjuryConversation> = call {
        val r = api.injuryMessages(id)
        val b = r.body()
        if (r.isSuccessful && b != null) Loaded.Data(InjuryConversation(b.report, b.messages))
        else Loaded.Failure(ApiErrors.message(r) ?: "خطا (${r.code()})")
    }

    suspend fun create(exercise: String?, description: String): Loaded<Int> = call {
        val r = api.createInjury(UUID.randomUUID().toString(), InjuryCreateRequest(description, exercise?.takeIf { it.isNotBlank() }))
        val id = r.body()?.id
        if (r.isSuccessful && id != null) Loaded.Data(id) else Loaded.Failure(ApiErrors.message(r) ?: "خطا (${r.code()})")
    }

    suspend fun send(reportId: Int, message: String): Loaded<Unit> = call {
        val r = api.sendInjuryMessage(reportId, UUID.randomUUID().toString(), InjuryMessageRequest(message))
        if (r.isSuccessful) Loaded.Data(Unit) else Loaded.Failure(ApiErrors.message(r) ?: "خطا (${r.code()})")
    }

    private suspend fun <T> call(block: suspend () -> Loaded<T>): Loaded<T> =
        try { block() } catch (e: IOException) { Loaded.Offline }
}
