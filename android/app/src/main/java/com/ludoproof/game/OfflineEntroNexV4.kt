package com.ludoproof.game

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.sqrt

internal const val OFFLINE_ENTRONEX_V4_ALGORITHM =
    "entronex-v4-dual-commit-hkdf-sha256-context-bound"

internal data class OfflineV4WorldConfig(
    val cellsPerOutcome: Int = 16,
    val timelineTicks: Int = 512,
    val epochCount: Int = 8,
    val probeCount: Int = 3,
)

internal data class OfflineV4Resolution(
    val roundId: String,
    val outcome: Int,
    val outcomeIndex: Int,
    val transcriptDigest: String,
    val proofDigest: String,
    val worldDigest: String,
    val fieldDigest: String,
    val serverCommitment: String,
    val clientCommitment: String,
)

internal object OfflineEntroNexV4 {
    private val secureRandom =
        SecureRandom()

    private val outcomes =
        listOf(
            1,
            2,
            3,
            4,
            5,
            6,
        )

    private val onlineWorldConfig =
        OfflineV4WorldConfig(
            cellsPerOutcome = 16,
            timelineTicks = 512,
            epochCount = 8,
            probeCount = 3,
        )

    fun actorHash(
        playerId: String,
    ): String =
        sha256Hex(
            "ludoproof:actor:v1:" +
                playerId,
        )

    fun rulesetHash(): String =
        sha256Hex(
            "entronex:v4:game-ruleset:" +
                canonicalJson(
                    linkedMapOf(
                        "id" to
                            "ludoproof-standard-v1",
                        "boardTrackCells" to
                            52,
                        "homePosition" to
                            57,
                        "tokensPerPlayer" to
                            4,
                        "startOffsets" to
                            linkedMapOf(
                                "RED" to 0,
                                "GREEN" to 13,
                                "YELLOW" to 26,
                                "BLUE" to 39,
                            ),
                        "safeGlobalCells" to
                            listOf(
                                0,
                                8,
                                13,
                                21,
                                26,
                                34,
                                39,
                                47,
                            ),
                        "leaveYardRequiresSix" to
                            true,
                        "exactRollToHome" to
                            true,
                        "extraTurnOnSix" to
                            true,
                        "extraTurnOnCapture" to
                            true,
                        "threeConsecutiveSixesForfeit" to
                            true,
                        "captureOnSafeCell" to
                            false,
                    ),
                ),
        )

    fun gameStateHash(
        state: Any?,
    ): String =
        sha256Hex(
            "entronex:v4:game-state:" +
                canonicalJson(
                    state,
                ),
        )

    fun runLudoRoll(
        sessionId: String,
        eventIndex: Int,
        actorHash: String,
        previousStateHash: String,
        rulesetHash: String,
    ): OfflineV4Resolution {
        require(
            sessionId.isNotBlank(),
        ) {
            "offline session id is required"
        }
        require(
            eventIndex >= 0,
        ) {
            "eventIndex must be non-negative"
        }
        requireDigest(
            actorHash,
            "actorHash",
        )
        requireDigest(
            previousStateHash,
            "previousStateHash",
        )
        requireDigest(
            rulesetHash,
            "rulesetHash",
        )

        val serverSeed =
            randomSeedHex()
        val clientSeed =
            randomSeedHex()
        val roundId =
            "offline-v4-" +
                randomHex(
                    16,
                )

        val context =
            linkedMapOf<String, Any?>(
                "applicationId" to
                    "ludoproof",
                "sessionId" to
                    sessionId,
                "eventId" to
                    "roll:" +
                    eventIndex,
                "eventType" to
                    "DICE_ROLL",
                "eventIndex" to
                    eventIndex,
                "subjectHash" to
                    actorHash,
                "previousStateHash" to
                    previousStateHash,
                "metadataDigest" to
                    rulesetHash,
            )

        return resolve(
            roundId =
                roundId,
            serverSeed =
                serverSeed,
            clientSeed =
                clientSeed,
            context =
                context,
            worldConfig =
                onlineWorldConfig,
        )
    }

    internal fun resolveForConformance(
        roundId: String,
        serverSeed: String,
        clientSeed: String,
        context: Map<String, Any?>,
        worldConfig: OfflineV4WorldConfig,
    ): OfflineV4Resolution =
        resolve(
            roundId =
                roundId,
            serverSeed =
                serverSeed,
            clientSeed =
                clientSeed,
            context =
                context,
            worldConfig =
                worldConfig,
        )

