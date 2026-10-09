package com.ludoproof.game

import android.opengl.GLES30
import android.opengl.Matrix
import com.ludoproof.game.feature.characters.prototype.Cat3DMotion
import com.ludoproof.game.feature.characters.prototype.Cat3DMotionTimeline
import com.ludoproof.game.feature.characters.prototype.Cat3DPose
import com.ludoproof.game.feature.characters.prototype.Dog3DMotion
import com.ludoproof.game.feature.characters.prototype.Dog3DMotionTimeline
import com.ludoproof.game.feature.characters.prototype.Dog3DPose
import com.ludoproof.game.feature.characters.prototype.Duck3DMotion
import com.ludoproof.game.feature.characters.prototype.Duck3DMotionTimeline
import com.ludoproof.game.feature.characters.prototype.Duck3DPose
import com.ludoproof.game.feature.characters.prototype.Goat3DMotion
import com.ludoproof.game.feature.characters.prototype.Goat3DMotionTimeline
import com.ludoproof.game.feature.characters.prototype.Goat3DPose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Shared production renderer for all code-generated animal pawns.
 *
 * Geometry comes from the four approved prototype renderers, but every pawn is
 * drawn through one ES3 program, one sphere mesh, and one EGL context. Board
 * coordinates come only from LudoPawsFxBoardGeometry/LudoPathEncoding so the 3D
 * layer cannot invent a second game route.
 */
internal class LudoPaws3DSceneRenderer {
    private var width = 1
    private var height = 1
    private var program = 0
    private var uModelLocation = -1
    private var uMvpLocation = -1
    private var uColorLocation = -1
    private var uLightDirectionLocation = -1
    private var uNormalMatrixLocation = -1
    private lateinit var sphere: SphereMesh

    private val projection = FloatArray(16)
    private val root = FloatArray(16)
    private val model = FloatArray(16)
    private val inverseModel = FloatArray(16)
    private val normalMatrix = FloatArray(9)
    private val mvp = FloatArray(16)

    fun onSurfaceCreated() {
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(
            GLES30.GL_SRC_ALPHA,
            GLES30.GL_ONE_MINUS_SRC_ALPHA,
        )
        program =
            createProgram(
                VERTEX_SHADER,
                FRAGMENT_SHADER,
            )
        uModelLocation = GLES30.glGetUniformLocation(program, "uModel")
        uMvpLocation = GLES30.glGetUniformLocation(program, "uMvp")
        uColorLocation = GLES30.glGetUniformLocation(program, "uColor")
        uLightDirectionLocation = GLES30.glGetUniformLocation(program, "uLightDirection")
        uNormalMatrixLocation = GLES30.glGetUniformLocation(program, "uNormalMatrix")
        check(
            uModelLocation >= 0 &&
                uMvpLocation >= 0 &&
                uColorLocation >= 0 &&
                uLightDirectionLocation >= 0 &&
                uNormalMatrixLocation >= 0,
        ) { "3D pawn shader is missing a required uniform" }
        sphere =
            SphereMesh(
                latitudeSegments = 12,
                longitudeSegments = 18,
                uModelLocation = uModelLocation,
                uMvpLocation = uMvpLocation,
                uColorLocation = uColorLocation,
                uLightDirectionLocation = uLightDirectionLocation,
                uNormalMatrixLocation = uNormalMatrixLocation,
            )
    }

    fun onSurfaceChanged(
        width: Int,
        height: Int,
    ) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
        GLES30.glViewport(
            0,
            0,
            this.width,
            this.height,
        )

