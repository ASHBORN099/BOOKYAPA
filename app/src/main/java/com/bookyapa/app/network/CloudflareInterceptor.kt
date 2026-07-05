package com.bookyapa.app.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "CFInterceptor"

private val CF_ERROR_CODES = listOf(403, 503)
private val CF_SERVERS = arrayOf("cloudflare", "cloudflare-nginx")
private const val CF_CLEARANCE_COOKIE = "cf_clearance"

class CloudflareInterceptor(
    private val context: Context,
    private val cookieJar: AndroidCookieJar,
    private val defaultUserAgent: String,
) : Interceptor {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!isCloudflareChallenge(response)) {
            return response
        }

        Log.d(TAG, "Cloudflare challenge detected for ${request.url}")

        val body = response.peekBody(Long.MAX_VALUE).string()
        response.close()

        val hasExistingCookie = cookieJar.get(request.url)
            .firstOrNull { it.name == CF_CLEARANCE_COOKIE } != null

        if (hasExistingCookie) {
            Log.d(TAG, "Existing cf_clearance cookie found, retrying without WebView")
            val retryResponse = chain.proceed(request)
            if (!isCloudflareChallenge(retryResponse)) {
                return retryResponse
            }
            Log.d(TAG, "Stale cf_clearance still rejected — clearing and falling through")
            retryResponse.close()
            cookieJar.remove(request.url, listOf(CF_CLEARANCE_COOKIE))
        }

        val isTurnstile = detectTurnstile(body)

        if (isTurnstile) {
            Log.d(TAG, "Turnstile detected — throwing for Activity resolution")
            throw IOException(
                "Cloudflare Turnstile requires verification",
                TurnstileBypassException(request.url.toString())
            )
        }

        Log.d(TAG, "IUAM challenge — attempting background WebView bypass")
        resolveWithBackgroundWebView(request)

        return chain.proceed(request)
    }

    private fun isCloudflareChallenge(response: Response): Boolean {
        if (response.code !in CF_ERROR_CODES) return false
        val server = response.header("Server")?.lowercase() ?: return false
        if (CF_SERVERS.none { server.contains(it) }) return false

        return try {
            val body = response.peekBody(Long.MAX_VALUE).string()
            val doc = Jsoup.parse(body, response.request.url.toString())
            doc.getElementById("challenge-error-title") != null ||
                doc.getElementById("challenge-error-text") != null ||
                doc.getElementById("challenge-running") != null
        } catch (_: Exception) {
            true
        }
    }

    private fun detectTurnstile(body: String): Boolean {
        val hasTurnstile = body.contains("challenges.cloudflare.com", ignoreCase = true) ||
            body.contains("cf-turnstile", ignoreCase = true) ||
            body.contains("turnstile", ignoreCase = true)
        Log.d(TAG, "Turnstile detection from body: $hasTurnstile")
        return hasTurnstile
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithBackgroundWebView(originalRequest: Request) {
        val latch = CountDownLatch(1)
        val cloudflareBypassed = AtomicBoolean(false)

        val origUrl = originalRequest.url

        val runnable = Runnable {
            val webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    userAgentString = defaultUserAgent
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val currentCookie = cookieJar.get(origUrl)
                            .firstOrNull { it.name == CF_CLEARANCE_COOKIE }

                        if (currentCookie != null) {
                            Log.d(TAG, "cf_clearance cookie found after IUAM page load")
                            cloudflareBypassed.set(true)
                            latch.countDown()
                            return
                        }

                        latch.countDown()
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        if (request?.isForMainFrame == true) {
                            Log.d(TAG, "IUAM challenge HTTP ${errorResponse?.statusCode}")
                        }
                    }
                }

                loadUrl(origUrl.toString())
            }
        }

        mainHandler.post(runnable)
        latch.await(30, TimeUnit.SECONDS)

        if (!cloudflareBypassed.get()) {
            Log.w(TAG, "IUAM background WebView bypass failed")
        }
    }
}
