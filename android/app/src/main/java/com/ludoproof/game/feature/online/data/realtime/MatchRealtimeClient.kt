package com.ludoproof.game

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class MatchRealtimeClient(
    baseUrl: String =
        BuildConfig.LUDOPROOF_API_BASE_URL,
    private val onRevision:
        (
            revision: Int,
            status: String?,
        ) -> Unit,
    private val onConnectionChanged:
        (
            connected: Boolean,
        ) -> Unit = {},
) {
    private val wsBaseUrl =
        baseUrl
            .trimEnd('/')
            .replaceFirst(
                "https://",
                "wss://",
            )

    private val client =
        OkHttpClient
            .Builder()
            .pingInterval(
                20,
                TimeUnit.SECONDS,
            )
            .build()

    private val generation =
        AtomicLong(0L)

    @Volatile
    private var socket:
        WebSocket? =
        null

    init {
        require(
            wsBaseUrl
                .startsWith(
                    "wss://",
                ),
        ) {
            "LudoProof realtime must use WSS"
        }
    }

    fun connect(
        matchId: String,
        playerToken: String,
    ) {
        val currentGeneration =
            generation
                .incrementAndGet()

        socket
            ?.cancel()
        socket =
            null
        onConnectionChanged(
            false,
        )

        val request =
            Request
                .Builder()
                .url(
                    wsBaseUrl +
                        "/api/matches/" +
                        matchId.uppercase() +
                        "/events",
                )
                .header(
                    "Authorization",
                    "Bearer $playerToken",
                )
                .header(
                    "User-Agent",
                    "LudoProof-Android/" +
                        BuildConfig.VERSION_NAME,
                )
                .build()

        socket =
            client
                .newWebSocket(
                    request,
                    object :
                        WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            if (
                                generation
                                    .get() !=
                                currentGeneration
                            ) {
                                webSocket.cancel()
                                return
                            }
                            onConnectionChanged(
                                true,
                            )
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String,
                        ) {
                            if (
                                generation
                                    .get() !=
                                currentGeneration
                            ) {
                                return
                            }

                            val payload =
                                runCatching {
                                    JSONObject(
                                        text,
                                    )
                                }
                                    .getOrNull()
                                    ?: return

                            val type =
                                payload
                                    .optString(
                                        "type",
                                    )
                            if (
                                type !=
                                    "SYNC" &&
                                type !=
                                    "STATE_CHANGED"
                            ) {
                                return
                            }

                            val revision =
                                payload
                                    .optInt(
                                        "revision",
                                        -1,
                                    )
                            if (
                                revision >=
                                0
                            ) {
                                onRevision(
                                    revision,
                                    payload
                                        .optString(
                                            "status",
                                        )
                                        .takeIf {
                                            it.isNotBlank()
                                        },
                                )
                            }
                        }

                        override fun onClosing(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            if (
                                generation
                                    .get() ==
                                currentGeneration
                            ) {
                                onConnectionChanged(
                                    false,
                                )
                            }
                            webSocket.close(
                                code,
                                reason,
                            )
                        }

                        override fun onClosed(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            if (
                                generation
                                    .get() ==
                                currentGeneration
                            ) {
                                onConnectionChanged(
                                    false,
                                )
                            }
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?,
                        ) {
                            if (
                                generation
                                    .get() ==
                                currentGeneration
                            ) {
                                onConnectionChanged(
                                    false,
                                )
                            }
                        }
                    },
                )
    }

    fun disconnect() {
        generation
            .incrementAndGet()
        socket
            ?.close(
                1000,
                "screen stopped",
            )
        socket =
            null
        onConnectionChanged(
            false,
        )
    }

    fun shutdown() {
        disconnect()
        client.dispatcher
            .executorService
            .shutdown()
        client.connectionPool
            .evictAll()
    }
}