    private fun resolve(
        roundId: String,
        serverSeed: String,
        clientSeed: String,
        context: Map<String, Any?>,
        worldConfig: OfflineV4WorldConfig,
    ): OfflineV4Resolution {
        require(
            roundId.isNotBlank(),
        ) {
            "roundId is required"
        }
        val normalizedServerSeed =
            validateSeed(
                serverSeed,
                "server seed",
            )
        val normalizedClientSeed =
            validateSeed(
                clientSeed,
                "client seed",
            )
        validateContext(
            context,
        )
        validateWorldConfig(
            worldConfig,
        )

        val serverCommitment =
            sha256Hex(
                "entronex:v4:server-commit:" +
                    normalizedServerSeed,
            )
        val clientCommitment =
            sha256Hex(
                "entronex:v4:client-commit:" +
                    normalizedClientSeed,
            )

        val config =
            linkedMapOf<String, Any?>(
                "outcomes" to
                    outcomes,
                "context" to
                    context,
                "world" to
                    worldConfigMap(
                        worldConfig,
                    ),
            )

        val contextDigest =
            sha256Hex(
                "entronex:v4:context:" +
                    canonicalJson(
                        context,
                    ),
            )
        val eventBinding =
            linkedMapOf<String, Any?>(
                "applicationId" to
                    context[
                        "applicationId"
                    ],
                "sessionId" to
                    context[
                        "sessionId"
                    ],
                "eventType" to
                    context[
                        "eventType"
                    ],
                "eventIndex" to
                    context[
                        "eventIndex"
                    ],
            )
        val eventBindingDigest =
            sha256Hex(
                "entronex:v4:event-binding:" +
                    canonicalJson(
                        eventBinding,
                    ),
            )
        val configDigest =
            sha256Hex(
                "entronex:v4:config:" +
                    canonicalJson(
                        config,
                    ),
            )

        val transcript =
            linkedMapOf<String, Any?>(
                "algorithm" to
                    OFFLINE_ENTRONEX_V4_ALGORITHM,
                "roundId" to
                    roundId,
                "serverCommitment" to
                    serverCommitment,
                "clientCommitment" to
                    clientCommitment,
                "configDigest" to
                    configDigest,
                "contextDigest" to
                    contextDigest,
                "eventBindingDigest" to
                    eventBindingDigest,
            )
        val transcriptDigest =
            sha256Hex(
                "entronex:v4:transcript:" +
                    canonicalJson(
                        transcript,
                    ),
            )

        val outcomeKey =
            deriveV4Key(
                normalizedServerSeed,
                normalizedClientSeed,
                transcriptDigest,
                "outcome",
            )
        val outcomeIndex =
            HmacStream(
                outcomeKey,
                "direct-outcome-selection",
            ).uniformInt(
                outcomes.size,
            )
        val outcome =
            outcomes[
                outcomeIndex
            ]

        val worldKey =
            deriveV4Key(
                normalizedServerSeed,
                normalizedClientSeed,
                transcriptDigest,
                "natural-world",
            )
        val world =
            NaturalWorld.sample(
                outcomes =
                    outcomes,
                outcome =
                    outcome,
                key =
                    worldKey,
                config =
                    worldConfig,
            )

        val core =
            linkedMapOf<String, Any?>(
                "algorithm" to
                    OFFLINE_ENTRONEX_V4_ALGORITHM,
                "roundId" to
                    roundId,
                "serverCommitment" to
                    serverCommitment,
                "serverSeed" to
                    normalizedServerSeed,
                "clientCommitment" to
                    clientCommitment,
                "clientSeed" to
                    normalizedClientSeed,
                "configDigest" to
                    configDigest,
                "contextDigest" to
                    contextDigest,
                "eventBindingDigest" to
                    eventBindingDigest,
                "transcriptDigest" to
                    transcriptDigest,
                "config" to
                    config,
                "outcomeIndex" to
                    outcomeIndex,
                "outcome" to
                    outcome,
                "world" to
                    world.manifest,
            )
        val proofDigest =
            sha256Hex(
                "entronex:v4:proof:" +
                    canonicalJson(
                        core,
                    ),
            )

        return OfflineV4Resolution(
            roundId =
                roundId,
            outcome =
                outcome,
            outcomeIndex =
                outcomeIndex,
            transcriptDigest =
                transcriptDigest,
            proofDigest =
                proofDigest,
            worldDigest =
                world.worldDigest,
            fieldDigest =
                world.fieldDigest,
            serverCommitment =
                serverCommitment,
            clientCommitment =
                clientCommitment,
        )
    }

