package com.restor.gamereel.feature.library

import com.restor.gamereel.platform.httpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.*

internal const val SUBMIT_URL =
    "https://script.google.com/macros/s/AKfycbwqP2oag-7qomvslLRcNPmSXAchXmgEUANsynRhw8yMGoX55XukKVWU-7XbWndw1yDJgA/exec"

internal class SubmissionException(message: String) : Exception(message)

// apps script answers a post with a redirect to the result, which must be fetched with get
internal suspend fun submitPack(packJson: String, note: String): String {
    val body = buildJsonObject {
        put("pack", Json.parseToJsonElement(packJson))
        if (note.isNotBlank()) put("note", note.trim().take(500))
    }.toString()
    var response = httpClient.post(SUBMIT_URL) {
        contentType(ContentType.Text.Plain)
        setBody(body)
    }
    if (response.status.value in 300..399)
        response = httpClient.get(response.headers[HttpHeaders.Location] ?: throw SubmissionException(""))
    if (!response.status.isSuccess()) throw SubmissionException("HTTP ${response.status.value}")
    val result = runCatching { Json.parseToJsonElement(response.bodyAsText()).jsonObject }
        .getOrElse { throw SubmissionException("") }
    if (result["ok"]?.jsonPrimitive?.booleanOrNull != true)
        throw SubmissionException(result["error"]?.jsonPrimitive?.contentOrNull.orEmpty())
    return result["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
}
