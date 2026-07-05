package com.bookyapa.app.di

import android.content.Context
import android.webkit.WebSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.bookyapa.app.network.AndroidCookieJar
import com.bookyapa.app.network.CloudflareInterceptor
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideAndroidCookieJar(): AndroidCookieJar = AndroidCookieJar()

    @Provides
    @Singleton
    fun provideNonCloudflareOkHttpClient(
        androidCookieJar: AndroidCookieJar,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .cookieJar(androidCookieJar)
            .build()
    }

    @Provides
    @Singleton
    fun provideHttpClient(
        json: Json,
        @ApplicationContext context: Context,
        androidCookieJar: AndroidCookieJar,
        nonCloudflareClient: OkHttpClient,
    ): HttpClient {
        val userAgent = WebSettings.getDefaultUserAgent(context)

        val cloudflareInterceptor = CloudflareInterceptor(
            context = context,
            cookieJar = androidCookieJar,
            defaultUserAgent = userAgent,
        )

        val okHttpClient = nonCloudflareClient.newBuilder()
            .addInterceptor(cloudflareInterceptor)
            .build()

        return HttpClient(OkHttp) {
            engine {
                preconfigured = okHttpClient
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
            defaultRequest {
                url.protocol = URLProtocol.HTTPS
                header(HttpHeaders.UserAgent, userAgent)
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.5")
                header("Upgrade-Insecure-Requests", "1")
                header("Cache-Control", "max-age=0")
            }
        }
    }
}
