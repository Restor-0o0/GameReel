package com.restor.gamereel.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.io.File
import okhttp3.Cache

internal actual fun createHttpClient() = HttpClient(OkHttp) {
    engine {
        config { cache(Cache(File(System.getProperty("java.io.tmpdir"), "gamereel-http"), 64L shl 20)) }
        addNetworkInterceptor(::cacheImages)
    }
}
