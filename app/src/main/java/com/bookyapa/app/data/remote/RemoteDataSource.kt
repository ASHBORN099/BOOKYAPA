package com.bookyapa.app.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RemoteDataSource"
private val BLOCK_PATTERNS = listOf(
    "Just a moment",
    "cf-browser-verification",
    "captcha",
    "accessdenied",
    "Please verify you are a human",
)

@Singleton
class RemoteDataSource @Inject constructor(
    private val httpClient: HttpClient,
) {

    suspend fun fetchHtml(url: String): Result<String> {
        Log.d(TAG, "Fetching url=$url")
        val result = runCatching {
            val response = httpClient.get(url)
            val statusCode = response.status.value
            val html = response.bodyAsText()
            val preview = html.take(500)
            Log.d(TAG, "Fetched url=$url (status=$statusCode) -> ${html.length} chars")
            Log.d(TAG, "Preview: $preview")

            if (statusCode == 403) {
                throw HttpForbiddenException(url)
            }

            val lower = html.lowercase()
            val match = BLOCK_PATTERNS.firstOrNull { it in lower }
            if (match != null) {
                Log.w(TAG, "Block page detected (status=$statusCode, matched: $match)")
                throw Exception("Access blocked by $match - this source may only work on a real device")
            }
            if ("page not found" in lower && html.length < 50_000) {
                Log.w(TAG, "Small 'Page Not Found' response (${html.length} bytes) - possible IP block")
                throw Exception("Source returned a 'Page Not Found' page (${html.length} bytes) - the server may be blocking this request")
            }
            if ("404 | project gutenberg" in lower) {
                Log.w(TAG, "Gutenberg 404 page detected (status=$statusCode)")
                throw Exception("Project Gutenberg blocked the request (common on emulators) - try on a real device")
            }
            html
        }

        if (result.isSuccess) return result

        val ex = result.exceptionOrNull()
        if (ex is HttpForbiddenException) {
            Log.w(TAG, "Got 403 for $url — Cloudflare interceptor will handle it")
            return Result.failure(ex)
        }

        Log.w(TAG, "fetchHtml failed url=$url error=${ex?.message}")
        return result
    }

    suspend fun fetchHtmlPost(url: String, body: String): Result<String> {
        Log.d(TAG, "Fetching url=$url body=$body")
        return runCatching {
            val response = httpClient.post(url) {
                setBody(body)
                contentType(ContentType.Application.FormUrlEncoded)
            }
            val statusCode = response.status.value
            val html = response.bodyAsText()
            Log.d(TAG, "Fetched POST url=$url (status=$statusCode) -> ${html.length} chars")
            html
        }.onFailure { e ->
            Log.w(TAG, "fetchHtmlPost failed url=$url error=${e.message}")
        }
    }
}

private class HttpForbiddenException(val url: String) :
    Exception("Source returned 403 Forbidden - the website is blocking the request (try on a real device)")
