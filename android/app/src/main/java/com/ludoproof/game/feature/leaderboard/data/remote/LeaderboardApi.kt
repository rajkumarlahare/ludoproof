package com.ludoproof.game.feature.leaderboard.data.remote

import com.ludoproof.game.BuildConfig
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LeaderboardApi(
    baseUrl: String =
        BuildConfig.LUDOPROOF_API_BASE_URL,
) {
    private val baseUrl =
        baseUrl.trimEnd('/')

    fun load(
        profileId: String,
        limit: Int = 50,
    ): JSONObject {
        require(
            baseUrl.startsWith(
                "https://",
            ),
        ) {
            "LudoProof API must use HTTPS"
        }

        val safeLimit =
            limit.coerceIn(
                1,
                50,
            )
        val encodedProfileId =
            URLEncoder.encode(
                profileId,
                Charsets.UTF_8.name(),
            )
        val connection =
            URL(
                baseUrl +
                    "/api/leaderboard" +
                    "?limit=" +
                    safeLimit +
                    "&profileId=" +
                    encodedProfileId,
            ).openConnection() as
                HttpURLConnection

        try {
            connection.requestMethod =
                "GET"
            connection.connectTimeout =
                10_000
            connection.readTimeout =
                20_000
            connection.instanceFollowRedirects =
                false
            connection.setRequestProperty(
                "Accept",
                "application/json",
            )
            connection.setRequestProperty(
                "User-Agent",
                "LudoProof-Android/" +
                    BuildConfig.VERSION_NAME,
            )

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
                if (
                    text.isBlank()
                ) {
                    JSONObject()
                } else {
                    JSONObject(
                        text,
                    )
                }

            if (
                status !in
                200..299
            ) {
                throw LeaderboardApiException(
                    json.optString(
                        "message",
                        "Leaderboard request failed with HTTP $status",
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
            ByteArray(
                8 * 1024,
            )
        var total =
            0

        while (true) {
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
                throw LeaderboardApiException(
                    "Leaderboard response exceeded the safety limit.",
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

    private companion object {
        const val MAX_RESPONSE_BYTES =
            256 * 1024
    }
}

class LeaderboardApiException(
    override val message: String,
) : RuntimeException(
    message,
)
