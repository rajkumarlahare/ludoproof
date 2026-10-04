package com.ludoproof.game

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadLocalRandom
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
    private val stateLock =
        Any()
    private val retryExecutor =
        Executors.newSingleThreadScheduledExecutor {
                runnable ->
            Thread(
                runnable,
                "ludo-paws-realtime-retry",
            ).apply {
                isDaemon = true
            }
        }

    private var desiredSession:
        DesiredSession? =
        null
    private var retryFuture:
        ScheduledFuture<*>? =
        null
    private var reconnectAttempt =
        0
    private var connecting =
        false
    private var connected =
        false

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
        val session =
            DesiredSession(
                matchId =
                    matchId.uppercase(),
                playerToken =
                    playerToken,
            )
        var staleSocket:
            WebSocket? =
            null
        val shouldOpen =
            synchronized(
                stateLock,
            ) {
                if (
                    desiredSession !=
                    session
                ) {
                    desiredSession =
                        session
                    reconnectAttempt =
                        0
                    retryFuture
                        ?.cancel(false)
                    retryFuture =
                        null
                    generation
                        .incrementAndGet()
                    staleSocket =
                        socket
                    socket =
                        null
                    connecting =
                        false
                    connected =
                        false
                }

                val retryScheduled =
                    retryFuture
                        ?.let {
                            !it.isDone &&
                                !it.isCancelled
                        }
                        ?: false

                !connected &&
                    !connecting &&
                    !retryScheduled
            }

        staleSocket
            ?.cancel()

        if (shouldOpen) {
            openSocket(
                session,
            )
        }
    }

    private fun openSocket(
        session: DesiredSession,
    ) {
        val currentGeneration =
            synchronized(
                stateLock,
            ) {
                if (
                    desiredSession !=
                        session ||
                    connected ||
                    connecting
                ) {
                    return
                }

                retryFuture =
                    null
                connecting =
                    true
                generation
                    .incrementAndGet()
            }

        val request =
            Request
                .Builder()
                .url(
                    wsBaseUrl +
                        "/api/matches/" +
                        session.matchId +
                        "/events",
                )
                .header(
                    "Authorization",
                    "Bearer ${session.playerToken}",
                )
                .header(
                    "User-Agent",
                    "LudoProof-Android/" +
                        BuildConfig.VERSION_NAME,
                )
                .build()

        val createdSocket =
            client
                .newWebSocket(
                    request,
                    listener(
                        session =
                            session,
                        connectionGeneration =
                            currentGeneration,
                    ),
                )

        val keepSocket =
            synchronized(
                stateLock,
            ) {
                desiredSession ==
                    session &&
                    generation.get() ==
                    currentGeneration &&
                    (
                        connecting ||
                            connected
                    )
            }
        if (keepSocket) {
            socket =
                createdSocket
        } else {
            createdSocket.cancel()
        }
    }

    private fun listener(
        session: DesiredSession,
        connectionGeneration: Long,
    ): WebSocketListener =
        object :
            WebSocketListener() {
            override fun onOpen(
                webSocket: WebSocket,
                response: Response,
            ) {
                val accepted =
                    synchronized(
                        stateLock,
                    ) {
                        if (
                            generation.get() !=
                                connectionGeneration ||
                            desiredSession !=
                                session
                        ) {
                            false
                        } else {
                            connecting =
                                false
                            connected =
                                true
                            reconnectAttempt =
                                0
                            retryFuture
                                ?.cancel(false)
                            retryFuture =
                                null
                            true
                        }
                    }

                if (!accepted) {
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
                    !isCurrent(
                        session,
                        connectionGeneration,
                    )
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
                handleTransportClosed(
                    session =
                        session,
                    connectionGeneration =
                        connectionGeneration,
                )
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?,
            ) {
                handleTransportClosed(
                    session =
                        session,
                    connectionGeneration =
                        connectionGeneration,
                )
            }
        }

    private fun handleTransportClosed(
        session: DesiredSession,
        connectionGeneration: Long,
    ) {
        val shouldNotify =
            synchronized(
                stateLock,
            ) {
                if (
                    generation.get() !=
                        connectionGeneration ||
                    desiredSession !=
                        session
                ) {
                    return
                }

                val wasActive =
                    connected ||
                        connecting
                connected =
                    false
                connecting =
                    false
                socket =
                    null
                scheduleRetryLocked(
                    session,
                )
                wasActive
            }

        if (shouldNotify) {
            onConnectionChanged(
                false,
            )
        }
    }

    private fun scheduleRetryLocked(
        session: DesiredSession,
    ) {
        if (
            desiredSession !=
                session ||
            connected ||
            connecting ||
            retryExecutor.isShutdown
        ) {
            return
        }

        val existing =
            retryFuture
        if (
            existing != null &&
            !existing.isDone &&
            !existing.isCancelled
        ) {
            return
        }

        val attempt =
            reconnectAttempt
        val delayMillis =
            RealtimeReconnectBackoff
                .delayMillis(
                    attempt =
                        attempt,
                    jitterUnit =
                        ThreadLocalRandom
                            .current()
                            .nextDouble(),
                )
        reconnectAttempt =
            (
                reconnectAttempt +
                    1
                )
                .coerceAtMost(4)

        retryFuture =
            retryExecutor.schedule(
                {
                    val nextSession =
                        synchronized(
                            stateLock,
                        ) {
                            retryFuture =
                                null
                            desiredSession
                                ?.takeIf {
                                    it ==
                                        session &&
                                        !connected &&
                                        !connecting
                                }
                        }
                    if (
                        nextSession !=
                        null
                    ) {
                        openSocket(
                            nextSession,
                        )
                    }
                },
                delayMillis,
                TimeUnit.MILLISECONDS,
            )
    }

    private fun isCurrent(
        session: DesiredSession,
        connectionGeneration: Long,
    ): Boolean =
        synchronized(
            stateLock,
        ) {
            generation.get() ==
                connectionGeneration &&
                desiredSession ==
                session
        }

    fun disconnect() {
        val staleSocket =
            synchronized(
                stateLock,
            ) {
                generation
                    .incrementAndGet()
                desiredSession =
                    null
                retryFuture
                    ?.cancel(false)
                retryFuture =
                    null
                reconnectAttempt =
                    0
                connecting =
                    false
                connected =
                    false
                socket
                    .also {
                        socket =
                            null
                    }
            }

        staleSocket
            ?.close(
                1000,
                "screen stopped",
            )
        onConnectionChanged(
            false,
        )
    }

    fun shutdown() {
        disconnect()
        retryExecutor
            .shutdownNow()
        client.dispatcher
            .executorService
            .shutdown()
        client.connectionPool
            .evictAll()
    }

    private data class DesiredSession(
        val matchId: String,
        val playerToken: String,
    )
}
