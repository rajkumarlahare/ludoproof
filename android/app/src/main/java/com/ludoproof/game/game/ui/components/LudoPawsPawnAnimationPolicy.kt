package com.ludoproof.game

enum class LudoPawsPawnMotionKind {
    FORWARD,
    CAPTURE_RETURN,
}

data class LudoPawsPawnMotion(
    val playerId: String,
    val tokenIndex: Int,
    val fromPosition: Int,
    val toPosition: Int,
    val kind: LudoPawsPawnMotionKind,
    val visualSteps: Int,
)

/**
 * Pure presentation policy for deciding which authoritative token transitions
 * deserve pawn animation. It never mutates match/game/proof state.
 */
object LudoPawsPawnAnimationPolicy {
    private const val CAPTURE_RETURN_VISUAL_STEPS = 4

    fun plans(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
    ): List<LudoPawsPawnMotion> {
        if (
            previous == null ||
            current == null ||
            previous.matchId != current.matchId
        ) {
            return emptyList()
        }

        return buildList {
            current.players.forEach {
                    currentPlayer ->
                val previousPlayer =
                    previous.players
                        .firstOrNull {
                            it.playerId == currentPlayer.playerId
                        }
                        ?: return@forEach

                currentPlayer.tokens
                    .forEachIndexed {
                            tokenIndex,
                            to ->
                        val from =
                            previousPlayer.tokens
                                .getOrNull(tokenIndex)
                                ?: return@forEachIndexed
                        transition(
                            playerId = currentPlayer.playerId,
                            tokenIndex = tokenIndex,
                            from = from,
                            to = to,
                        )
                            ?.let(::add)
                    }
            }
        }
    }

    fun transition(
        playerId: String,
        tokenIndex: Int,
        from: Int,
        to: Int,
    ): LudoPawsPawnMotion? =
        when {
            to > from &&
                from >= -1 &&
                to <= 57 ->
                LudoPawsPawnMotion(
                    playerId = playerId,
                    tokenIndex = tokenIndex,
                    fromPosition = from,
                    toPosition = to,
                    kind = LudoPawsPawnMotionKind.FORWARD,
                    visualSteps =
                        (to - from)
                            .coerceAtLeast(1),
                )

            from in 0..51 &&
                to == -1 ->
                LudoPawsPawnMotion(
                    playerId = playerId,
                    tokenIndex = tokenIndex,
                    fromPosition = from,
                    toPosition = to,
                    kind = LudoPawsPawnMotionKind.CAPTURE_RETURN,
                    visualSteps = CAPTURE_RETURN_VISUAL_STEPS,
                )

            else -> null
        }
}