    private fun deriveV4Key(
        serverSeed: String,
        clientSeed: String,
        transcriptDigest: String,
        label: String,
    ): ByteArray =
        hkdfSha256(
            ikm =
                serverSeed.hexToBytes() +
                    clientSeed.hexToBytes(),
            salt =
                transcriptDigest
                    .hexToBytes(),
            info =
                (
                    "entronex:v4:" +
                        label
                    ).toByteArray(
                    Charsets.UTF_8,
                ),
            length =
                32,
        )

    private fun validateContext(
        context: Map<String, Any?>,
    ) {
        for (
            field in
            listOf(
                "applicationId",
                "sessionId",
                "eventId",
                "eventType",
            )
        ) {
            require(
                (
                    context[
                        field
                    ] as? String
                    )
                    ?.isNotEmpty() ==
                    true,
            ) {
                "$field must be a non-empty string"
            }
        }

        val eventIndex =
            (
                context[
                    "eventIndex"
                ] as? Number
                )
                ?.toLong()
                ?: error(
                    "eventIndex must be an integer",
                )
        require(
            eventIndex >= 0 &&
                eventIndex <=
                JS_SAFE_INTEGER_MAX,
        ) {
            "eventIndex must be a non-negative safe integer"
        }

        for (
            field in
            listOf(
                "subjectHash",
                "previousStateHash",
                "metadataDigest",
            )
        ) {
            val value =
                context[
                    field
                ]
            if (
                value != null
            ) {
                requireDigest(
                    value as String,
                    field,
                )
            }
        }

        canonicalJson(
            context,
        )
    }

    private fun validateWorldConfig(
        config: OfflineV4WorldConfig,
    ) {
        require(
            config.cellsPerOutcome in
                1..4_096,
        )
        require(
            outcomes.size *
                config.cellsPerOutcome <=
                65_536,
        )
        require(
            config.timelineTicks in
                256..16_384,
        )
        require(
            config.epochCount in
                2..128,
        )
        require(
            config.probeCount in
                1..8,
        )
        require(
            config.epochCount <=
                config.timelineTicks,
        )
    }

    private fun worldConfigMap(
        config: OfflineV4WorldConfig,
    ): Map<String, Any?> =
        linkedMapOf(
            "cellsPerOutcome" to
                config.cellsPerOutcome,
            "timelineTicks" to
                config.timelineTicks,
            "epochCount" to
                config.epochCount,
            "probeCount" to
                config.probeCount,
        )

    private data class NaturalWorldResult(
        val manifest:
            Map<String, Any?>,
        val worldDigest:
            String,
        val fieldDigest:
            String,
    )

    private object NaturalWorld {
        private val motionProfiles =
            listOf(
                "crosswind",
                "orbital-drift",
                "pulse-reverse",
                "toroidal-flow",
                "scatter-wave",
            )

