package com.bookyapa.app.network

class TurnstileBypassException(val url: String) :
    Exception("Cloudflare Turnstile requires verification in a browser")
