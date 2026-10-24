package com.restor.gamereel.platform

import okhttp3.Interceptor
import okhttp3.Response

// image cdns send no cache headers, keep pictures for a month
internal fun cacheImages(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())
    return if (response.header("Content-Type").orEmpty().startsWith("image/"))
        response.newBuilder().header("Cache-Control", "public, max-age=2592000").removeHeader("Pragma").build()
    else response
}