        fun sample(
            outcomes: List<Int>,
            outcome: Int,
            key: ByteArray,
            config:
                OfflineV4WorldConfig,
        ): NaturalWorldResult {
            val fieldSize =
                outcomes.size *
                    config
                        .cellsPerOutcome
            require(
                fieldSize <=
                    65_536,
            ) {
                "v4 natural world is too large"
            }
            require(
                key.size == 32,
            ) {
                "natural-world key must contain 32 bytes"
            }
            require(
                outcome in
                    outcomes,
            )

            val dimensions =
                nearSquareDimensions(
                    fieldSize,
                )
            val width =
                dimensions.first
            val height =
                dimensions.second

            val sampleTick =
                HmacStream(
                    key,
                    "timeline",
                ).uniformInt(
                    config
                        .timelineTicks,
                )
            val layoutEpoch =
                minOf(
                    config.epochCount -
                        1,
                    (
                        sampleTick
                            .toLong() *
                            config
                                .epochCount
                                .toLong() /
                            config
                                .timelineTicks
                                .toLong()
                        ).toInt(),
                )

            val motionProfile =
                motionProfiles[
                    HmacStream(
                        key,
                        "motion-profile",
                    ).uniformInt(
                        motionProfiles
                            .size,
                    )
                ]

            val probes =
                mutableListOf<
                    Map<String, Any?>
                    >()
            repeat(
                config.probeCount,
            ) {
                    probe ->
                probes +=
                    createProbe(
                        key =
                            key,
                        probe =
                            probe,
                        width =
                            width,
                        height =
                            height,
                        timelineTicks =
                            config
                                .timelineTicks,
                        sampleTick =
                            sampleTick,
                    )
            }

            val selectedProbe =
                HmacStream(
                    key,
                    "probe-selector",
                ).uniformInt(
                    config.probeCount,
                )
            val selected =
                probes[
                    selectedProbe
                ]

            val motion =
                HmacStream(
                    key,
                    "logical-motion:" +
                        layoutEpoch +
                        ":" +
                        sampleTick,
                )
            val driftX =
                motion.uniformInt(
                    width,
                )
            val driftY =
                motion.uniformInt(
                    height,
                )
            val mirrorX =
                motion.uniformInt(
                    2,
                ) == 1
            val mirrorY =
                motion.uniformInt(
                    2,
                ) == 1

            var sourceX =
                mod(
                    requireInt(
                        selected,
                        "sampleX",
                    ) -
                        driftX,
                    width,
                )
            var sourceY =
                mod(
                    requireInt(
                        selected,
                        "sampleY",
                    ) -
                        driftY,
                    height,
                )

            if (
                mirrorX
            ) {
                sourceX =
                    width -
                        1 -
                        sourceX
            }
            if (
                mirrorY
            ) {
                sourceY =
                    height -
                        1 -
                        sourceY
            }

            val rowShift =
                HmacStream(
                    key,
                    "row-shift:" +
                        layoutEpoch +
                        ":" +
                        sourceY,
                ).uniformInt(
                    width,
                )
            sourceX =
                mod(
                    sourceX -
                        rowShift,
                    width,
                )

            val columnShift =
                HmacStream(
                    key,
                    "column-shift:" +
                        layoutEpoch +
                        ":" +
                        sourceX,
                ).uniformInt(
                    height,
                )
            sourceY =
                mod(
                    sourceY -
                        columnShift,
                    height,
                )

            val sampleIndex =
                sourceY *
                    width +
                    sourceX
            val field =
                buildBalancedField(
                    outcomes =
                        outcomes,
                    rng =
                        HmacStream(
                            key,
                            "field-layout:epoch:" +
                                layoutEpoch,
                        ),
                    cellsPerOutcome =
                        config
                            .cellsPerOutcome,
                )

            val witness =
                alignOutcomeWitness(
                    field =
                        field,
                    outcome =
                        outcome,
                    sampleIndex =
                        sampleIndex,
                    key =
                        key,
                    layoutEpoch =
                        layoutEpoch,
                )

            val fieldDigest =
                sha256Hex(
                    "entronex:natural-world:v1:field:" +
                        canonicalJson(
                            field,
                        ),
                )

            val regionRng =
                HmacStream(
                    key,
                    "region-grid:" +
                        layoutEpoch,
                )
            val regionColumns =
                ranged(
                    regionRng,
                    minOf(
                        2,
                        width,
                    ),
                    minOf(
                        6,
                        width,
                    ),
                )
            val regionRows =
                ranged(
                    regionRng,
                    minOf(
                        2,
                        height,
                    ),
                    minOf(
                        6,
                        height,
                    ),
                )

            val regionStates =
                mutableListOf<
                    Map<String, Any?>
                    >()
            repeat(
                regionColumns *
                    regionRows,
            ) {
                    region ->
                val rng =
                    HmacStream(
                        key,
                        "region-motion:" +
                            layoutEpoch +
                            ":" +
                            sampleTick +
                            ":" +
                            region,
                    )
                regionStates +=
                    linkedMapOf(
                        "region" to
                            region,
                        "offsetXMilli" to
                            (
                                rng.uniformInt(
                                    901,
                                ) -
                                    450
                                ),
                        "offsetYMilli" to
                            (
                                rng.uniformInt(
                                    901,
                                ) -
                                    450
                                ),
                        "rotationMilliDegrees" to
                            (
                                rng.uniformInt(
                                    24_001,
                                ) -
                                    12_000
                                ),
                        "scalePermille" to
                            (
                                900 +
                                    rng.uniformInt(
                                        201,
                                    )
                                ),
                        "phasePermille" to
                            rng.uniformInt(
                                1_000,
                            ),
                    )
            }

            val worldCore =
                linkedMapOf<String, Any?>(
                    "version" to
                        "entronex-natural-world-v1",
                    "role" to
                        "deterministic-presentation-proof",
                    "outcomeSelection" to
                        "independent-hkdf-sha256-rejection-sampling",
                    "topology" to
                        "rectangular-torus",
                    "fieldSize" to
                        fieldSize,
                    "width" to
                        width,
                    "height" to
                        height,
                    "cellsPerOutcome" to
                        config
                            .cellsPerOutcome,
                    "timelineTicks" to
                        config
                            .timelineTicks,
                    "epochCount" to
                        config
                            .epochCount,
                    "layoutEpoch" to
                        layoutEpoch,
                    "sampleTick" to
                        sampleTick,
                    "motionProfile" to
                        motionProfile,
                    "selectedProbe" to
                        selectedProbe,
                    "probes" to
                        probes,
                    "logicalTransform" to
                        linkedMapOf(
                            "driftX" to
                                driftX,
                            "driftY" to
                                driftY,
                            "mirrorX" to
                                mirrorX,
                            "mirrorY" to
                                mirrorY,
                            "rowShift" to
                                rowShift,
                            "columnShift" to
                                columnShift,
                        ),
                    "regions" to
                        linkedMapOf(
                            "presentationOnly" to
                                true,
                            "columns" to
                                regionColumns,
                            "rows" to
                                regionRows,
                            "states" to
                                regionStates,
                        ),
                    "jitter" to
                        linkedMapOf(
                            "presentationOnly" to
                                true,
                            "amplitudePermille" to
                                420,
                            "seedDigest" to
                                sha256Hex(
                                    "entronex:natural-world:v1:jitter:" +
                                        key.toHex(),
                                ),
                        ),
                    "sampleIndex" to
                        sampleIndex,
                    "witness" to
                        witness,
                    "fieldDigest" to
                        fieldDigest,
                    "sampledOutcome" to
                        field[
                            sampleIndex
                        ],
                )

            val worldDigest =
                sha256Hex(
                    "entronex:natural-world:v1:manifest:" +
                        canonicalJson(
                            worldCore,
                        ),
                )
            val manifest =
                LinkedHashMap(
                    worldCore,
                ).apply {
                    put(
                        "worldDigest",
                        worldDigest,
                    )
                }

            return NaturalWorldResult(
                manifest =
                    manifest,
                worldDigest =
                    worldDigest,
                fieldDigest =
                    fieldDigest,
            )
        }

