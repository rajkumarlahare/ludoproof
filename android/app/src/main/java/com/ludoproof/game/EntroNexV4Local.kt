package com.ludoproof.game

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor
import kotlin.math.sqrt

data class LocalV4Context(
    val applicationId: String,
    val sessionId: String,
    val eventId: String,
    val eventType: String,
    val eventIndex: Long,
    val subjectHash: String? = null,
    val previousStateHash: String? = null,
    val metadataDigest: String? = null,
) {
    fun toJcsMap(): Map<String, Any?> =
        mapOf(
            "applicationId" to applicationId,
            "sessionId" to sessionId,
            "eventId" to eventId,
            "eventType" to eventType,
            "eventIndex" to eventIndex,
            "subjectHash" to subjectHash,
            "previousStateHash" to previousStateHash,
            "metadataDigest" to metadataDigest,
        )
}

data class LocalV4WorldConfig(
    val cellsPerOutcome: Int = 256,
    val timelineTicks: Int = 4096,
    val epochCount: Int = 32,
    val probeCount: Int = 3,
) {
    fun toJcsMap(): Map<String, Any?> =
        mapOf(
            "cellsPerOutcome" to cellsPerOutcome,
            "timelineTicks" to timelineTicks,
            "epochCount" to epochCount,
            "probeCount" to probeCount,
        )
}

data class LocalV4Config(
    val outcomes: List<Int>,
    val context: LocalV4Context,
    val world: LocalV4WorldConfig,
) {
    fun toJcsMap(): Map<String, Any?> =
        mapOf(
            "outcomes" to outcomes,
            "context" to context.toJcsMap(),
            "world" to world.toJcsMap(),
        )
}

data class LocalV4Probe(
    val probe: Int,
    val startX: Int,
    val startY: Int,
    val velocityX: Int,
    val velocityY: Int,
    val accelerationX: Int,
    val accelerationY: Int,
    val bendTick: Int,
    val impulseX: Int,
    val impulseY: Int,
    val sampleX: Int,
    val sampleY: Int,
) {
    fun toJcsMap(): Map<String, Any?> =
        mapOf(
            "probe" to probe,
            "startX" to startX,
            "startY" to startY,
            "velocityX" to velocityX,
            "velocityY" to velocityY,
            "accelerationX" to accelerationX,
            "accelerationY" to accelerationY,
            "bendTick" to bendTick,
            "impulseX" to impulseX,
            "impulseY" to impulseY,
            "sampleX" to sampleX,
            "sampleY" to sampleY,
        )
}

data class LocalV4Witness(
    val swapped: Boolean,
    val sourceIndex: Int,
    val targetIndex: Int,
) {
    fun toJcsMap(): Map<String, Any?> =
        mapOf(
            "mode" to "balanced-swap-witness",
            "swapped" to swapped,
            "sourceIndex" to sourceIndex,
            "targetIndex" to targetIndex,
        )
}

data class LocalV4RegionState(
    val region: Int,
    val offsetXMilli: Int,
    val offsetYMilli: Int,
    val rotationMilliDegrees: Int,
    val scalePermille: Int,
    val phasePermille: Int,
) {
    fun toJcsMap(): Map<String, Any?> =
        mapOf(
            "region" to region,
            "offsetXMilli" to offsetXMilli,
            "offsetYMilli" to offsetYMilli,
            "rotationMilliDegrees" to rotationMilliDegrees,
            "scalePermille" to scalePermille,
            "phasePermille" to phasePermille,
        )
}

