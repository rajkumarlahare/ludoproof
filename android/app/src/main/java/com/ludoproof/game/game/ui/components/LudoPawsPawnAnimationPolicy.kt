package com.ludoproof.game

import kotlin.math.PI
import kotlin.math.sin

enum class LudoPawsPawnMotionKind {
    FORWARD,
    CAPTURE_RETURN,
}

enum class LudoPawsCaptureReturnPhase {
    IMPACT_SHAKE,
    POP,
    RETURN_TO_YARD,
    SETTLE,
}

data class LudoPawsPawnMotion(
    val playerId: String,
    val tokenIndex: Int,
    val fromPosition: Int,
    val toPosition: Int,
    val kind: LudoPawsPawnMotionKind,
    val visualSteps: Int,
)

data class LudoPawsCaptureReturnFrame(
    val phase: LudoPawsCaptureReturnPhase,
    val routeProgress: Float,
    val shakeXCells: Float,
    val liftCells: Float,
    val scale: Float,
)

/**
 * Pure presentation policy for deciding which authoritative token transitions
 * deserve pawn animation. It never mutates match/game/proof state.
 */
object LudoPawsPawnAnimationPolicy {
    private const val CAPTURE_RETURN_VISUAL_STEPS = 4
    private const val IMPACT_END = .18f
    private const val POP_END = .34f
    private const val RETURN_END = .90f

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
                to <= LudoPathEncoding.HOME_POSITION ->
                LudoPawsPawnMotion(
                    playerId = playerId,
                    tokenIndex = tokenIndex,
                    fromPosition = from,
                    toPosition = to,
                    kind = LudoPawsPawnMotionKind.FORWARD,
                    visualSteps =
                        LudoPathEncoding
                            .visualStepCount(
                                fromPosition = from,
                                toPosition = to,
                            )
                            .coerceAtLeast(1),
                )

            LudoPathEncoding.isTrackPosition(from) &&
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

    /**
     * Four-stage presentation sequence for a captured pawn:
     * impact shake -> pop -> curved return -> soft yard settle.
     */
    fun captureReturnFrame(
        progress: Float,
    ): LudoPawsCaptureReturnFrame {
        val safe =
            progress.coerceIn(0f, 1f)

        return when {
            safe < IMPACT_END -> {
                val local = safe / IMPACT_END
                LudoPawsCaptureReturnFrame(
                    phase = LudoPawsCaptureReturnPhase.IMPACT_SHAKE,
                    routeProgress = 0f,
                    shakeXCells =
                        sin(local * PI * 6.0)
                            .toFloat() *
                            .10f *
                            (1f - local * .35f),
                    liftCells = 0f,
                    scale =
                        1f +
                            sin(local * PI)
                                .toFloat() *
                            .05f,
                )
            }

            safe < POP_END -> {
                val local =
                    (safe - IMPACT_END) /
                        (POP_END - IMPACT_END)
                val pop =
                    sin(local * PI)
                        .toFloat()
                LudoPawsCaptureReturnFrame(
                    phase = LudoPawsCaptureReturnPhase.POP,
                    routeProgress = 0f,
                    shakeXCells = 0f,
                    liftCells = pop * .24f,
                    scale = 1f + pop * .18f,
                )
            }

            safe < RETURN_END -> {
                val local =
                    (safe - POP_END) /
                        (RETURN_END - POP_END)
                val routed = easeOutCubic(local)
                LudoPawsCaptureReturnFrame(
                    phase = LudoPawsCaptureReturnPhase.RETURN_TO_YARD,
                    routeProgress = routed,
                    shakeXCells = 0f,
                    liftCells =
                        sin(local * PI)
                            .toFloat() *
                            .58f,
                    scale = 1.05f - routed * .13f,
                )
            }

            else -> {
                val local =
                    ((safe - RETURN_END) /
                        (1f - RETURN_END))
                        .coerceIn(0f, 1f)
                LudoPawsCaptureReturnFrame(
                    phase = LudoPawsCaptureReturnPhase.SETTLE,
                    routeProgress = 1f,
                    shakeXCells = 0f,
                    liftCells =
                        sin(local * PI)
                            .toFloat() *
                            .11f *
                            (1f - local),
                    scale =
                        .92f +
                            .08f * local +
                            sin(local * PI)
                                .toFloat() *
                            .04f,
                )
            }
        }
    }

    private fun easeOutCubic(
        value: Float,
    ): Float {
        val inverse =
            1f - value.coerceIn(0f, 1f)
        return 1f - inverse * inverse * inverse
    }
}