        private fun createProbe(
            key: ByteArray,
            probe: Int,
            width: Int,
            height: Int,
            timelineTicks: Int,
            sampleTick: Int,
        ): Map<String, Any?> {
            val rng =
                HmacStream(
                    key,
                    "laser-probe:" +
                        probe,
                )
            val startX =
                rng.uniformInt(
                    width,
                )
            val startY =
                rng.uniformInt(
                    height,
                )
            var velocityX =
                rng.uniformInt(
                    7,
                ) -
                    3
            val velocityY =
                rng.uniformInt(
                    7,
                ) -
                    3

            if (
                velocityX == 0 &&
                velocityY == 0
            ) {
                velocityX =
                    1
            }

            val accelerationX =
                rng.uniformInt(
                    3,
                ) -
                    1
            val accelerationY =
                rng.uniformInt(
                    3,
                ) -
                    1
            val bendTick =
                1 +
                    rng.uniformInt(
                        maxOf(
                            1,
                            timelineTicks -
                                1,
                        ),
                    )
            val impulseX =
                rng.uniformInt(
                    7,
                ) -
                    3
            val impulseY =
                rng.uniformInt(
                    7,
                ) -
                    3

            val sampleX =
                mod(
                    kinematicPosition(
                        start =
                            startX,
                        velocity =
                            velocityX,
                        acceleration =
                            accelerationX,
                        impulse =
                            impulseX,
                        bendTick =
                            bendTick,
                        tick =
                            sampleTick,
                    ),
                    width,
                )
            val sampleY =
                mod(
                    kinematicPosition(
                        start =
                            startY,
                        velocity =
                            velocityY,
                        acceleration =
                            accelerationY,
                        impulse =
                            impulseY,
                        bendTick =
                            bendTick,
                        tick =
                            sampleTick,
                    ),
                    height,
                )

            return linkedMapOf(
                "probe" to
                    probe,
                "startX" to
                    startX,
                "startY" to
                    startY,
                "velocityX" to
                    velocityX,
                "velocityY" to
                    velocityY,
                "accelerationX" to
                    accelerationX,
                "accelerationY" to
                    accelerationY,
                "bendTick" to
                    bendTick,
                "impulseX" to
                    impulseX,
                "impulseY" to
                    impulseY,
                "sampleX" to
                    sampleX,
                "sampleY" to
                    sampleY,
            )
        }