data class LocalV4World(
    val fieldSize: Int,
    val width: Int,
    val height: Int,
    val cellsPerOutcome: Int,
    val timelineTicks: Int,
    val epochCount: Int,
    val layoutEpoch: Int,
    val sampleTick: Int,
    val motionProfile: String,
    val selectedProbe: Int,
    val probes: List<LocalV4Probe>,
    val driftX: Int,
    val driftY: Int,
    val mirrorX: Boolean,
    val mirrorY: Boolean,
    val rowShift: Int,
    val columnShift: Int,
    val regionColumns: Int,
    val regionRows: Int,
    val regionStates: List<LocalV4RegionState>,
    val jitterSeedDigest: String,
    val sampleIndex: Int,
    val witness: LocalV4Witness,
    val fieldDigest: String,
    val sampledOutcome: Int,
    val worldDigest: String,
    val field: List<Int>,
) {
    fun toProofMap(): Map<String, Any?> =
        mapOf(
            "version" to EntroNexV4Local.NATURAL_WORLD_V1,
            "role" to "deterministic-presentation-proof",
            "outcomeSelection" to "independent-hkdf-sha256-rejection-sampling",
            "topology" to "rectangular-torus",
            "fieldSize" to fieldSize,
            "width" to width,
            "height" to height,
            "cellsPerOutcome" to cellsPerOutcome,
            "timelineTicks" to timelineTicks,
            "epochCount" to epochCount,
            "layoutEpoch" to layoutEpoch,
            "sampleTick" to sampleTick,
            "motionProfile" to motionProfile,
            "selectedProbe" to selectedProbe,
            "probes" to probes.map { it.toJcsMap() },
            "logicalTransform" to mapOf(
                "driftX" to driftX,
                "driftY" to driftY,
                "mirrorX" to mirrorX,
                "mirrorY" to mirrorY,
                "rowShift" to rowShift,
                "columnShift" to columnShift,
            ),
            "regions" to mapOf(
                "presentationOnly" to true,
                "columns" to regionColumns,
                "rows" to regionRows,
                "states" to regionStates.map { it.toJcsMap() },
            ),
            "jitter" to mapOf(
                "presentationOnly" to true,
                "amplitudePermille" to 420,
                "seedDigest" to jitterSeedDigest,
            ),
            "sampleIndex" to sampleIndex,
            "witness" to witness.toJcsMap(),
            "fieldDigest" to fieldDigest,
            "sampledOutcome" to sampledOutcome,
            "worldDigest" to worldDigest,
        )
}

data class LocalV4Result(
    val roundId: String,
    val serverSeed: String,
    val serverCommitment: String,
    val clientSeed: String,
    val clientCommitment: String,
    val config: LocalV4Config,
    val configDigest: String,
    val contextDigest: String,
    val eventBindingDigest: String,
    val transcriptDigest: String,
    val outcomeIndex: Int,
    val outcome: Int,
    val world: LocalV4World,
    val proofDigest: String,
) {
    fun coreMap(): Map<String, Any?> =
        mapOf(
            "algorithm" to EntroNexV4Local.ALGORITHM_V4,
            "roundId" to roundId,
            "serverCommitment" to serverCommitment,
            "serverSeed" to serverSeed,
            "clientCommitment" to clientCommitment,
            "clientSeed" to clientSeed,
            "configDigest" to configDigest,
            "contextDigest" to contextDigest,
            "eventBindingDigest" to eventBindingDigest,
            "transcriptDigest" to transcriptDigest,
            "config" to config.toJcsMap(),
            "outcomeIndex" to outcomeIndex,
            "outcome" to outcome,
            "world" to world.toProofMap(),
        )
}

object EntroNexV4Local {
    const val ALGORITHM_V4 =
        "entronex-v4-dual-commit-hkdf-sha256-context-bound"
    const val NATURAL_WORLD_V1 =
        "entronex-natural-world-v1"

    private const val TWO_POW_48 =
        281_474_976_710_656L
    private const val JS_SAFE_INTEGER_MAX =
        9_007_199_254_740_991L
    private const val MAX_V4_WORLD_CELLS =
        65_536

    private val motionProfiles =
        listOf(
            "crosswind",
            "orbital-drift",
            "pulse-reverse",
            "toroidal-flow",
            "scatter-wave",
        )

    private val secureRandom =
        SecureRandom()

    fun randomSeedHex(): String {
        val bytes =
            ByteArray(32)
        secureRandom.nextBytes(
            bytes,
        )
        return bytes.toHex()
    }

    fun randomRoundId(): String =
        UUID.randomUUID()
            .toString()

