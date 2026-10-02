package com.ludoproof.game

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class GameApi(
    baseUrl: String = BuildConfig.LUDOPROOF_API_BASE_URL,
) {
    private val baseUrl = baseUrl.trimEnd('/')

    fun createMatch(
        displayName: String,
        clientRequestId: String,
        profileId: String? = null,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches",
            body = JSONObject()
                .put("displayName", displayName)
                .put(
                    "clientRequestId",
                    clientRequestId,
                )
                .apply {
                    if (
                        !profileId
                            .isNullOrBlank()
                    ) {
                        put(
                            "profileId",
                            profileId,
                        )
                    }
                },
        )

    fun joinMatch(
        matchId: String,
        displayName: String,
        clientRequestId: String,
        profileId: String? = null,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches/${matchId.uppercase()}/join",
            body = JSONObject()
                .put("displayName", displayName)
                .put(
                    "clientRequestId",
                    clientRequestId,
                )
                .apply {
                    if (
                        !profileId
                            .isNullOrBlank()
                    ) {
                        put(
                            "profileId",
                            profileId,
                        )
                    }
                },
        )

    fun state(
        matchId: String,
        playerToken: String,
    ): JSONObject =
        request(
            method = "GET",
            path = "/api/matches/${matchId.uppercase()}/state",
            playerToken = playerToken,
        )

    fun start(
        matchId: String,
        playerToken: String,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches/${matchId.uppercase()}/start",
            playerToken = playerToken,
            body = JSONObject(),
        )

    fun commitRoll(
        matchId: String,
        playerToken: String,
        clientCommitment: String,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches/${matchId.uppercase()}/roll/commit",
            playerToken = playerToken,
            body = JSONObject()
                .put("clientCommitment", clientCommitment),
        )

    fun revealRoll(
        matchId: String,
        playerToken: String,
        clientSeed: String,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches/${matchId.uppercase()}/roll/reveal",
            playerToken = playerToken,
            body = JSONObject()
                .put("clientSeed", clientSeed),
        )

    fun move(
        matchId: String,
        playerToken: String,
        tokenIndex: Int,
        eventIndex: Int,
    ): JSONObject =
        request(
            method = "POST",
            path = "/api/matches/${matchId.uppercase()}/move",
            playerToken = playerToken,
            body = JSONObject()
                .put("tokenIndex", tokenIndex)
                .put(
                    "eventIndex",
                    eventIndex,
                ),
        )

    private fun request(
        method: String,
        path: String,
        playerToken: String? = null,
        body: JSONObject? = null,
    ): JSONObject {
        require(baseUrl.startsWith("https://")) {
            "LudoProof API must use HTTPS"
        }

        val connection =
            URL(baseUrl + path)
                .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 25_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty(
                "Accept",
                "application/json",
            )
            connection.setRequestProperty(
                "User-Agent",
                "LudoProof-Android/" + BuildConfig.VERSION_NAME,
            )

            if (playerToken != null) {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $playerToken",
                )
            }

            if (body != null) {
                val bytes =
                    body.toString()
                        .toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json",
                )
                connection.outputStream.use {
                    it.write(bytes)
                }
            }

            val status = connection.responseCode
            val stream =
                if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            val text =
                stream
                    ?.use {
                        readUtf8Limited(
                            it,
                            MAX_RESPONSE_BYTES,
                        )
                    }
                    .orEmpty()

            val json =
                if (text.isBlank()) {
                    JSONObject()
                } else {
                    JSONObject(text)
                }

            if (status !in 200..299) {
                throw GameApiException(
                    code = json.optString(
                        "error",
                        "HTTP_$status",
                    ),
                    message =
                        json.optString(
                            "message",
                            "Request failed with HTTP $status",
                        ),
                )
            }

            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun readUtf8Limited(
        stream: InputStream,
        maxBytes: Int,
    ): String {
        val output =
            ByteArrayOutputStream()
        val buffer =
            ByteArray(8 * 1024)
        var total = 0

        while (true) {
            val count =
                stream.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) {
                throw GameApiException(
                    code = "RESPONSE_TOO_LARGE",
                    message = "Server response exceeded the safety limit.",
                )
            }
            output.write(
                buffer,
                0,
                count,
            )
        }

        return output
            .toByteArray()
            .toString(Charsets.UTF_8)
    }

    private companion object {
        const val MAX_RESPONSE_BYTES =
            512 * 1024
    }

}

class GameApiException(
    val code: String,
    override val message: String,
) : RuntimeException(message)