        private fun kinematicPosition(
            start: Int,
            velocity: Int,
            acceleration: Int,
            impulse: Int,
            bendTick: Int,
            tick: Int,
        ): Long {
            val t =
                tick.toLong()
            val accelerated =
                start.toLong() +
                    velocity
                        .toLong() *
                        t +
                    (
                        acceleration
                            .toLong() *
                            t *
                            (
                                t -
                                    1L
                                )
                        ) /
                    2L
            val afterBend =
                maxOf(
                    0L,
                    t -
                        bendTick
                            .toLong(),
                )

            return accelerated +
                impulse
                    .toLong() *
                    afterBend
        }

        private fun alignOutcomeWitness(
            field: MutableList<Int>,
            outcome: Int,
            sampleIndex: Int,
            key: ByteArray,
            layoutEpoch: Int,
        ): Map<String, Any?> {
            if (
                field[
                    sampleIndex
                ] ==
                outcome
            ) {
                return linkedMapOf(
                    "mode" to
                        "balanced-swap-witness",
                    "swapped" to
                        false,
                    "sourceIndex" to
                        sampleIndex,
                    "targetIndex" to
                        sampleIndex,
                )
            }

            val candidates =
                field.indices
                    .filter {
                        field[
                            it
                        ] ==
                            outcome
                    }
            require(
                candidates
                    .isNotEmpty(),
            )

            val sourceIndex =
                candidates[
                    HmacStream(
                        key,
                        "outcome-witness:" +
                            layoutEpoch +
                            ":" +
                            sampleIndex,
                    ).uniformInt(
                        candidates.size,
                    )
                ]

            val temporary =
                field[
                    sourceIndex
                ]
            field[
                sourceIndex
            ] =
                field[
                    sampleIndex
                ]
            field[
                sampleIndex
            ] =
                temporary

            return linkedMapOf(
                "mode" to
                    "balanced-swap-witness",
                "swapped" to
                    true,
                "sourceIndex" to
                    sourceIndex,
                "targetIndex" to
                    sampleIndex,
            )
        }

        private fun buildBalancedField(
            outcomes: List<Int>,
            rng: HmacStream,
            cellsPerOutcome: Int,
        ): MutableList<Int> {
            val field =
                ArrayList<Int>(
                    outcomes.size *
                        cellsPerOutcome,
                )
            for (
                outcome in
                outcomes
            ) {
                repeat(
                    cellsPerOutcome,
                ) {
                    field +=
                        outcome
                }
            }

            for (
                index in
                field.lastIndex
                    downTo 1
            ) {
                val swapIndex =
                    rng.uniformInt(
                        index +
                            1,
                    )
                val temporary =
                    field[
                        index
                    ]
                field[
                    index
                ] =
                    field[
                        swapIndex
                    ]
                field[
                    swapIndex
                ] =
                    temporary
            }
            return field
        }

        private fun nearSquareDimensions(
            size: Int,
        ): Pair<Int, Int> {
            var height =
                sqrt(
                    size
                        .toDouble(),
                )
                    .toInt()
            while (
                height > 1 &&
                size %
                    height !=
                0
            ) {
                height -=
                    1
            }
            return (
                size /
                    height
                ) to
                height
        }

        private fun ranged(
            rng: HmacStream,
            minimum: Int,
            maximum: Int,
        ): Int {
            if (
                maximum <=
                minimum
            ) {
                return minimum
            }
            return minimum +
                rng.uniformInt(
                    maximum -
                        minimum +
                        1,
                )
        }