    fun clientCommitmentForSeed(
        seedHex: String,
    ): String {
        val seed =
            validateSeed(
                seedHex,
                "client seed",
            )
        return sha256Hex(
            "entronex:v4:client-commit:" +
                seed,
        )
    }

    fun serverCommitmentForSeed(
        seedHex: String,
    ): String {
        val seed =
            validateSeed(
                seedHex,
                "server seed",
            )
        return sha256Hex(
            "entronex:v4:server-commit:" +
                seed,
        )
    }

    fun contextDigest(
        context: LocalV4Context,
    ): String {
        validateContext(
            context,
        )
        return sha256Hex(
            "entronex:v4:context:" +
                Jcs.serialize(
                    context.toJcsMap(),
                ),
        )
    }

    fun eventBindingDigest(
        context: LocalV4Context,
    ): String {
        validateContext(
            context,
        )
        val binding =
            mapOf(
                "applicationId" to
                    context.applicationId,
                "sessionId" to
                    context.sessionId,
                "eventType" to
                    context.eventType,
                "eventIndex" to
                    context.eventIndex,
            )
        return sha256Hex(
            "entronex:v4:event-binding:" +
                Jcs.serialize(
                    binding,
                ),
        )
    }

    fun configDigest(
        config: LocalV4Config,
    ): String {
        validateConfig(
            config,
        )
        return sha256Hex(
            "entronex:v4:config:" +
                Jcs.serialize(
                    config.toJcsMap(),
                ),
        )
    }

