package com.restor.gamereel.platform

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout

internal expect fun createHttpClient(): HttpClient

internal val httpClient by lazy {
    createHttpClient().config {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 30_000
        }
    }
}