        // LudoPawsReactiveBoardView may give this transparent surface extra
        // pixels only above the square board. Keep world-space board Y=0 at the
        // exact visual board edge by mapping that overflow to negative world Y.
        // The board, pawn centers, path geometry and model scale therefore remain
        // unchanged while heads/ears/horns can render above the top row.
        val topOverflow =
            (this.height - this.width)
                .coerceAtLeast(0)
                .toFloat()
        val boardBottom =
            (this.height.toFloat() - topOverflow)
                .coerceAtLeast(1f)
        Matrix.orthoM(
            projection,
            0,
            0f,
            this.width.toFloat(),
            boardBottom,
            -topOverflow,
            -100f,
            100f,
        )
    }

    fun drawFrame(
        state: LudoPaws3DSceneState,
        nowMillis: Long,
    ) {
        GLES30.glClear(
            GLES30.GL_COLOR_BUFFER_BIT or
                GLES30.GL_DEPTH_BUFFER_BIT,
        )
        val snapshot = state.snapshot ?: return
        if (program == 0) return
        GLES30.glUseProgram(program)

        val size = min(width, height).toFloat()
        val cell = size / LudoPawsFxBoardGeometry.BOARD_SIZE
        val turns =
            state.perspectiveColor
                ?.let(OfflinePlayerLayout::rotationQuarterTurns)
                ?: 0
        val occupancy =
            occupancyByCenter(
                snapshot = snapshot,
                cell = cell,
            )
        val stackPlacements =
            LudoPawsPawnLayout.stackPlacements(
                snapshot = snapshot,
                cell = cell,
            )

        val renderPawns =
            buildList {
                snapshot.players.forEach { player ->
                    val teamColor =
                        LudoPawsTeamIdentityPolicy.resolve(player.color)
                            ?: return@forEach
                    val species =
                        LudoPaws3DCharacterPolicy
                            .speciesForColor(player.color)
                            ?: return@forEach
                    player.tokens.forEachIndexed { tokenIndex, currentPosition ->
                        val key =
                            LudoPaws3DPawnKey(
                                playerId = player.playerId,
                                tokenIndex = tokenIndex,
                            )
                        val captureVisual =
                            state.captureReturns[key]
                                ?.let { capture ->
                                    captureReturnVisual(
                                        player = player,
                                        tokenIndex = tokenIndex,
                                        capture = capture,
                                        cell = cell,
                                        nowMillis = nowMillis,
                                    )
                                }

                        val rawCenter =
                            captureVisual?.center
                                ?: animatedCenter(
                                    player = player,
                                    tokenIndex = tokenIndex,
                                    currentPosition = currentPosition,
                                    cell = cell,
                                    state = state,
                                    nowMillis = nowMillis,
                                )
                                ?: return@forEachIndexed
                        val stackPlacement =
                            if (captureVisual == null) {
                                stackPlacements[
                                    LudoPawsPawnVisualKey(
                                        playerId = player.playerId,
                                        tokenIndex = tokenIndex,
                                    )
                                ]
                            } else {
                                null
                            }
                        val offset =
                            if (captureVisual == null) {
                                (stackPlacement?.offsetXFraction ?: 0f) to
                                    (stackPlacement?.offsetYFraction ?: 0f)
                            } else {
                                0f to 0f
                            }
                        val unrotatedX = rawCenter.first + offset.first * cell
                        val unrotatedY = rawCenter.second + offset.second * cell
                        val rotated =
                            rotatePoint(
                                x = unrotatedX,
                                y = unrotatedY,
                                size = size,
                                turns = turns,
                            )
                        val renderPosition =
                            captureVisual?.fromPosition
                                ?: currentPosition
                        val occupants =
                            if (captureVisual != null) {
                                1
                            } else {
                                val occupancyKey =
                                    centerKey(
                                        LudoPawsFxBoardGeometry.tokenCenter(
                                            color = player.color,
                                            tokenIndex = tokenIndex,
                                            position = currentPosition,
                                            cell = cell,
                                        ),
                                    )
                                occupancy[occupancyKey] ?: 1
                            }
                        val radius =
                            cell *
                                LudoPawsPawnLayout.radiusScale(
                                    position = renderPosition,
                                    occupancy = occupants,
                                )
                        val facingYawDegrees =
                            if (captureVisual == null) {
                                facingYawDegrees(
                                    player = player,
                                    tokenIndex = tokenIndex,
                                    state = state,
                                    nowMillis = nowMillis,
                                    size = size,
                                    cell = cell,
                                    turns = turns,
                                )
                            } else {
                                LudoPawsPawnFacingPolicy.FRONT_YAW_DEGREES
                            }

                        add(
                            RenderPawn(
                                key = key,
                                species = species,
                                teamColor = teamColor,
                                seat = player.seat,
                                x = rotated.first,
                                y = rotated.second,
                                radius = radius,
                                facingYawDegrees = facingYawDegrees,
                                presentationScale =
                                    captureVisual?.scale ?: 1f,
                                attentionScale =
                                    legalAttentionScale(
                                        state = state,
                                        key = key,
                                        nowMillis = nowMillis,
                                    ),
                            ),
                        )
                    }
                }
            }.sortedBy(RenderPawn::y)

        // Shadows are drawn as a separate background pass so one pawn's shadow
        // can never darken another pawn that was already painted in front of it.
        // The board coordinates remain authoritative; this pass is visual only.
        renderPawns.forEach(::drawContactShadow)

        renderPawns.forEach { pawn ->
            // Preserve depth inside one animal but use painter ordering between
            // animals. This prevents one pawn's nose/ear depth cutting another.
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
            drawPawn(
                pawn = pawn,
                state = state,
                nowMillis = nowMillis,
            )
        }
    }

    private fun drawPawn(
        pawn: RenderPawn,
        state: LudoPaws3DSceneState,
        nowMillis: Long,
    ) {
        val frame =
            motionFrame(
                pawn = pawn,
                state = state,
                nowMillis = nowMillis,
            )
        val reaction =
            reactionPose(
                pawn = pawn,
                state = state,
                nowMillis = nowMillis,
            )
        val movementPose =
            movementScalePose(
                pawn = pawn,
                state = state,
                nowMillis = nowMillis,
            )
        val modelScale =
            pawn.radius *
                MODEL_SCALE_PER_RADIUS *
                pawn.presentationScale *
                pawn.attentionScale *
                reaction.scale *
                movementPose.scale
        val rootY =
            pawn.y +
                pawn.radius * rootYOffsetPerRadius(pawn.species) +
                movementPose.liftY

        when (pawn.species) {
            LudoPaws3DSpecies.DOG -> {
                val base =
                    when (frame.kind) {
                        SceneMotion.IDLE -> Dog3DMotionTimeline.sample(Dog3DMotion.IDLE, frame.progress)
                        SceneMotion.HOP -> Dog3DMotionTimeline.sample(Dog3DMotion.HOP, frame.progress)
                        SceneMotion.HOME -> Dog3DMotionTimeline.sample(Dog3DMotion.HOME, frame.progress)
                    }
                val pose =
                    base.copy(
                        liftY = base.liftY + reaction.liftY,
                        bodyYawDegrees = pawn.facingYawDegrees + base.bodyYawDegrees + reaction.bodyYawDegrees,
                        headTiltDegrees = base.headTiltDegrees + reaction.headTiltDegrees,
                        earBounceDegrees = base.earBounceDegrees + reaction.primaryAppendageDegrees,
                        tailWagDegrees = base.tailWagDegrees + reaction.secondaryAppendageDegrees,
                    )
                prepareRoot(
                    x = pawn.x,
                    y = rootY,
                    scale = modelScale,
                    liftY = pose.liftY,
                    bodyYawDegrees = pose.bodyYawDegrees,
                )
                drawDog(root, pose, pawn.teamColor)
            }

            LudoPaws3DSpecies.GOAT -> {
                val base =
                    when (frame.kind) {
                        SceneMotion.IDLE -> Goat3DMotionTimeline.sample(Goat3DMotion.IDLE, frame.progress)
                        SceneMotion.HOP -> Goat3DMotionTimeline.sample(Goat3DMotion.HOP, frame.progress)
                        SceneMotion.HOME -> Goat3DMotionTimeline.sample(Goat3DMotion.HOME, frame.progress)
                    }
                val pose =
                    base.copy(
                        liftY = base.liftY + reaction.liftY,
                        bodyYawDegrees = pawn.facingYawDegrees + base.bodyYawDegrees + reaction.bodyYawDegrees,
                        headTiltDegrees = base.headTiltDegrees + reaction.headTiltDegrees,
                        earFlickDegrees = base.earFlickDegrees + reaction.primaryAppendageDegrees,
                        beardSwingDegrees = base.beardSwingDegrees + reaction.secondaryAppendageDegrees,
                        tailFlickDegrees = base.tailFlickDegrees + reaction.tertiaryAppendageDegrees,
                    )
                prepareRoot(
                    x = pawn.x,
                    y = rootY,
                    scale = modelScale,
                    liftY = pose.liftY,
                    bodyYawDegrees = pose.bodyYawDegrees,
                )
                drawGoat(root, pose, pawn.teamColor)
            }

            LudoPaws3DSpecies.DUCK -> {
                val base =
                    when (frame.kind) {
                        SceneMotion.IDLE -> Duck3DMotionTimeline.sample(Duck3DMotion.IDLE, frame.progress)
                        SceneMotion.HOP -> Duck3DMotionTimeline.sample(Duck3DMotion.HOP, frame.progress)
                        SceneMotion.HOME -> Duck3DMotionTimeline.sample(Duck3DMotion.HOME, frame.progress)
                    }
                val pose =
                    base.copy(
                        liftY = base.liftY + reaction.liftY,
                        bodyYawDegrees = pawn.facingYawDegrees + base.bodyYawDegrees + reaction.bodyYawDegrees,
                        wingFlapDegrees = base.wingFlapDegrees + reaction.primaryAppendageDegrees,
                        headTiltDegrees = base.headTiltDegrees + reaction.headTiltDegrees,
                    )
                prepareRoot(
                    x = pawn.x,
                    y = rootY,
                    scale = modelScale,
                    liftY = pose.liftY,
                    bodyYawDegrees = pose.bodyYawDegrees,
                )
                drawDuck(root, pose, pawn.teamColor)
            }

            LudoPaws3DSpecies.CAT -> {
                val base =
                    when (frame.kind) {
                        SceneMotion.IDLE -> Cat3DMotionTimeline.sample(Cat3DMotion.IDLE, frame.progress)
                        SceneMotion.HOP -> Cat3DMotionTimeline.sample(Cat3DMotion.HOP, frame.progress)
                        SceneMotion.HOME -> Cat3DMotionTimeline.sample(Cat3DMotion.HOME, frame.progress)
                    }
                val pose =
                    base.copy(
                        liftY = base.liftY + reaction.liftY,
                        bodyYawDegrees = pawn.facingYawDegrees + base.bodyYawDegrees + reaction.bodyYawDegrees,
                        headTiltDegrees = base.headTiltDegrees + reaction.headTiltDegrees,
                        earTwitchDegrees = base.earTwitchDegrees + reaction.primaryAppendageDegrees,
                        tailSwayDegrees = base.tailSwayDegrees + reaction.secondaryAppendageDegrees,
                    )
                prepareRoot(
                    x = pawn.x,
                    y = rootY,
                    scale = modelScale,
                    liftY = pose.liftY,
                    bodyYawDegrees = pose.bodyYawDegrees,
                )
                drawCat(root, pose, pawn.teamColor)
            }
        }
    }

    // Active-move squash/stretch envelope. Kept presentation-only so board
    // coordinates, route state, hit testing and timing remain authoritative.
    private fun movementScalePose(
        pawn: RenderPawn,
        state: LudoPaws3DSceneState,
        nowMillis: Long,
    ): MovementScalePose {
        val forward = state.forwardMotion
        if (
            forward == null ||
            forward.playerId != pawn.key.playerId ||
            forward.tokenIndex != pawn.key.tokenIndex ||
            state.forwardDurationMillis <= 0L
        ) {
            return MovementScalePose()
        }

        val elapsed =
            (nowMillis - state.forwardStartedAtMillis)
                .coerceAtLeast(0L)
        val totalDuration = state.forwardDurationMillis
        if (elapsed >= totalDuration) {
            return MovementScalePose()
        }

        val travelDuration =
            (
                totalDuration -
                    LudoPaws3DRenderCadencePolicy.FORWARD_LANDING_SETTLE_MILLIS
            ).coerceAtLeast(1L)

        // Deliberately exaggerated for a clear, playful jump silhouette:
        // 88% anticipation -> 124% takeoff pop -> 106% airborne -> 91% landing.
        val anticipationMillis =
            min(
                80L,
                max(
                    55L,
                    travelDuration / 7L,
                ),
            )
        val takeoffMillis =
            min(
                150L,
                max(
                    105L,
                    travelDuration / 4L,
                ),
            )
        val landingMillis =
            min(
                95L,
                max(
                    65L,
                    LudoPaws3DRenderCadencePolicy
                        .FORWARD_LANDING_SETTLE_MILLIS / 3L,
                ),
            )

        val scale: Float
        val liftY: Float

        when {
            elapsed < anticipationMillis -> {
                val t =
                    smoothStep(
                        elapsed.toFloat() /
                            anticipationMillis.toFloat(),
                    )
                scale = lerp(1f, 0.88f, t)
                // A tiny pre-load keeps the takeoff readable instead of looking flat.
                liftY =
                    pawn.radius *
                        0.06f *
                        smoothStep(t)
            }

            elapsed < anticipationMillis + takeoffMillis -> {
                val t =
                    smoothStep(
                        (
                            elapsed - anticipationMillis
                        ).toFloat() /
                            takeoffMillis.toFloat(),
                    )
                scale = lerp(0.88f, 1.24f, t)
                // Strong vertical takeoff: clearly rises before the path hop begins.
                liftY =
                    lerp(
                        pawn.radius * 0.06f,
                        pawn.radius * 0.78f,
                        t,
                    )
            }

            elapsed < travelDuration -> {
                val travelElapsed =
                    elapsed - anticipationMillis - takeoffMillis
                val travelWindow =
                    (
                        travelDuration -
                            anticipationMillis -
                            takeoffMillis
                    ).coerceAtLeast(1L)
                val t =
                    (
                        travelElapsed.toFloat() /
                            travelWindow.toFloat()
                    ).coerceIn(0f, 1f)
                val eased = smoothStep(t)
                scale = lerp(1.24f, 1.06f, eased)
                liftY =
                    lerp(
                        pawn.radius * 0.78f,
                        pawn.radius * 0.16f,
                        eased,
                    )
            }

            elapsed < totalDuration - landingMillis -> {
                scale = 1.06f
                liftY = pawn.radius * 0.16f
            }

            else -> {
                val t =
                    smoothStep(
                        (
                            elapsed - (totalDuration - landingMillis)
                        ).toFloat() /
                            landingMillis.toFloat(),
                    )
                val landingT =
                    (
                        t * 2f
                    ).coerceAtMost(1f)
                scale =
                    when {
                        t < 0.5f ->
                            lerp(1.06f, 0.91f, smoothStep(landingT))

                        else ->
                            lerp(
                                0.91f,
                                1f,
                                smoothStep(
                                    (t - 0.5f) * 2f,
                                ),
                            )
                    }
                liftY =
                    lerp(
                        pawn.radius * 0.16f,
                        0f,
                        t,
                    )
            }
        }

        return MovementScalePose(
            scale = scale,
            liftY = liftY,
        )
    }

    private data class MovementScalePose(
        val scale: Float = 1f,
        val liftY: Float = 0f,
    )

    private fun reactionPose(
        pawn: RenderPawn,
        state: LudoPaws3DSceneState,
        nowMillis: Long,
    ): LudoPaws3DReactionPose {
        val active =
            state.activeReactions[pawn.key]
                ?: return LudoPaws3DReactionPose()
        if (active.durationMillis <= 0L) return LudoPaws3DReactionPose()
        val elapsed = (nowMillis - active.startedAtMillis).coerceAtLeast(0L)
        if (elapsed >= active.durationMillis) return LudoPaws3DReactionPose()
        return LudoPaws3DReactionMotion.sample(
            species = pawn.species,
            cue = active.cue,
            progress = elapsed.toFloat() / active.durationMillis.toFloat(),
        )
    }

    private fun motionFrame(
        pawn: RenderPawn,
        state: LudoPaws3DSceneState,
        nowMillis: Long,
    ): MotionFrame {
        val capture = state.captureReturns[pawn.key]
        if (
            capture != null &&
            capture.durationMillis > 0L &&
            nowMillis < capture.startedAtMillis + capture.durationMillis
        ) {
            // The capture route owns board translation/scale. Keep the base body
            // stable so the CAPTURED reaction can add species-specific emotion.
            return MotionFrame(
                kind = SceneMotion.IDLE,
                progress = 0f,
            )
        }

        val forward = state.forwardMotion
        if (
            forward != null &&
            forward.playerId == pawn.key.playerId &&
            forward.tokenIndex == pawn.key.tokenIndex
        ) {
            val elapsed =
                (nowMillis - state.forwardStartedAtMillis)
                    .coerceAtLeast(0L)
            val travelDuration =
                (
                    state.forwardDurationMillis -
                        LudoPaws3DRenderCadencePolicy.FORWARD_LANDING_SETTLE_MILLIS
                ).coerceAtLeast(1L)
            if (state.forwardDurationMillis > 0L && elapsed < travelDuration) {
                // Hold a stable landing pose through the settle tail rather than
                // restarting the next hop cycle after reaching the destination.
                val visualProgress =
                    elapsed.toFloat() /
                        travelDuration.toFloat() *
                        forward.visualSteps.toFloat()
                val stepFraction = visualProgress - floor(visualProgress)
                return MotionFrame(
                    kind = SceneMotion.HOP,
                    progress = stepFraction.coerceIn(0f, 1f),
                )
            }

            if (
                elapsed < state.forwardDurationMillis ||
                forward.toPosition != LudoPathEncoding.HOME_POSITION
            ) {
                return MotionFrame(kind = SceneMotion.IDLE, progress = 0f)
            }

            val celebrationElapsed = elapsed - state.forwardDurationMillis
            val homeDuration = homeDurationMillis(pawn.species)
            if (celebrationElapsed in 0 until homeDuration) {
                return MotionFrame(
                    kind = SceneMotion.HOME,
                    progress =
                        celebrationElapsed.toFloat() / homeDuration.toFloat(),
                )
            }
        }

        val duration = idleDurationMillis(pawn.species)
        val phaseOffset =
            ((pawn.seat * 4 + pawn.key.tokenIndex) * 173L) % duration
        return MotionFrame(
            kind = SceneMotion.IDLE,
            progress =
                ((nowMillis + phaseOffset) % duration)
                    .toFloat() /
                    duration.toFloat(),
        )
    }

    private fun captureReturnVisual(
        player: PlayerSnapshot,
        tokenIndex: Int,
        capture: LudoPaws3DCaptureReturnState,
        cell: Float,
        nowMillis: Long,
    ): CaptureVisual? {
        if (capture.durationMillis <= 0L) return null
        val elapsed =
            (nowMillis - capture.startedAtMillis)
                .coerceAtLeast(0L)
        if (elapsed >= capture.durationMillis) return null

        val slot = tokenIndex + player.seat
        val fromBase =
            LudoPawsFxBoardGeometry.tokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                position = capture.motion.fromPosition,
                cell = cell,
            ) ?: return null
        val toBase =
            LudoPawsFxBoardGeometry.tokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                position = -1,
                cell = cell,
            ) ?: return null
        val fromOffset =
            LudoPawsPawnLayout.tokenOffsetFraction(
                slot = slot,
                position = capture.motion.fromPosition,
            )
        val toOffset =
            LudoPawsPawnLayout.tokenOffsetFraction(
                slot = slot,
                position = -1,
            )
        val from =
            (fromBase.first + fromOffset.first * cell) to
                (fromBase.second + fromOffset.second * cell)
        val to =
            (toBase.first + toOffset.first * cell) to
                (toBase.second + toOffset.second * cell)
        val placement =
            LudoPawsCaptureReturnPlacement.sample(
                from = from,
                to = to,
                cell = cell,
                progress =
                    elapsed.toFloat() /
                        capture.durationMillis.toFloat(),
            )
        return CaptureVisual(
            center = placement.x to placement.y,
            scale = placement.scale,
            fromPosition = capture.motion.fromPosition,
        )
    }

    private fun animatedCenter(
        player: PlayerSnapshot,
        tokenIndex: Int,
        currentPosition: Int,
        cell: Float,
        state: LudoPaws3DSceneState,
        nowMillis: Long,
    ): Pair<Float, Float>? {
        val motion =
            state.forwardMotion
                ?.takeIf {
                    it.playerId == player.playerId &&
                        it.tokenIndex == tokenIndex &&
                        state.forwardDurationMillis > 0L
                }
                ?: return LudoPawsFxBoardGeometry.tokenCenter(
                    color = player.color,
                    tokenIndex = tokenIndex,
                    position = currentPosition,
                    cell = cell,
                )
        val elapsed =
            (nowMillis - state.forwardStartedAtMillis)
                .coerceAtLeast(0L)
        val travelDuration =
            (
                state.forwardDurationMillis -
                    LudoPaws3DRenderCadencePolicy.FORWARD_LANDING_SETTLE_MILLIS
            ).coerceAtLeast(1L)
        if (elapsed >= travelDuration) {
            return LudoPawsFxBoardGeometry.tokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                position = currentPosition,
                cell = cell,
            )
        }

        val totalSteps = motion.visualSteps.coerceAtLeast(1)
        val visualProgress =
            elapsed.toFloat() /
                travelDuration.toFloat() *
                totalSteps.toFloat()
        val whole =
            floor(visualProgress)
                .toInt()
                .coerceIn(0, totalSteps - 1)
        val fraction =
            (visualProgress - whole)
                .coerceIn(0f, 1f)
        val fromPosition =
            LudoPathEncoding.positionAtVisualStep(
                fromPosition = motion.fromPosition,
                step = whole,
            ) ?: return null
        val toPosition =
            LudoPathEncoding.positionAtVisualStep(
                fromPosition = motion.fromPosition,
                step = (whole + 1).coerceAtMost(totalSteps),
            ) ?: return null
        val from =
            LudoPawsFxBoardGeometry.tokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                position = fromPosition,
                cell = cell,
            ) ?: return null
        val to =
            LudoPawsFxBoardGeometry.tokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                position = toPosition,
                cell = cell,
            ) ?: return from

        // Keep the pawn's board-plane route on the cell-center path. A screen-space arc
        // toward the board center changes X/Y and can push top/bottom-road pawns into the
        // neighboring row. The existing 3D hop, scale, and pose animations remain untouched.
        return LudoPawsFxBoardGeometry.interpolateMovementCenter(
            from = from,
            to = to,
            progress = fraction,
        )
    }

    private fun facingYawDegrees(
        player: PlayerSnapshot,
        tokenIndex: Int,
        state: LudoPaws3DSceneState,
        nowMillis: Long,
        size: Float,
        cell: Float,
        turns: Int,
    ): Float {
        val motion =
            state.forwardMotion
                ?.takeIf {
                    it.playerId == player.playerId &&
                        it.tokenIndex == tokenIndex &&
                        state.forwardDurationMillis > 0L
                }
                ?: return LudoPawsPawnFacingPolicy.FRONT_YAW_DEGREES
        val elapsed =
            (nowMillis - state.forwardStartedAtMillis)
                .coerceAtLeast(0L)
        val duration =
            (
                state.forwardDurationMillis -
                    LudoPaws3DRenderCadencePolicy.FORWARD_LANDING_SETTLE_MILLIS
            ).coerceAtLeast(1L)

        // HOME owns its celebratory spin after the final hop. Reset the path-facing
        // contribution there so the species animation remains centered on the player.
        if (
            motion.toPosition == LudoPathEncoding.HOME_POSITION &&
            elapsed >= duration
        ) {
            return LudoPawsPawnFacingPolicy.FRONT_YAW_DEGREES
        }

        val totalSteps = motion.visualSteps.coerceAtLeast(1)
        if (elapsed < duration) {
            val visualProgress =
                elapsed.toFloat() /
                    duration.toFloat() *
                    totalSteps.toFloat()
            val whole =
                floor(visualProgress)
                    .toInt()
                    .coerceIn(0, totalSteps - 1)
            val fraction =
                (visualProgress - whole)
                    .coerceIn(0f, 1f)
            val targetYaw =
                segmentFacingYaw(
                    player = player,
                    tokenIndex = tokenIndex,
                    motion = motion,
                    step = whole,
                    size = size,
                    cell = cell,
                    turns = turns,
                )
                    ?: LudoPawsPawnFacingPolicy.FRONT_YAW_DEGREES
            val previousYaw =
                if (whole > 0) {
                    segmentFacingYaw(
                        player = player,
                        tokenIndex = tokenIndex,
                        motion = motion,
                        step = whole - 1,
                        size = size,
                        cell = cell,
                        turns = turns,
                    ) ?: targetYaw
                } else {
                    LudoPawsPawnFacingPolicy.FRONT_YAW_DEGREES
                }
            return LudoPawsPawnFacingPolicy.movingYaw(
                previousYawDegrees = previousYaw,
                targetYawDegrees = targetYaw,
                stepProgress = fraction,
            )
        }

        val lastTravelYaw =
            segmentFacingYaw(
                player = player,
                tokenIndex = tokenIndex,
                motion = motion,
                step = totalSteps - 1,
                size = size,
                cell = cell,
                turns = turns,
            ) ?: LudoPawsPawnFacingPolicy.FRONT_YAW_DEGREES
        return LudoPawsPawnFacingPolicy.settlingYaw(
            lastTravelYawDegrees = lastTravelYaw,
            elapsedAfterMoveMillis = elapsed - duration,
        )
    }

    private fun segmentFacingYaw(
        player: PlayerSnapshot,
        tokenIndex: Int,
        motion: LudoPawsPawnMotion,
        step: Int,
        size: Float,
        cell: Float,
        turns: Int,
    ): Float? {
        val fromPosition =
            LudoPathEncoding.positionAtVisualStep(
                fromPosition = motion.fromPosition,
                step = step.coerceAtLeast(0),
            ) ?: return null
        val toPosition =
            LudoPathEncoding.positionAtVisualStep(
                fromPosition = motion.fromPosition,
                step = (step + 1).coerceAtMost(motion.visualSteps.coerceAtLeast(1)),
            ) ?: return null
        val from =
            LudoPawsFxBoardGeometry.tokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                position = fromPosition,
                cell = cell,
            ) ?: return null
        val to =
            LudoPawsFxBoardGeometry.tokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                position = toPosition,
                cell = cell,
            ) ?: return null
        val visibleFrom =
            rotatePoint(
                x = from.first,
                y = from.second,
                size = size,
                turns = turns,
            )
        val visibleTo =
            rotatePoint(
                x = to.first,
                y = to.second,
                size = size,
                turns = turns,
            )
        return LudoPawsPawnFacingPolicy.yawForVisibleDelta(
            dx = visibleTo.first - visibleFrom.first,
            dy = visibleTo.second - visibleFrom.second,
        )
    }

    private fun occupancyByCenter(
        snapshot: MatchSnapshot,
        cell: Float,
    ): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        snapshot.players.forEach { player ->
            player.tokens.forEachIndexed { tokenIndex, position ->
                val key =
                    centerKey(
                        LudoPawsFxBoardGeometry.tokenCenter(
                            color = player.color,
                            tokenIndex = tokenIndex,
                            position = position,
                            cell = cell,
                        ),
                    )
                counts[key] = (counts[key] ?: 0) + 1
            }
        }
        return counts
    }

    private fun centerKey(center: Pair<Float, Float>?): String {
        if (center == null) return "missing"
        return "${(center.first * 100f).roundToInt()}:${(center.second * 100f).roundToInt()}"
    }

    private fun rotatePoint(
        x: Float,
        y: Float,
        size: Float,
        turns: Int,
    ): Pair<Float, Float> =
        when (((turns % 4) + 4) % 4) {
            1 -> (size - y) to x
            2 -> (size - x) to (size - y)
            3 -> y to (size - x)
            else -> x to y
        }

    private fun prepareRoot(
        x: Float,
        y: Float,
        scale: Float,
        liftY: Float,
        bodyYawDegrees: Float,
    ) {
        Matrix.setIdentityM(root, 0)
        Matrix.translateM(root, 0, x, y, 0f)
        // Screen Y grows downward. Negative local-Y scale keeps the prototype's
        // positive-Y-up character geometry visually upright on the Android board.
        Matrix.scaleM(root, 0, scale, -scale, scale)
        if (liftY != 0f) {
            Matrix.translateM(root, 0, 0f, liftY, 0f)
        }
        if (bodyYawDegrees != 0f) {
            Matrix.rotateM(root, 0, bodyYawDegrees, 0f, 1f, 0f)
        }
    }

    private fun rootYOffsetPerRadius(
        species: LudoPaws3DSpecies,
    ): Float =
        when (species) {
            // These offsets preserve the old foot baseline after the visual model
            // scale increases from 0.72 to 1.20. Only the body grows upward/outward.
            LudoPaws3DSpecies.DOG -> 0.01f
            LudoPaws3DSpecies.GOAT -> 0.02f
            LudoPaws3DSpecies.DUCK -> 0.14f
            LudoPaws3DSpecies.CAT -> 0.02f
        }

    private fun footBaselinePerRadius(
        species: LudoPaws3DSpecies,
    ): Float =
        when (species) {
            LudoPaws3DSpecies.DOG -> 1.19f
            LudoPaws3DSpecies.GOAT -> 1.17f
            LudoPaws3DSpecies.DUCK -> 0.99f
            LudoPaws3DSpecies.CAT -> 1.18f
        }

    private fun drawContactShadow(pawn: RenderPawn) {
        val visualRadius =
            pawn.radius * pawn.presentationScale
        val shadowY =
            pawn.y +
                pawn.radius * footBaselinePerRadius(pawn.species)

        Matrix.setIdentityM(root, 0)
        Matrix.translateM(
            root,
            0,
            pawn.x,
            shadowY,
            -0.80f,
        )

        // Keep shadows out of the depth buffer. They are board-contact cues only
        // and must never occlude or clip the 3D animal geometry drawn afterwards.
        GLES30.glDepthMask(false)
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0f,
            translateZ = 0f,
            scaleX = visualRadius * 1.70f,
            scaleY = visualRadius * 0.42f,
            scaleZ = 0.02f,
            color = SHADOW_OUTER,
        )
        drawPart(
            parent = root,
            translateX = 0f,
            translateY = 0f,
            translateZ = 0.01f,
            scaleX = visualRadius * 1.45f,
            scaleY = visualRadius * 0.30f,
            scaleZ = 0.02f,
            color = SHADOW_INNER,
        )
        GLES30.glDepthMask(true)
    }

    private fun drawDuck(
        parent: FloatArray,
        pose: Duck3DPose,
        teamColor: LudoPawsTeamColor,
    ) {
        drawPart(parent, -0.25f, -0.61f, 0.18f, 0.23f, 0.095f, 0.30f, DUCK_ORANGE, rotateY = -8f)
        drawPart(parent, 0.25f, -0.61f, 0.18f, 0.23f, 0.095f, 0.30f, DUCK_ORANGE, rotateY = 8f)
        drawPart(parent, 0f, -0.04f, 0f, 0.68f, 0.78f, 0.58f, DUCK_YELLOW)
        drawPart(parent, 0f, -0.14f, 0.47f, 0.40f, 0.48f, 0.12f, teamColor.bellyGlColor)
        drawDuckEye(parent, -0.23f)
        drawDuckEye(parent, 0.23f)
        drawPart(parent, 0f, -0.31f, 0.63f, 0.22f, 0.12f, 0.14f, DUCK_ORANGE)
        drawPart(parent, -0.45f, -0.07f, 0.02f, 0.13f, 0.13f, 0.20f, DUCK_YELLOW, rotateZ = -pose.wingFlapDegrees)
        drawPart(parent, 0.45f, -0.07f, 0.02f, 0.13f, 0.13f, 0.20f, DUCK_YELLOW, rotateZ = pose.wingFlapDegrees)
        drawPart(parent, -0.25f, 0.68f, 0.06f, 0.16f, 0.17f, 0.14f, DUCK_ORANGE)
        drawPart(parent, 0.25f, 0.68f, 0.06f, 0.16f, 0.17f, 0.14f, DUCK_ORANGE)
    }

    private fun drawDuckEye(
        parent: FloatArray,
        x: Float,
    ) {
        drawPart(parent, x, 0.22f, 0.52f, 0.075f, 0.11f, 0.055f, WHITE)
        drawPart(parent, x, 0.22f, 0.575f, 0.028f, 0.055f, 0.025f, EYE_DARK)
    }

    private fun drawDog(
        parent: FloatArray,
        pose: Dog3DPose,
        teamColor: LudoPawsTeamColor,
    ) {
        drawPart(parent, 0f, 0.03f, -0.08f, 0.62f, 0.67f, 0.53f, DOG_TAN)
        drawPart(parent, 0f, 0.32f, 0.34f, 0.43f, 0.42f, 0.36f, DOG_CREAM)
        drawPart(parent, 0f, 0.49f, 0.59f, 0.21f, 0.14f, 0.13f, NOSE_DARK)
        drawPart(parent, -0.31f, 0.57f, 0.37f, 0.13f, 0.24f, 0.13f, DOG_EAR_BROWN, rotateZ = -pose.earBounceDegrees - pose.headTiltDegrees)
        drawPart(parent, 0.31f, 0.57f, 0.37f, 0.13f, 0.24f, 0.13f, DOG_EAR_BROWN, rotateZ = pose.earBounceDegrees - pose.headTiltDegrees)
        drawPart(parent, -0.22f, -0.48f, 0.35f, 0.16f, 0.10f, 0.26f, DOG_TAN, rotateX = -pose.bodyYawDegrees * 0.1f)
        drawPart(parent, 0.22f, -0.48f, 0.35f, 0.16f, 0.10f, 0.26f, DOG_TAN, rotateX = pose.bodyYawDegrees * 0.1f)
        drawDogLeg(parent, -0.38f, -0.62f)
        drawDogLeg(parent, 0.38f, -0.62f)
        drawPart(parent, 0f, 0.13f, -0.49f, 0.13f, 0.14f, 0.45f, DOG_TAN, rotateX = pose.tailWagDegrees)
        drawPart(parent, 0f, -0.22f, 0.55f, 0.18f, 0.10f, 0.08f, TONGUE_PINK)
        drawPart(parent, -0.19f, 0.38f, 0.66f, 0.035f, 0.060f, 0.030f, EYE_DARK)
        drawPart(parent, 0.19f, 0.38f, 0.66f, 0.035f, 0.060f, 0.030f, EYE_DARK)
    }

    private fun drawDogLeg(parent: FloatArray, x: Float, y: Float) {
        drawPart(parent, x, y, 0.05f, 0.13f, 0.28f, 0.13f, DOG_TAN)
        drawPart(parent, x, y - 0.17f, 0.17f, 0.15f, 0.09f, 0.18f, DOG_CREAM)
    }

    private fun drawDogEye(
        parent: FloatArray,
        x: Float,
    ) {
        drawPart(parent, x, 0.22f, 0.52f, 0.075f, 0.11f, 0.055f, WHITE)
        drawPart(parent, x, 0.22f, 0.575f, 0.028f, 0.055f, 0.025f, EYE_DARK)
    }

    private fun drawGoat(
        parent: FloatArray,
        pose: Goat3DPose,
        teamColor: LudoPawsTeamColor,
    ) {
        drawPart(parent, 0f, 0.02f, -0.06f, 0.59f, 0.65f, 0.50f, GOAT_IVORY)
        drawPart(parent, 0f, 0.34f, 0.33f, 0.43f, 0.42f, 0.35f, GOAT_CREAM)
        drawPart(parent, 0f, 0.49f, 0.57f, 0.18f, 0.16f, 0.12f, GOAT_MUZZLE)
        drawPart(parent, -0.24f, 0.60f, 0.30f, 0.09f, 0.23f, 0.09f, GOAT_HORN, rotateZ = -15f)
        drawPart(parent, 0.24f, 0.60f, 0.30f, 0.09f, 0.23f, 0.09f, GOAT_HORN, rotateZ = 15f)
        drawPart(parent, -0.38f, 0.43f, 0.20f, 0.20f, 0.12f, 0.10f, GOAT_EAR_TAN, rotateZ = -pose.earFlickDegrees)
        drawPart(parent, 0.38f, 0.43f, 0.20f, 0.20f, 0.12f, 0.10f, GOAT_EAR_TAN, rotateZ = pose.earFlickDegrees)
        drawPart(parent, 0f, 0.14f, 0.53f, 0.12f, 0.20f, 0.08f, GOAT_BEARD, rotateX = pose.beardSwingDegrees)
        drawPart(parent, -0.22f, -0.49f, 0.34f, 0.12f, 0.09f, 0.18f, GOAT_HOOF_DARK)
        drawPart(parent, 0.22f, -0.49f, 0.34f, 0.12f, 0.09f, 0.18f, GOAT_HOOF_DARK)
        drawPart(parent, -0.18f, -0.35f, 0.60f, 0.03f, 0.05f, 0.03f, EYE_DARK)
        drawPart(parent, 0.18f, -0.35f, 0.60f, 0.03f, 0.05f, 0.03f, EYE_DARK)
        drawPart(parent, 0f, 0.03f, -0.48f, 0.10f, 0.12f, 0.30f, GOAT_WARM_GRAY, rotateX = pose.tailFlickDegrees)
    }

    private fun drawGoatLeg(parent: FloatArray, x: Float, y: Float) {
        drawPart(parent, x, y, 0.02f, 0.11f, 0.26f, 0.11f, GOAT_IVORY)
        drawPart(parent, x, y - 0.16f, 0.12f, 0.12f, 0.08f, 0.14f, GOAT_HOOF_DARK)
    }

    private fun drawGoatHorn(
        parent: FloatArray,
        x: Float,
        direction: Float,
    ) {
        drawPart(parent, x, 0.62f, 0.28f, 0.075f, 0.18f, 0.075f, GOAT_HORN, rotateZ = 18f * direction)
        drawPart(parent, x + 0.04f * direction, 0.76f, 0.30f, 0.055f, 0.09f, 0.055f, GOAT_HORN_TIP)
    }

    private fun drawGoatEye(
        parent: FloatArray,
        x: Float,
    ) {
        drawPart(parent, x, 0.22f, 0.52f, 0.075f, 0.11f, 0.055f, WHITE)
        drawPart(parent, x, 0.22f, 0.575f, 0.028f, 0.055f, 0.025f, EYE_DARK)
    }

    private fun drawCat(
        parent: FloatArray,
        pose: Cat3DPose,
        teamColor: LudoPawsTeamColor,
    ) {
        drawPart(parent, 0f, 0.03f, -0.07f, 0.60f, 0.66f, 0.50f, CAT_SILVER)
        drawPart(parent, 0f, 0.34f, 0.33f, 0.42f, 0.40f, 0.35f, CAT_CREAM)
        drawPart(parent, 0f, 0.49f, 0.57f, 0.16f, 0.15f, 0.12f, CAT_NOSE_PINK)
        drawCatEar(parent, -0.27f, -1f, pose.earTwitchDegrees, pose.headTiltDegrees)
        drawCatEar(parent, 0.27f, 1f, pose.earTwitchDegrees, pose.headTiltDegrees)
        drawPart(parent, -0.22f, -0.49f, 0.34f, 0.12f, 0.09f, 0.18f, CAT_SILVER)
        drawPart(parent, 0.22f, -0.49f, 0.34f, 0.12f, 0.09f, 0.18f, CAT_SILVER)
        drawPart(parent, 0f, 0.14f, -0.52f, 0.12f, 0.12f, 0.34f, CAT_SILVER, rotateX = pose.tailSwayDegrees)
        drawCatEye(parent, -0.18f)
        drawCatEye(parent, 0.18f)
        drawPart(parent, -0.08f, 0.12f, 0.69f, 0.018f, 0.31f, 0.018f, CAT_WHISKER, rotateZ = -66f)
        drawPart(parent, 0.08f, 0.12f, 0.69f, 0.018f, 0.31f, 0.018f, CAT_WHISKER, rotateZ = 66f)
    }

    private fun drawCatLeg(parent: FloatArray, x: Float, y: Float) {
        drawPart(parent, x, y, 0.02f, 0.12f, 0.28f, 0.12f, CAT_SILVER)
        drawPart(parent, x, y - 0.17f, 0.14f, 0.14f, 0.08f, 0.15f, CAT_CREAM)
    }

    private fun drawCatEye(
        parent: FloatArray,
        x: Float,
    ) {
        drawPart(parent, x, 0.22f, 0.52f, 0.075f, 0.11f, 0.055f, WHITE)
        drawPart(parent, x, 0.22f, 0.575f, 0.028f, 0.055f, 0.025f, CAT_PUPIL_DARK)
    }

    private fun drawCatEar(
        parent: FloatArray,
        x: Float,
        side: Float,
        earTwitchDegrees: Float,
        headTiltDegrees: Float,
    ) {
        val angle = 16f * side + earTwitchDegrees * side - headTiltDegrees * 0.35f
        drawPart(parent, x, 0.73f, 0.28f, 0.13f, 0.22f, 0.08f, CAT_SILVER, rotateZ = angle)
        drawPart(parent, x, 0.74f, 0.345f, 0.07f, 0.15f, 0.025f, CAT_EAR_PINK, rotateZ = angle)
    }

    private fun drawCatWhiskers(
        parent: FloatArray,
        side: Float,
    ) {
        val x = 0.34f * side
        drawPart(parent, x, 0.12f, 0.68f, 0.018f, 0.31f, 0.018f, CAT_WHISKER, rotateZ = 66f * side)
    }

    private fun drawPart(
        parent: FloatArray,
        translateX: Float,
        translateY: Float,
        translateZ: Float,
        scaleX: Float,
        scaleY: Float,
        scaleZ: Float,
        color: FloatArray,
        rotateX: Float = 0f,
        rotateY: Float = 0f,
        rotateZ: Float = 0f,
    ) {
        System.arraycopy(parent, 0, model, 0, 16)
        Matrix.translateM(
            model,
            0,
            translateX,
            translateY,
            translateZ,
        )
        if (rotateX != 0f) Matrix.rotateM(model, 0, rotateX, 1f, 0f, 0f)
        if (rotateY != 0f) Matrix.rotateM(model, 0, rotateY, 0f, 1f, 0f)
        if (rotateZ != 0f) Matrix.rotateM(model, 0, rotateZ, 0f, 0f, 1f)
        Matrix.scaleM(
            model,
            0,
            scaleX,
            scaleY,
            scaleZ,
        )
        Matrix.multiplyMM(
            mvp,
            0,
            projection,
            0,
            model,
            0,
        )
        check(Matrix.invertM(inverseModel, 0, model, 0)) {
            "3D pawn model matrix is not invertible"
        }
        normalMatrix[0] = inverseModel[0]
        normalMatrix[1] = inverseModel[4]
        normalMatrix[2] = inverseModel[8]
        normalMatrix[3] = inverseModel[1]
        normalMatrix[4] = inverseModel[5]
        normalMatrix[5] = inverseModel[9]
        normalMatrix[6] = inverseModel[2]
        normalMatrix[7] = inverseModel[6]
        normalMatrix[8] = inverseModel[10]
        sphere.draw(
            model = model,
            normalMatrix = normalMatrix,
            mvp = mvp,
            color = color,
        )
    }

    private fun lerp(
        start: Float,
        end: Float,
        progress: Float,
    ): Float =
        start + (end - start) * progress.coerceIn(0f, 1f)

    private fun smoothStep(
        progress: Float,
    ): Float {
        val t = progress.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun idleDurationMillis(
        species: LudoPaws3DSpecies,
    ): Long =
        when (species) {
            LudoPaws3DSpecies.DOG -> Dog3DMotionTimeline.IDLE_DURATION_MILLIS
            LudoPaws3DSpecies.GOAT -> Goat3DMotionTimeline.IDLE_DURATION_MILLIS
            LudoPaws3DSpecies.DUCK -> Duck3DMotionTimeline.IDLE_DURATION_MILLIS
            LudoPaws3DSpecies.CAT -> Cat3DMotionTimeline.IDLE_DURATION_MILLIS
        }

    private fun homeDurationMillis(
        species: LudoPaws3DSpecies,
    ): Long =
        when (species) {
            LudoPaws3DSpecies.DOG -> Dog3DMotionTimeline.HOME_DURATION_MILLIS
            LudoPaws3DSpecies.GOAT -> Goat3DMotionTimeline.HOME_DURATION_MILLIS
            LudoPaws3DSpecies.DUCK -> Duck3DMotionTimeline.HOME_DURATION_MILLIS
            LudoPaws3DSpecies.CAT -> Cat3DMotionTimeline.HOME_DURATION_MILLIS
        }

    private fun createProgram(
        vertexSource: String,
        fragmentSource: String,
    ): Int {
        val vertex =
            compileShader(
                GLES30.GL_VERTEX_SHADER,
                vertexSource,
            )
        val fragment =
            compileShader(
                GLES30.GL_FRAGMENT_SHADER,
                fragmentSource,
            )
        return GLES30.glCreateProgram().also { created ->
            GLES30.glAttachShader(created, vertex)
            GLES30.glAttachShader(created, fragment)
            GLES30.glLinkProgram(created)
            val status = IntArray(1)
            GLES30.glGetProgramiv(
                created,
                GLES30.GL_LINK_STATUS,
                status,
                0,
            )
            check(status[0] == GLES30.GL_TRUE) {
                "3D pawn program link failed: ${GLES30.glGetProgramInfoLog(created)}"
            }
            GLES30.glDeleteShader(vertex)
            GLES30.glDeleteShader(fragment)
        }
    }

    private fun compileShader(
        type: Int,
        source: String,
    ): Int =
        GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)
            val status = IntArray(1)
            GLES30.glGetShaderiv(
                shader,
                GLES30.GL_COMPILE_STATUS,
                status,
                0,
            )
            check(status[0] == GLES30.GL_TRUE) {
                "3D pawn shader compile failed: ${GLES30.glGetShaderInfoLog(shader)}"
            }
        }

    private fun legalAttentionScale(
        state: LudoPaws3DSceneState,
        key: LudoPaws3DPawnKey,
        nowMillis: Long,
    ): Float {
        val snapshot = state.snapshot ?: return 1f
        val pending = snapshot.pendingRoll ?: return 1f
        if (pending.status != "RESOLVED") return 1f
        if (state.localPlayerId != key.playerId) return 1f
        val player =
            snapshot.players.firstOrNull {
                it.playerId == key.playerId
            } ?: return 1f
        if (pending.seat != player.seat) return 1f
        if (key.tokenIndex !in pending.legalTokenIndexes) return 1f
        if (state.forwardMotion != null || state.captureReturns.isNotEmpty()) return 1f

        val cycleMillis = 900L
        val phase =
            (
                nowMillis % cycleMillis
            ).toFloat() /
                cycleMillis.toFloat()
        val breath =
            (
                sin(
                    phase *
                        PI.toFloat() *
                        2f,
                ) +
                    1f
                ) *
                .5f
        return 1f + breath * .0975f
    }

    private data class RenderPawn(
        val key: LudoPaws3DPawnKey,
        val species: LudoPaws3DSpecies,
        val teamColor: LudoPawsTeamColor,
        val seat: Int,
        val x: Float,
        val y: Float,
        val radius: Float,
        val facingYawDegrees: Float = LudoPawsPawnFacingPolicy.FRONT_YAW_DEGREES,
        val presentationScale: Float = 1f,
        val attentionScale: Float = 1f,
    )

    private data class CaptureVisual(
        val center: Pair<Float, Float>,
        val scale: Float,
        val fromPosition: Int,
    )

    private enum class SceneMotion {
        IDLE,
        HOP,
        HOME,
    }

    private data class MotionFrame(
        val kind: SceneMotion,
        val progress: Float,
    )

    private class SphereMesh(
        latitudeSegments: Int,
        longitudeSegments: Int,
        private val uModelLocation: Int,
        private val uMvpLocation: Int,
        private val uColorLocation: Int,
        private val uLightDirectionLocation: Int,
        private val uNormalMatrixLocation: Int,
    ) {
        private val vertexBuffer: FloatBuffer
        private val indexBuffer: ShortBuffer
        private val indexCount: Int

        init {
            val vertices = mutableListOf<Float>()
            val indices = mutableListOf<Short>()

            for (lat in 0..latitudeSegments) {
                val theta =
                    PI * lat.toDouble() / latitudeSegments.toDouble()
                val sinTheta = sin(theta).toFloat()
                val cosTheta = cos(theta).toFloat()
                for (lon in 0..longitudeSegments) {
                    val phi =
                        2.0 * PI * lon.toDouble() / longitudeSegments.toDouble()
                    val x = sinTheta * cos(phi).toFloat()
                    val y = cosTheta
                    val z = sinTheta * sin(phi).toFloat()
                    vertices += x
                    vertices += y
                    vertices += z
                    vertices += x
                    vertices += y
                    vertices += z
                }
            }

            val row = longitudeSegments + 1
            for (lat in 0 until latitudeSegments) {
                for (lon in 0 until longitudeSegments) {
                    val first = lat * row + lon
                    val second = first + row
                    indices += first.toShort()
                    indices += second.toShort()
                    indices += (first + 1).toShort()
                    indices += second.toShort()
                    indices += (second + 1).toShort()
                    indices += (first + 1).toShort()
                }
            }

            vertexBuffer =
                ByteBuffer
                    .allocateDirect(vertices.size * Float.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .apply {
                        vertices.forEach(::put)
                        position(0)
                    }
            indexBuffer =
                ByteBuffer
                    .allocateDirect(indices.size * Short.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .apply {
                        indices.forEach(::put)
                        position(0)
                    }
            indexCount = indices.size
        }

        fun draw(
            model: FloatArray,
            normalMatrix: FloatArray,
            mvp: FloatArray,
            color: FloatArray,
        ) {
            GLES30.glUniformMatrix4fv(
                uModelLocation,
                1,
                false,
                model,
                0,
            )
            GLES30.glUniformMatrix4fv(
                uMvpLocation,
                1,
                false,
                mvp,
                0,
            )
            GLES30.glUniformMatrix3fv(
                uNormalMatrixLocation,
                1,
                false,
                normalMatrix,
                0,
            )
            GLES30.glUniform4fv(
                uColorLocation,
                1,
                color,
                0,
            )
            GLES30.glUniform3f(
                uLightDirectionLocation,
                0.40f,
                -1.0f,
                -0.65f,
            )

            vertexBuffer.position(0)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(
                0,
                3,
                GLES30.GL_FLOAT,
                false,
                6 * Float.SIZE_BYTES,
                vertexBuffer,
            )
            vertexBuffer.position(3)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(
                1,
                3,
                GLES30.GL_FLOAT,
                false,
                6 * Float.SIZE_BYTES,
                vertexBuffer,
            )
            indexBuffer.position(0)
            GLES30.glDrawElements(
                GLES30.GL_TRIANGLES,
                indexCount,
                GLES30.GL_UNSIGNED_SHORT,
                indexBuffer,
            )
            GLES30.glDisableVertexAttribArray(0)
            GLES30.glDisableVertexAttribArray(1)
        }
    }

    private companion object {
        // 0.72 -> 1.20 is a 1.67x visual enlargement. Logical radius, path,
        // stacking, hit testing and animation timing stay exactly as before.
        const val MODEL_SCALE_PER_RADIUS = 1.20f

        val SHADOW_OUTER = floatArrayOf(0.025f, 0.030f, 0.040f, 0.07f)
        val SHADOW_INNER = floatArrayOf(0.020f, 0.025f, 0.035f, 0.16f)

        val WHITE = floatArrayOf(1f, 1f, 1f, 1f)
        val EYE_DARK = floatArrayOf(0.025f, 0.030f, 0.035f, 1f)
        val NOSE_DARK = floatArrayOf(0.05f, 0.04f, 0.04f, 1f)
        val TAG_GOLD = floatArrayOf(0.97f, 0.72f, 0.11f, 1f)

        val DUCK_YELLOW = floatArrayOf(1.00f, 0.78f, 0.08f, 1f)
        val DUCK_BELLY = floatArrayOf(1.00f, 0.88f, 0.34f, 1f)
        val DUCK_ORANGE = floatArrayOf(1.00f, 0.42f, 0.03f, 1f)

        val DOG_TAN = floatArrayOf(0.78f, 0.40f, 0.16f, 1f)
        val DOG_CREAM = floatArrayOf(0.98f, 0.84f, 0.65f, 1f)
        val DOG_EAR_BROWN = floatArrayOf(0.39f, 0.16f, 0.07f, 1f)
        val TONGUE_PINK = floatArrayOf(1.00f, 0.35f, 0.46f, 1f)

        val GOAT_IVORY = floatArrayOf(0.88f, 0.86f, 0.78f, 1f)
        val GOAT_CREAM = floatArrayOf(0.96f, 0.93f, 0.82f, 1f)
        val GOAT_WARM_GRAY = floatArrayOf(0.58f, 0.55f, 0.49f, 1f)
        val GOAT_MUZZLE = floatArrayOf(0.92f, 0.88f, 0.75f, 1f)
        val GOAT_EAR_TAN = floatArrayOf(0.66f, 0.58f, 0.47f, 1f)
        val GOAT_BEARD = floatArrayOf(0.70f, 0.68f, 0.62f, 1f)
        val GOAT_HORN = floatArrayOf(0.76f, 0.69f, 0.54f, 1f)
        val GOAT_HORN_TIP = floatArrayOf(0.55f, 0.49f, 0.38f, 1f)
        val GOAT_HOOF_DARK = floatArrayOf(0.14f, 0.13f, 0.12f, 1f)

        val CAT_SILVER = floatArrayOf(0.60f, 0.63f, 0.68f, 1f)
        val CAT_CREAM = floatArrayOf(0.90f, 0.87f, 0.80f, 1f)
        val CAT_STRIPE_DARK = floatArrayOf(0.25f, 0.28f, 0.33f, 1f)
        val CAT_NOSE_PINK = floatArrayOf(0.95f, 0.49f, 0.56f, 1f)
        val CAT_EAR_PINK = floatArrayOf(0.91f, 0.55f, 0.61f, 1f)
        val CAT_EYE_GREEN = floatArrayOf(0.45f, 0.78f, 0.48f, 1f)
        val CAT_PUPIL_DARK = floatArrayOf(0.02f, 0.025f, 0.03f, 1f)
        val CAT_WHISKER = floatArrayOf(0.92f, 0.92f, 0.92f, 1f)

        const val VERTEX_SHADER = """
            #version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aNormal;
            uniform mat4 uModel;
            uniform mat4 uMvp;
            uniform mat3 uNormalMatrix;
            out vec3 vNormal;
            void main() {
                vNormal = normalize(uNormalMatrix * aNormal);
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        const val FRAGMENT_SHADER = """
            #version 300 es
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLightDirection;
            in vec3 vNormal;
            out vec4 outColor;
            void main() {
                vec3 normal = normalize(vNormal);
                vec3 light = normalize(-uLightDirection);
                vec3 viewDir = vec3(0.0, 0.0, 1.0);
                vec3 halfDir = normalize(light + viewDir);
                float diffuse = max(dot(normal, light), 0.0);
                float specular = pow(max(dot(normal, halfDir), 0.0), 28.0) * 0.18;
                float rim = pow(1.0 - max(dot(normal, viewDir), 0.0), 2.2) * 0.08;
                vec3 rgb = uColor.rgb * (0.36 + diffuse * 0.64) + vec3((specular + rim) * uColor.a);
                outColor = vec4(rgb, uColor.a);
            }
        """
    }
}