        private fun requireInt(
            map: Map<String, Any?>,
            name: String,
        ): Int =
            (
                map[
                    name
                ] as Number
                )
                .toInt()

        private fun mod(
            value: Int,
            divisor: Int,
        ): Int =
            (
                (
                    value %
                        divisor
                    ) +
                    divisor
                ) %
                divisor

        private fun mod(
            value: Long,
            divisor: Int,
        ): Int {
            val d =
                divisor
                    .toLong()
            return (
                (
                    (
                        value %
                            d
                        ) +
                        d
                    ) %
                    d
                )
                .toInt()
        }
    }

    private class HmacStream(
        private val key:
            ByteArray,
        private val namespace:
            String,
    ) {
        init {
            require(
                key.size >= 16,
            ) {
                "key must contain at least 16 bytes"
            }
        }

        private var counter:
            Long = 0
        private var buffer =
            ByteArray(
                0,
            )

        fun bytes(
            size: Int,
        ): ByteArray {
            require(
                size >= 0,
            )

            while (
                buffer.size <
                size
            ) {
                val input =
                    (
                        "entronex:v1:stream:" +
                            namespace +
                            ":" +
                            counter
                        ).toByteArray(
                        Charsets.UTF_8,
                    )
                val block =
                    hmacSha256(
                        key,
                        input,
                    )
                counter +=
                    1
                buffer +=
                    block
            }

            val output =
                buffer.copyOfRange(
                    0,
                    size,
                )
            buffer =
                buffer.copyOfRange(
                    size,
                    buffer.size,
                )
            return output
        }

        fun uniformInt(
            maxExclusive: Int,
        ): Int {
            require(
                maxExclusive > 0,
            )
            val twoPow48 =
                281_474_976_710_656L
            val limit =
                (
                    twoPow48 /
                        maxExclusive
                            .toLong()
                    ) *
                    maxExclusive
                        .toLong()

            while (
                true
            ) {
                val six =
                    bytes(
                        6,
                    )
                var value =
                    0L
                for (
                    byte in
                    six
                ) {
                    value =
                        (
                            value shl 8
                            ) or
                            (
                                byte.toLong() and
                                    0xffL
                                )
                }
                if (
                    value <
                    limit
                ) {
                    return (
                        value %
                            maxExclusive
                                .toLong()
                        )
                        .toInt()
                }
            }
        }
    }

    private fun hkdfSha256(
        ikm: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int,
    ): ByteArray {
        require(
            length in
                1..(
                    255 *
                        32
                    ),
        )
        val prk =
            hmacSha256(
                salt,
                ikm,
            )
        val output =
            ArrayList<Byte>(
                length,
            )
        var previous =
            ByteArray(
                0,
            )
        var counter =
            1

        while (
            output.size <
            length
        ) {
            val blockInput =
                ByteArray(
                    previous.size +
                        info.size +
                        1,
                )
            previous.copyInto(
                blockInput,
                0,
            )
            info.copyInto(
                blockInput,
                previous.size,
            )
            blockInput[
                blockInput.lastIndex
            ] =
                counter.toByte()
            previous =
                hmacSha256(
                    prk,
                    blockInput,
                )
            for (
                byte in
                previous
            ) {
                if (
                    output.size ==
                    length
                ) {
                    break
                }
                output.add(
                    byte,
                )
            }
            counter +=
                1
        }

        return ByteArray(
            output.size,
        ) {
            output[
                it
            ]
        }
    }

    private fun hmacSha256(
        key: ByteArray,
        bytes: ByteArray,
    ): ByteArray {
        val mac =
            Mac.getInstance(
                "HmacSHA256",
            )
        mac.init(
            SecretKeySpec(
                key,
                "HmacSHA256",
            ),
        )
        return mac.doFinal(
            bytes,
        )
    }

    private fun sha256Hex(
        text: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                text.toByteArray(
                    Charsets.UTF_8,
                ),
            )
            .toHex()

    private fun randomSeedHex():
        String =
        randomHex(
            32,
        )

    private fun randomHex(
        byteCount: Int,
    ): String {
        val bytes =
            ByteArray(
                byteCount,
            )
        secureRandom.nextBytes(
            bytes,
        )
        return bytes.toHex()
    }

    private fun requireDigest(
        value: String,
        name: String,
    ): String {
        require(
            Regex(
                "^[0-9a-fA-F]{64}$",
            ).matches(
                value,
            ),
        ) {
            "$name must be a 32-byte SHA-256 hex digest"
        }
        return value.lowercase()
    }

