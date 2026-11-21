package ir.petropro.app.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
    @POST("api/mobile/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/mobile/logout")
    suspend fun logout(): Response<ResponseBody>

    /** لغوِ توکنی که هنوز در SessionStore ذخیره نشده (مثلاً ورودِ ردشده) */
    @POST("api/mobile/logout")
    suspend fun logoutWith(@Header("Authorization") authorization: String): Response<ResponseBody>

    @GET("api/mobile/config")
    suspend fun config(): Response<ConfigResponse>

    @GET("api/mobile/sync/manifest")
    suspend fun manifest(): Response<ManifestResponse>

    @GET("api/programs/{id}")
    suspend fun program(@Path("id") id: Int): Response<ProgramResponse>

    @GET("api/mobile/sync/media-manifest")
    suspend fun mediaManifest(): Response<MediaManifestResponse>

    @GET("api/checkin/today")
    suspend fun checkinToday(): Response<CheckinTodayResponse>

    @GET("api/sets/today")
    suspend fun setsForDate(@Query("date") date: String): Response<SetsTodayResponse>

    /** ارسال عملیاتِ Outbox: مسیر و بدنه‌ی JSON از دیتابیس می‌آید؛ کلید یکتا = X-Idempotency-Key */
    @POST
    suspend fun postJson(
        @Url path: String,
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>
}
