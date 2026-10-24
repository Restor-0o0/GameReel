package com.restor.gamereel.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.io.File
import okhttp3.Cache

private var cacheDir: File? = null

fun initHttpCache(dir: File) {
    cacheDir = dir
}

internal actual fun createHttpClient() = HttpClient(OkHttp) {
    engine {
        config { cacheDir?.let { cache(Cache(File(it, "http"), 64L shl 20)) } }
        addNetworkInterceptor(::cacheImages)
    }
}
