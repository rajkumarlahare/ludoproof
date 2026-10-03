package com.ludoproof.game

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal object RemoteForfeitApi {
    fun forfeit(
        matchId: String,
        playerToken: String,
        baseUrl: String =
            BuildConfig.LUDOPROOF_API_BASE_URL,
    ) {
        require(
            baseUrl.startsWith(
                "https://",
            ),
        ) {
            "LudoProof API must use HTTPS"
        }

        val normalizedBase =
            baseUrl.trimEnd('/')
        val normalizedMatchId =
            matchId
                .trim()
                .uppercase()
        val connection =
            URL(
                normalizedBase +
                    "/api/matches/" +
                    normalizedMatchId +
                    "/start",
            )
                .openConnection() as
                HttpURLConnection

        try {
            connection.requestMethod =
                "POST"
            connection.connectTimeout =
                3_500
            connection.readTimeout =
                5_000
            connection.instanceFollowRedirects =
                false
            connection.doOutput =
                true
            connection.setRequestProperty(
                "Accept",
                "application/json",
            )
            connection.setRequestProperty(
                "Content-Type",
                "application/json",
            )
            connection.setRequestProperty(
                "Authorization",
                "Bearer " +
                    playerToken,
            )
            connection.setRequestProperty(
                "User-Agent",
                "LudoProof-Android/" +
                    BuildConfig.VERSION_NAME,
            )

            val bytes =
                JSONObject()
                    .put(
                        "forfeit",
                        true,
                    )
                    .toString()
                    .toByteArray(
                        Charsets.UTF_8,
                    )
            connection.outputStream.use {
                it.write(
                    bytes,
                )
            }

            val status =
                connection.responseCode
            val stream =
                if (
                    status in
                    200..299
                ) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            val responseText =
                stream
                    ?.use {
                        readUtf8Limited(
                            it,
                            MAX_RESPONSE_BYTES,
                        )
                    }
                    .orEmpty()

            if (
                status !in
                200..299
            ) {
                val value =
                    runCatching {
                        JSONObject(
                            responseText,
                        )
                    }
                        .getOrNull()
                throw GameApiException(
                    code =
                        value
                            ?.optString(
                                "error",
                                "HTTP_$status",
                            )
                            ?: "HTTP_$status",
                    message =
                        value
                            ?.optString(
                                "message",
                                "Forfeit failed with HTTP $status",
                            )
                            ?: "Forfeit failed with HTTP $status",
                )
            }
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
            ByteArray(
                4 *
                    1024,
            )
        var total =
            0

        while (
            true
        ) {
            val count =
                stream.read(
                    buffer,
                )
            if (
                count <
                0
            ) {
                break
            }
            total +=
                count
            if (
                total >
                maxBytes
            ) {
                throw GameApiException(
                    code =
                        "RESPONSE_TOO_LARGE",
                    message =
                        "Forfeit response exceeded the safety limit.",
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
            .toString(
                Charsets.UTF_8,
            )
    }

    private const val MAX_RESPONSE_BYTES =
        64 *
            1024
}
