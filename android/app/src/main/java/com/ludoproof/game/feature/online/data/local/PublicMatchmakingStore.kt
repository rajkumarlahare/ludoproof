package com.ludoproof.game

import android.content.Context

data class PublicMatchmakingTicket(
    val requestId: String,
    val displayName: String,
    val playerCount: Int,
    val startedAt: Long,
)

class PublicMatchmakingStore(
    context: Context,
) {
    private val prefs =
        context
            .applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            )

    fun save(
        ticket: PublicMatchmakingTicket,
    ) {
        require(
            UUID_V4.matches(
                ticket.requestId,
            ),
        )
        require(
            ticket.playerCount ==
                2 ||
                ticket.playerCount ==
                4,
        )
        require(
            ticket.displayName
                .isNotBlank(),
        )

        val persisted =
            prefs.edit()
                .putString(
                    KEY_REQUEST_ID,
                    ticket.requestId,
                )
                .putString(
                    KEY_DISPLAY_NAME,
                    ticket.displayName,
                )
                .putInt(
                    KEY_PLAYER_COUNT,
                    ticket.playerCount,
                )
                .putLong(
                    KEY_STARTED_AT,
                    ticket.startedAt,
                )
                .commit()

        check(
            persisted,
        ) {
            "Matchmaking search could not be persisted"
        }
    }

    fun load():
        PublicMatchmakingTicket? {
        val requestId =
            prefs.getString(
                KEY_REQUEST_ID,
                null,
            )
                ?: return null
        val displayName =
            prefs.getString(
                KEY_DISPLAY_NAME,
                null,
            )
                ?: return clearAndNull()
        val playerCount =
            prefs.getInt(
                KEY_PLAYER_COUNT,
                0,
            )
        val startedAt =
            prefs.getLong(
                KEY_STARTED_AT,
                0L,
            )

        if (
            !UUID_V4.matches(
                requestId,
            ) ||
            displayName
                .isBlank() ||
            (
                playerCount !=
                    2 &&
                playerCount !=
                    4
                ) ||
            startedAt <=
                0L
        ) {
            return clearAndNull()
        }

        return PublicMatchmakingTicket(
            requestId =
                requestId,
            displayName =
                displayName,
            playerCount =
                playerCount,
            startedAt =
                startedAt,
        )
    }

    fun clear() {
        prefs.edit()
            .clear()
            .apply()
    }

    private fun clearAndNull():
        PublicMatchmakingTicket? {
        clear()
        return null
    }

    private companion object {
        const val PREFS_NAME =
            "ludoproof_public_matchmaking_v1"
        const val KEY_REQUEST_ID =
            "requestId"
        const val KEY_DISPLAY_NAME =
            "displayName"
        const val KEY_PLAYER_COUNT =
            "playerCount"
        const val KEY_STARTED_AT =
            "startedAt"

        val UUID_V4 =
            Regex(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            )
    }
}