    fun resolve(
        roundId: String,
        serverSeed: String,
        clientSeed: String,
        config: LocalV4Config,
    ): LocalV4Result {
        require(
            roundId.isNotEmpty() &&
                roundId.length <=
                200,
        ) {
            "roundId must be a non-empty string up to 200 characters"
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
        validateConfig(
            config,
        )

        val serverCommitment =
            serverCommitmentForSeed(
                normalizedServerSeed,
            )
        val clientCommitment =
            clientCommitmentForSeed(
                normalizedClientSeed,
            )
        val configDigest =
            configDigest(
                config,
            )
        val contextDigest =
            contextDigest(
                config.context,
            )
        val eventBindingDigest =
            eventBindingDigest(
                config.context,
            )

        val transcript =
            mapOf(
                "algorithm" to
                    ALGORITHM_V4,
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
                    Jcs.serialize(
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
        val outcomeRng =
            HmacStream(
                outcomeKey,
                "direct-outcome-selection",
            )
        val outcomeIndex =
            outcomeRng.uniformInt(
                config.outcomes.size,
            )
        val outcome =
            config.outcomes[
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
            sampleNaturalWorld(
                outcomes =
                    config.outcomes,
                outcome =
                    outcome,
                key =
                    worldKey,
                worldConfig =
                    config.world,
            )

        val provisional =
            LocalV4Result(
                roundId =
                    roundId,
                serverSeed =
                    normalizedServerSeed,
                serverCommitment =
                    serverCommitment,
                clientSeed =
                    normalizedClientSeed,
                clientCommitment =
                    clientCommitment,
                config =
                    config,
                configDigest =
                    configDigest,
                contextDigest =
                    contextDigest,
                eventBindingDigest =
                    eventBindingDigest,
                transcriptDigest =
                    transcriptDigest,
                outcomeIndex =
                    outcomeIndex,
                outcome =
                    outcome,
                world =
                    world,
                proofDigest =
                    "",
            )

        val proofDigest =
            sha256Hex(
                "entronex:v4:proof:" +
                    Jcs.serialize(
                        provisional.coreMap(),
                    ),
            )

        return provisional.copy(
            proofDigest =
                proofDigest,
        )
    }

    fun verify(
        result: LocalV4Result,
    ): Boolean =
        runCatching {
            val recomputed =
                resolve(
                    result.roundId,
                    result.serverSeed,
                    result.clientSeed,
                    result.config,
                )
            secureEqualHex(
                result.proofDigest,
                recomputed.proofDigest,
            ) &&
                Jcs.serialize(
                    result.coreMap(),
                ) ==
                Jcs.serialize(
                    recomputed.coreMap(),
                )
        }.getOrDefault(
            false,
        )

    fun sha256Hex(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    Charsets.UTF_8,
                ),
            )
            .toHex()

    fun jcs(
        value: Any?,
    ): String =
        Jcs.serialize(
            value,
        )

    private fun sampleNaturalWorld(
        outcomes: List<Int>,
        outcome: Int,
        key: ByteArray,
        worldConfig: LocalV4WorldConfig,
    ): LocalV4World {
        val fieldSize =
            validateFieldComplexity(
                outcomes,
                worldConfig
                    .cellsPerOutcome,
            )
        require(
            fieldSize <=
                MAX_V4_WORLD_CELLS,
        ) {
            "v4 natural world is too large"
        }
        require(
            key.size ==
                32,
        ) {
            "natural-world key must be 32 bytes"
        }

        validateWorldConfig(
            worldConfig,
        )
        require(
            outcomes.contains(
                outcome,
            ),
        ) {
            "outcome must belong to outcomes"
        }

        val dimensions =
            nearSquareDimensions(
                fieldSize,
            )
        val width =
            dimensions.first
        val height =
            dimensions.second

        val timelineRng =
            HmacStream(
                key,
                "timeline",
            )
        val sampleTick =
            timelineRng.uniformInt(
                worldConfig
                    .timelineTicks,
            )
        val layoutEpoch =
            minOf(
                worldConfig
                    .epochCount -
                    1,
                floor(
                    sampleTick
                        .toDouble() *
                        worldConfig
                            .epochCount /
                        worldConfig
                            .timelineTicks,
                ).toInt(),
            )

        val profileRng =
            HmacStream(
                key,
                "motion-profile",
            )
        val motionProfile =
            motionProfiles[
                profileRng.uniformInt(
                    motionProfiles.size,
                )
            ]

        val probes =
            buildList {
                for (
                    probe in
                    0 until
                        worldConfig
                            .probeCount
                ) {
                    add(
                        createProbe(
                            key,
                            probe,
                            width,
                            height,
                            worldConfig
                                .timelineTicks,
                            sampleTick,
                        ),
                    )
                }
            }

        val probeSelector =
            HmacStream(
                key,
                "probe-selector",
            )
        val selectedProbe =
            probeSelector.uniformInt(
                worldConfig
                    .probeCount,
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
            ) ==
                1
        val mirrorY =
            motion.uniformInt(
                2,
            ) ==
                1

        var sourceX =
            mod(
                selected.sampleX -
                    driftX,
                width,
            )
        var sourceY =
            mod(
                selected.sampleY -
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

        val rawSampleIndex =
            sourceY *
                width +
                sourceX

        val field =
            buildBalancedField(
                outcomes,
                HmacStream(
                    key,
                    "field-layout:epoch:" +
                        layoutEpoch,
                ),
                worldConfig
                    .cellsPerOutcome,
            )

        val witness =
            alignOutcomeWitness(
                field,
                outcome,
                rawSampleIndex,
                key,
                layoutEpoch,
            )

        val fieldDigest =
            sha256Hex(
                "entronex:natural-world:v1:field:" +
                    Jcs.serialize(
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
            buildList {
                for (
                    region in
                    0 until
                        regionColumns *
                        regionRows
                ) {
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
                    add(
                        LocalV4RegionState(
                            region =
                                region,
                            offsetXMilli =
                                rng.uniformInt(
                                    901,
                                ) -
                                    450,
                            offsetYMilli =
                                rng.uniformInt(
                                    901,
                                ) -
                                    450,
                            rotationMilliDegrees =
                                rng.uniformInt(
                                    24_001,
                                ) -
                                    12_000,
                            scalePermille =
                                900 +
                                    rng.uniformInt(
                                        201,
                                    ),
                            phasePermille =
                                rng.uniformInt(
                                    1_000,
                                ),
                        ),
                    )
                }
            }

        val jitterSeedDigest =
            sha256Hex(
                "entronex:natural-world:v1:jitter:" +
                    key.toHex(),
            )

        val worldCore =
            mapOf(
                "version" to
                    NATURAL_WORLD_V1,
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
                    worldConfig
                        .cellsPerOutcome,
                "timelineTicks" to
                    worldConfig
                        .timelineTicks,
                "epochCount" to
                    worldConfig
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
                    probes.map {
                        it.toJcsMap()
                    },
                "logicalTransform" to
                    mapOf(
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
                    mapOf(
                        "presentationOnly" to
                            true,
                        "columns" to
                            regionColumns,
                        "rows" to
                            regionRows,
                        "states" to
                            regionStates.map {
                                it.toJcsMap()
                            },
                    ),
                "jitter" to
                    mapOf(
                        "presentationOnly" to
                            true,
                        "amplitudePermille" to
                            420,
                        "seedDigest" to
                            jitterSeedDigest,
                    ),
                "sampleIndex" to
                    rawSampleIndex,
                "witness" to
                    witness.toJcsMap(),
                "fieldDigest" to
                    fieldDigest,
                "sampledOutcome" to
                    field[
                        rawSampleIndex
                    ],
            )

        val worldDigest =
            sha256Hex(
                "entronex:natural-world:v1:manifest:" +
                    Jcs.serialize(
                        worldCore,
                    ),
            )

        return LocalV4World(
            fieldSize =
                fieldSize,
            width =
                width,
            height =
                height,
            cellsPerOutcome =
                worldConfig
                    .cellsPerOutcome,
            timelineTicks =
                worldConfig
                    .timelineTicks,
            epochCount =
                worldConfig
                    .epochCount,
            layoutEpoch =
                layoutEpoch,
            sampleTick =
                sampleTick,
            motionProfile =
                motionProfile,
            selectedProbe =
                selectedProbe,
            probes =
                probes,
            driftX =
                driftX,
            driftY =
                driftY,
            mirrorX =
                mirrorX,
            mirrorY =
                mirrorY,
            rowShift =
                rowShift,
            columnShift =
                columnShift,
            regionColumns =
                regionColumns,
            regionRows =
                regionRows,
            regionStates =
                regionStates,
            jitterSeedDigest =
                jitterSeedDigest,
            sampleIndex =
                rawSampleIndex,
            witness =
                witness,
            fieldDigest =
                fieldDigest,
            sampledOutcome =
                field[
                    rawSampleIndex
                ],
            worldDigest =
                worldDigest,
            field =
                field.toList(),
        )
    }

    private fun createProbe(
        key: ByteArray,
        probe: Int,
        width: Int,
        height: Int,
        timelineTicks: Int,
        sampleTick: Int,
    ): LocalV4Probe {
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
            velocityX = 1
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
                    startX,
                    velocityX,
                    accelerationX,
                    impulseX,
                    bendTick,
                    sampleTick,
                ),
                width,
            )
        val sampleY =
            mod(
                kinematicPosition(
                    startY,
                    velocityY,
                    accelerationY,
                    impulseY,
                    bendTick,
                    sampleTick,
                ),
                height,
            )

        return LocalV4Probe(
            probe =
                probe,
            startX =
                startX,
            startY =
                startY,
            velocityX =
                velocityX,
            velocityY =
                velocityY,
            accelerationX =
                accelerationX,
            accelerationY =
                accelerationY,
            bendTick =
                bendTick,
            impulseX =
                impulseX,
            impulseY =
                impulseY,
            sampleX =
                sampleX,
            sampleY =
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
                velocity.toLong() *
                t +
                (
                    acceleration.toLong() *
                        t *
                        (
                            t -
                                1
                            )
                    ) /
                2L
        val afterBend =
            maxOf(
                0,
                tick -
                    bendTick,
            )
        return accelerated +
            impulse.toLong() *
            afterBend.toLong()
    }

    private fun alignOutcomeWitness(
        field: MutableList<Int>,
        outcome: Int,
        sampleIndex: Int,
        key: ByteArray,
        layoutEpoch: Int,
    ): LocalV4Witness {
        if (
            field[
                sampleIndex
            ] ==
            outcome
        ) {
            return LocalV4Witness(
                swapped =
                    false,
                sourceIndex =
                    sampleIndex,
                targetIndex =
                    sampleIndex,
            )
        }

        val candidates =
            buildList {
                field.forEachIndexed {
                        index,
                        value ->
                    if (
                        value ==
                        outcome
                    ) {
                        add(
                            index,
                        )
                    }
                }
            }
        require(
            candidates.isNotEmpty(),
        ) {
            "natural-world outcome witness is unavailable"
        }

        val rng =
            HmacStream(
                key,
                "outcome-witness:" +
                    layoutEpoch +
                    ":" +
                    sampleIndex,
            )
        val sourceIndex =
            candidates[
                rng.uniformInt(
                    candidates.size,
                )
            ]

        val sourceValue =
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
            sourceValue

        return LocalV4Witness(
            swapped =
                true,
            sourceIndex =
                sourceIndex,
            targetIndex =
                sampleIndex,
        )
    }

    private fun buildBalancedField(
        outcomes: List<Int>,
        rng: HmacStream,
        cellsPerOutcome: Int,
    ): MutableList<Int> {
        validateFieldComplexity(
            outcomes,
            cellsPerOutcome,
        )
        val field =
            mutableListOf<Int>()
        outcomes.forEach {
                outcome ->
            repeat(
                cellsPerOutcome,
            ) {
                field.add(
                    outcome,
                )
            }
        }

        for (
            index in
            field.lastIndex
                downTo
                1
        ) {
            val other =
                rng.uniformInt(
                    index +
                        1,
                )
            val value =
                field[
                    index
                ]
            field[
                index
            ] =
                field[
                    other
                ]
            field[
                other
            ] =
                value
        }
        return field
    }

    private fun validateFieldComplexity(
        outcomes: List<Int>,
        cellsPerOutcome: Int,
    ): Int {
        require(
            outcomes.size in
                2..1024,
        ) {
            "outcomes must contain between 2 and 1024 items"
        }
        require(
            outcomes.distinct()
                .size ==
                outcomes.size,
        ) {
            "outcomes must be unique"
        }
        require(
            cellsPerOutcome in
                1..4096,
        ) {
            "cellsPerOutcome must be between 1 and 4096"
        }
        val cells =
            outcomes.size *
                cellsPerOutcome
        require(
            cells <=
                262_144,
        ) {
            "field exceeds maximum cell count"
        }
        return cells
    }

    private fun validateConfig(
        config: LocalV4Config,
    ) {
        val fieldSize =
            validateFieldComplexity(
                config.outcomes,
                config.world
                    .cellsPerOutcome,
            )
        require(
            fieldSize <=
                MAX_V4_WORLD_CELLS,
        ) {
            "v4 natural world exceeds maximum cell count"
        }
        validateContext(
            config.context,
        )
        validateWorldConfig(
            config.world,
        )
    }

    private fun validateWorldConfig(
        world: LocalV4WorldConfig,
    ) {
        require(
            world.timelineTicks in
                256..16_384,
        ) {
            "timelineTicks must be between 256 and 16384"
        }
        require(
            world.epochCount in
                2..128,
        ) {
            "epochCount must be between 2 and 128"
        }
        require(
            world.probeCount in
                1..8,
        ) {
            "probeCount must be between 1 and 8"
        }
        require(
            world.epochCount <=
                world.timelineTicks,
        ) {
            "epochCount cannot exceed timelineTicks"
        }
    }

    private fun validateContext(
        context: LocalV4Context,
    ) {
        requireText(
            context.applicationId,
            "applicationId",
            120,
        )
        requireText(
            context.sessionId,
            "sessionId",
            200,
        )
        requireText(
            context.eventId,
            "eventId",
            200,
        )
        requireText(
            context.eventType,
            "eventType",
            80,
        )
        require(
            context.eventIndex >=
                0 &&
                context.eventIndex <=
                JS_SAFE_INTEGER_MAX,
        ) {
            "eventIndex must be a non-negative safe integer"
        }
        listOf(
            "subjectHash" to
                context.subjectHash,
            "previousStateHash" to
                context.previousStateHash,
            "metadataDigest" to
                context.metadataDigest,
        ).forEach {
                pair ->
            val value =
                pair.second
            require(
                value == null ||
                    HEX_32_BYTES
                        .matches(
                            value,
                        ),
            ) {
                pair.first +
                    " must be SHA-256 hex or null"
            }
        }
    }

    private fun requireText(
        value: String,
        name: String,
        maxLength: Int,
    ) {
        require(
            value.isNotEmpty() &&
                value.length <=
                maxLength,
        ) {
            name +
                " must be a non-empty string up to " +
                maxLength +
                " characters"
        }
        Jcs.validateUnicode(
            value,
            name,
        )
    }

    private fun validateSeed(
        seedHex: String,
        name: String,
    ): String {
        require(
            HEX_32_BYTES
                .matches(
                    seedHex,
                ),
        ) {
            name +
                " must contain exactly 32 bytes of hex"
        }
        return seedHex
            .lowercase()
    }

    private fun deriveV4Key(
        serverSeed: String,
        clientSeed: String,
        transcriptDigest: String,
        label: String,
    ): ByteArray {
        val ikm =
            serverSeed.hexToBytes() +
                clientSeed
                    .hexToBytes()
        return hkdfSha256(
            ikm =
                ikm,
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
            ByteArray(0)
        var counter =
            1

        while (
            output.size <
            length
        ) {
            val input =
                ByteArray(
                    previous.size +
                        info.size +
                        1,
                )
            previous.copyInto(
                input,
                0,
            )
            info.copyInto(
                input,
                previous.size,
            )
            input[
                input.lastIndex
            ] =
                counter.toByte()
            previous =
                hmacSha256(
                    prk,
                    input,
                )
            previous.forEach {
                    byte ->
                if (
                    output.size <
                    length
                ) {
                    output.add(
                        byte,
                    )
                }
            }
            counter += 1
        }

        return ByteArray(
            output.size,
        ) {
                index ->
            output[
                index
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

    private fun secureEqualHex(
        left: String,
        right: String,
    ): Boolean =
        runCatching {
            MessageDigest
                .isEqual(
                    left.hexToBytes(),
                    right.hexToBytes(),
                )
        }.getOrDefault(
            false,
        )

    private fun nearSquareDimensions(
        size: Int,
    ): Pair<Int, Int> {
        var height =
            floor(
                sqrt(
                    size.toDouble(),
                ),
            ).toInt()
        while (
            height >
            1 &&
            size %
            height !=
            0
        ) {
            height -= 1
        }
        return (
            size /
                height
            ) to height
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
    ): Int =
        (
            (
                value %
                    divisor
                        .toLong()
                ) +
                divisor
                    .toLong()
            ) %
            divisor
                .toLong()
            ).toInt()

    private class HmacStream(
        private val key: ByteArray,
        namespace: String,
    ) {
        private val namespace =
            namespace
        private var counter =
            0L
        private var buffer =
            ByteArray(0)

        init {
            require(
                key.size >=
                    16,
            ) {
                "key must contain at least 16 bytes"
            }
        }

        fun bytes(
            size: Int,
        ): ByteArray {
            require(
                size >=
                    0,
            )
            while (
                buffer.size <
                size
            ) {
                val block =
                    hmacSha256(
                        key,
                        (
                            "entronex:v1:stream:" +
                                namespace +
                                ":" +
                                counter
                            ).toByteArray(
                            Charsets.UTF_8,
                        ),
                    )
                counter += 1
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
                maxExclusive >
                    0,
            )
            val max =
                maxExclusive
                    .toLong()
            val limit =
                (
                    TWO_POW_48 /
                        max
                    ) *
                    max

            while (
                true
            ) {
                val six =
                    bytes(
                        6,
                    )
                var value =
                    0L
                six.forEach {
                        byte ->
                    value =
                        (
                            value shl
                                8
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
                            max
                        ).toInt()
                }
            }
        }
    }

    private object Jcs {
        fun serialize(
            value: Any?,
        ): String =
            when (
                value
            ) {
                null ->
                    "null"
                is String ->
                    quote(
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
                is Byte ->
                    value.toString()
                is Short ->
                    value.toString()
                is Int ->
                    value.toString()
                is Long -> {
                    require(
                        value in
                            -JS_SAFE_INTEGER_MAX..
                            JS_SAFE_INTEGER_MAX,
                    ) {
                        "EntroNex JCS profile accepts safe integers only"
                    }
                    value.toString()
                }
                is List<*> ->
                    value.joinToString(
                        prefix = "[",
                        postfix = "]",
                        separator = ",",
                    ) {
                            child ->
                        serialize(
                            child,
                        )
                    }
                is Map<*, *> -> {
                    val entries =
                        value.entries
                            .map {
                                entry ->
                                val key =
                                    entry.key as? String
                                        ?: error(
                                            "EntroNex JCS object keys must be strings",
                                        )
                                validateUnicode(
                                    key,
                                    "object key",
                                )
                                key to
                                    entry.value
                            }
                            .sortedBy {
                                it.first
                            }
                    entries.joinToString(
                        prefix = "{",
                        postfix = "}",
                        separator = ",",
                    ) {
                            entry ->
                        quote(
                            entry.first,
                        ) +
                            ":" +
                            serialize(
                                entry.second,
                            )
                    }
                }
                else ->
                    error(
                        "unsupported EntroNex JCS value: " +
                            value::class
                                .java
                                .name,
                    )
            }

        fun validateUnicode(
            value: String,
            name: String,
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
                if (
                    code in
                    0xD800..0xDBFF
                ) {
                    val next =
                        value.getOrNull(
                            index +
                                1,
                        )
                            ?.code
                    require(
                        next != null &&
                            next in
                            0xDC00..0xDFFF,
                    ) {
                        name +
                            " contains an unpaired high surrogate"
                    }
                    index += 2
                    continue
                }
                require(
                    code !in
                        0xDC00..0xDFFF,
                ) {
                    name +
                        " contains an unpaired low surrogate"
                }
                index += 1
            }
        }

        private fun quote(
            value: String,
        ): String {
            validateUnicode(
                value,
                "string",
            )
            val builder =
                StringBuilder()
            builder.append(
                '"',
            )
            var index =
                0
            while (
                index <
                value.length
            ) {
                val char =
                    value[
                        index
                    ]
                when (
                    char
                ) {
                    '"' ->
                        builder.append(
                            "\\\"",
                        )
                    '\\' ->
                        builder.append(
                            "\\\\",
                        )
                    '\b' ->
                        builder.append(
                            "\\b",
                        )
                    '\u000C' ->
                        builder.append(
                            "\\f",
                        )
                    '\n' ->
                        builder.append(
                            "\\n",
                        )
                    '\r' ->
                        builder.append(
                            "\\r",
                        )
                    '\t' ->
                        builder.append(
                            "\\t",
                        )
                    else -> {
                        if (
                            char.code <
                            0x20
                        ) {
                            builder.append(
                                "\\u",
                            )
                            builder.append(
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
                            builder.append(
                                char,
                            )
                            if (
                                char.code in
                                0xD800..0xDBFF
                            ) {
                                builder.append(
                                    value[
                                        index +
                                            1
                                    ],
                                )
                                index += 1
                            }
                        }
                    }
                }
                index += 1
            }
            builder.append(
                '"',
            )
            return builder
                .toString()
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString(
            "",
        ) {
                byte ->
            "%02x".format(
                byte.toInt() and
                    0xff,
            )
        }

    private fun String.hexToBytes(): ByteArray {
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
            ).toInt(
                16,
            ).toByte()
        }
    }

    private val HEX_32_BYTES =
        Regex(
            "^[0-9a-fA-F]{64}$",
        )
}
