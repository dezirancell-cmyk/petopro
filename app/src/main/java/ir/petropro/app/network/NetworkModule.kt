@file:OptIn(ExperimentalSerializationApi::class)

package ir.petropro.app.network

import ir.petropro.app.core.SessionStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** آدرس سرور هنگام هر درخواست از تنظیمات خوانده می‌شود (هیچ آدرسی ثابت نیست). */
class HostInterceptor(private val store: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val base = store.serverUrl?.toHttpUrlOrNull()
            ?: throw IOException("آدرس سرور تنظیم نشده است")
        val req = chain.request()
        val prefix = base.encodedPath.trimEnd('/')
        val newUrl = req.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .encodedPath(prefix + req.url.encodedPath)
            .build()
        return chain.proceed(req.newBuilder().url(newUrl).build())
    }
}

class AuthInterceptor(private val store: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var req = chain.request()
        val token = store.token
        if (req.header("Authorization") == null && !token.isNullOrEmpty()) {
            req = req.newBuilder().header("Authorization", "Bearer $token").build()
        }
        val resp = chain.proceed(req)
        val isLogin = req.url.encodedPath.endsWith("/api/mobile/login")
        if (resp.code == 401 && !isLogin && req.header("Authorization") == "Bearer $token") {
            store.markExpired()
        }
        return resp
    }
}

class NetworkModule(store: SessionStore) {
    val json: Json = ApiJson.instance

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HostInterceptor(store))
        .addInterceptor(AuthInterceptor(store))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** برای دانلود رسانه (بدون بازنویسی آدرس و بدون هدر احراز هویت) */
    val mediaClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiService::class.java)
}
