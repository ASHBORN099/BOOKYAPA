package com.bookyapa.app

import android.app.Application
import android.os.Looper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BookyapaApp : Application() {

    override fun getPackageName(): String {
        try {
            val stackTrace = Looper.getMainLooper().thread.stackTrace

            // Don't spoof during WebView/CookieManager initialization
            val isWebViewInit = stackTrace.any {
                it.className.startsWith("android.webkit.")
            }
            if (isWebViewInit) return super.getPackageName()

            val isChromiumCall = stackTrace.any {
                it.className.startsWith("org.chromium.") &&
                    it.methodName in setOf("getAll", "getPackageName", "<init>")
            }
            if (isChromiumCall) return SPOOFED_PACKAGE
        } catch (_: Exception) {
        }
        return super.getPackageName()
    }

    companion object {
        private const val SPOOFED_PACKAGE = "com.android.chrome"
    }
}