    private fun validateSeed(
        seed: String,
        name: String,
    ): String {
        require(
            Regex(
                "^[0-9a-fA-F]{64}$",
            ).matches(
                seed,
            ),
        ) {
            "$name must contain exactly 32 bytes of hex"
        }
        return seed.lowercase()
    }

    private fun canonicalJson(
        value: Any?,
    ): String =
        when (
            value
        ) {
            null ->
                "null"
            is String ->
                quoteJsonString(
                    value,
                )
            is Boolean ->
                if (
                    value
                ) {
                    "true"
                } else {
                    "false"
                }
            is Byte,
            is Short,
            is Int,
            is Long,
            ->
                (
                    value as Number
                    )
                    .toLong()
                    .toString()
            is List<*> ->
                value.joinToString(
                    prefix = "[",
                    postfix = "]",
                    separator = ",",
                ) {
                    canonicalJson(
                        it,
                    )
                }
            is Map<*, *> ->
                value.entries
                    .map {
                        entry ->
                        val key =
                            entry.key as? String
                                ?: error(
                                    "JSON object keys must be strings",
                                )
                        key to
                            entry.value
                    }
                    .sortedBy {
                        it.first
                    }
                    .joinToString(
                        prefix = "{",
                        postfix = "}",
                        separator = ",",
                    ) {
                            entry ->
                        quoteJsonString(
                            entry.first,
                        ) +
                            ":" +
                            canonicalJson(
                                entry.second,
                            )
                    }
            else ->
                error(
                    "Unsupported canonical JSON value: " +
                        value::class
                            .java
                            .name,
                )
        }

    private fun quoteJsonString(
        value: String,
    ): String {
        validateUnicode(
            value,
        )
        val out =
            StringBuilder(
                value.length +
                    2,
            )
        out.append(
            '"',
        )

        for (
            char in
            value
        ) {
            when (
                char
            ) {
                '"' ->
                    out.append(
                        "\\\"",
                    )
                '\\' ->
                    out.append(
                        "\\\\",
                    )
                '\b' ->
                    out.append(
                        "\\b",
                    )
                '\u000c' ->
                    out.append(
                        "\\f",
                    )
                '\n' ->
                    out.append(
                        "\\n",
                    )
                '\r' ->
                    out.append(
                        "\\r",
                    )
                '\t' ->
                    out.append(
                        "\\t",
                    )
                else -> {
                    if (
                        char.code <
                        0x20
                    ) {
                        out.append(
                            "\\u",
                        )
                        out.append(
                            char.code
                                .toString(
                                    16,
                                )
                                .padStart(
                                    4,
                                    '0',
                                ),
                        )
                    } else {
                        out.append(
                            char,
                        )
                    }
                }
            }
        }

        out.append(
            '"',
        )
        return out.toString()
    }

    private fun validateUnicode(
        value: String,
    ) {
        var index =
            0
        while (
            index <
            value.length
        ) {
            val code =
                value[
                    index
                ].code
            when {
                code in
                    0xD800..0xDBFF -> {
                    require(
                        index +
                            1 <
                            value.length,
                    ) {
                        "string contains an unpaired high surrogate"
                    }
                    val next =
                        value[
                            index +
                                1
                        ].code
                    require(
                        next in
                            0xDC00..0xDFFF,
                    ) {
                        "string contains an unpaired high surrogate"
                    }
                    index +=
                        2
                }
                code in
                    0xDC00..0xDFFF ->
                    throw IllegalArgumentException(
                        "string contains an unpaired low surrogate",
                    )
                else ->
                    index +=
                        1
            }
        }
    }

    private fun ByteArray.toHex():
        String =
        joinToString(
            "",
        ) {
            "%02x".format(
                it.toInt() and
                    0xff,
            )
        }

    private fun String.hexToBytes():
        ByteArray {
        require(
            length %
                2 ==
                0,
        )
        return ByteArray(
            length /
                2,
        ) {
                index ->
            substring(
                index *
                    2,
                index *
                    2 +
                    2,
            )
                .toInt(
                    16,
                )
                .toByte()
        }
    }

    private const val JS_SAFE_INTEGER_MAX:
        Long =
        9_007_199_254_740_991L
}
