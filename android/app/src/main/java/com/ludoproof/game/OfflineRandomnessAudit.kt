package com.ludoproof.game

import org.json.JSONArray
import org.json.JSONObject

data class OfflineRandomnessAudit(
    val eventIndex: Int,
    val roundId: String,
    val serverSeed: String,
    val clientSeed: String,
    val serverCommitment: String,
    val clientCommitment: String,
    val subjectHash: String?,
    val previousStateHash: String?,
    val rulesetHash: String?,
    val configDigest: String,
    val contextDigest: String,
    val eventBindingDigest: String,
    val transcriptDigest: String,
    val outcomeIndex: Int,
    val outcome: Int,
    val proofDigest: String,
    val worldDigest: String,
    val fieldDigest: String,
    val width: Int,
    val height: Int,
    val sampleIndex: Int,
    val sampleTick: Int,
    val layoutEpoch: Int,
    val motionProfile: String,
    val selectedProbe: Int,
    val witnessSwapped: Boolean,
    val witnessSourceIndex: Int,
    val witnessTargetIndex: Int,
    val cellsPerOutcome: Int,
    val timelineTicks: Int,
    val epochCount: Int,
    val probeCount: Int,
    val field: List<Int>,
    val sessionId: String,
) {
    fun verify(): Boolean =
        runCatching {
            val result =
                EntroNexV4Local.resolve(
                    roundId =
                        roundId,
                    serverSeed =
                        serverSeed,
                    clientSeed =
                        clientSeed,
                    config =
                        LocalV4Config(
                            outcomes =
                                listOf(
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    6,
                                ),
                            context =
                                LocalV4Context(
                                    applicationId =
                                        "ludoproof",
                                    sessionId =
                                        sessionId,
                                    eventId =
                                        "roll:" +
                                            eventIndex,
                                    eventType =
                                        "DICE_ROLL",
                                    eventIndex =
                                        eventIndex
                                            .toLong(),
                                    subjectHash =
                                        subjectHash,
                                    previousStateHash =
                                        previousStateHash,
                                    metadataDigest =
                                        rulesetHash,
                                ),
                            world =
                                LocalV4WorldConfig(
                                    cellsPerOutcome =
                                        cellsPerOutcome,
                                    timelineTicks =
                                        timelineTicks,
                                    epochCount =
                                        epochCount,
                                    probeCount =
                                        probeCount,
                                ),
                        ),
                )

            result.serverCommitment ==
                serverCommitment &&
                result.clientCommitment ==
                clientCommitment &&
                result.configDigest ==
                configDigest &&
                result.contextDigest ==
                contextDigest &&
                result.eventBindingDigest ==
                eventBindingDigest &&
                result.transcriptDigest ==
                transcriptDigest &&
                result.outcomeIndex ==
                outcomeIndex &&
                result.outcome ==
                outcome &&
                result.proofDigest ==
                proofDigest &&
                result.world.worldDigest ==
                worldDigest &&
                result.world.fieldDigest ==
                fieldDigest &&
                result.world.field ==
                field
        }.getOrDefault(
            false,
        )

    fun toJson(): JSONObject =
        JSONObject()
            .put(
                "eventIndex",
                eventIndex,
            )
            .put(
                "roundId",
                roundId,
            )
            .put(
                "serverSeed",
                serverSeed,
            )
            .put(
                "clientSeed",
                clientSeed,
            )
            .put(
                "serverCommitment",
                serverCommitment,
            )
            .put(
                "clientCommitment",
                clientCommitment,
            )
            .put(
                "subjectHash",
                subjectHash,
            )
            .put(
                "previousStateHash",
                previousStateHash,
            )
            .put(
                "rulesetHash",
                rulesetHash,
            )
            .put(
                "configDigest",
                configDigest,
            )
            .put(
                "contextDigest",
                contextDigest,
            )
            .put(
                "eventBindingDigest",
                eventBindingDigest,
            )
            .put(
                "transcriptDigest",
                transcriptDigest,
            )
            .put(
                "outcomeIndex",
                outcomeIndex,
            )
            .put(
                "outcome",
                outcome,
            )
            .put(
                "proofDigest",
                proofDigest,
            )
            .put(
                "worldDigest",
                worldDigest,
            )
            .put(
                "fieldDigest",
                fieldDigest,
            )
            .put(
                "width",
                width,
            )
            .put(
                "height",
                height,
            )
            .put(
                "sampleIndex",
                sampleIndex,
            )
            .put(
                "sampleTick",
                sampleTick,
            )
            .put(
                "layoutEpoch",
                layoutEpoch,
            )
            .put(
                "motionProfile",
                motionProfile,
            )
            .put(
                "selectedProbe",
                selectedProbe,
            )
            .put(
                "witnessSwapped",
                witnessSwapped,
            )
            .put(
                "witnessSourceIndex",
                witnessSourceIndex,
            )
            .put(
                "witnessTargetIndex",
                witnessTargetIndex,
            )
            .put(
                "cellsPerOutcome",
                cellsPerOutcome,
            )
            .put(
                "timelineTicks",
                timelineTicks,
            )
            .put(
                "epochCount",
                epochCount,
            )
            .put(
                "probeCount",
                probeCount,
            )
            .put(
                "field",
                JSONArray(
                    field,
                ),
            )
            .put(
                "sessionId",
                sessionId,
            )

    companion object {
        fun fromResult(
            eventIndex: Int,
            result: LocalV4Result,
        ): OfflineRandomnessAudit =
            OfflineRandomnessAudit(
                eventIndex =
                    eventIndex,
                roundId =
                    result.roundId,
                serverSeed =
                    result.serverSeed,
                clientSeed =
                    result.clientSeed,
                serverCommitment =
                    result.serverCommitment,
                clientCommitment =
                    result.clientCommitment,
                subjectHash =
                    result.config
                        .context
                        .subjectHash,
                previousStateHash =
                    result.config
                        .context
                        .previousStateHash,
                rulesetHash =
                    result.config
                        .context
                        .metadataDigest,
                configDigest =
                    result.configDigest,
                contextDigest =
                    result.contextDigest,
                eventBindingDigest =
                    result.eventBindingDigest,
                transcriptDigest =
                    result.transcriptDigest,
                outcomeIndex =
                    result.outcomeIndex,
                outcome =
                    result.outcome,
                proofDigest =
                    result.proofDigest,
                worldDigest =
                    result.world
                        .worldDigest,
                fieldDigest =
                    result.world
                        .fieldDigest,
                width =
                    result.world
                        .width,
                height =
                    result.world
                        .height,
                sampleIndex =
                    result.world
                        .sampleIndex,
                sampleTick =
                    result.world
                        .sampleTick,
                layoutEpoch =
                    result.world
                        .layoutEpoch,
                motionProfile =
                    result.world
                        .motionProfile,
                selectedProbe =
                    result.world
                        .selectedProbe,
                witnessSwapped =
                    result.world
                        .witness
                        .swapped,
                witnessSourceIndex =
                    result.world
                        .witness
                        .sourceIndex,
                witnessTargetIndex =
                    result.world
                        .witness
                        .targetIndex,
                cellsPerOutcome =
                    result.config
                        .world
                        .cellsPerOutcome,
                timelineTicks =
                    result.config
                        .world
                        .timelineTicks,
                epochCount =
                    result.config
                        .world
                        .epochCount,
                probeCount =
                    result.config
                        .world
                        .probeCount,
                field =
                    result.world
                        .field,
                sessionId =
                    result.config
                        .context
                        .sessionId,
            )

        fun fromJson(
            value: JSONObject?,
        ): OfflineRandomnessAudit? {
            if (
                value == null
            ) {
                return null
            }

            return runCatching {
                val fieldJson =
                    value.getJSONArray(
                        "field",
                    )
                val field =
                    buildList {
                        for (
                            index in
                            0 until
                                fieldJson
                                    .length()
                        ) {
                            add(
                                fieldJson
                                    .getInt(
                                        index,
                                    ),
                            )
                        }
                    }

                OfflineRandomnessAudit(
                    eventIndex =
                        value.getInt(
                            "eventIndex",
                        ),
                    roundId =
                        value.getString(
                            "roundId",
                        ),
                    serverSeed =
                        value.getString(
                            "serverSeed",
                        ),
                    clientSeed =
                        value.getString(
                            "clientSeed",
                        ),
                    serverCommitment =
                        value.getString(
                            "serverCommitment",
                        ),
                    clientCommitment =
                        value.getString(
                            "clientCommitment",
                        ),
                    subjectHash =
                        value
                            .optString(
                                "subjectHash",
                            )
                            .takeIf {
                                it.isNotBlank() &&
                                    it !=
                                    "null"
                            },
                    previousStateHash =
                        value
                            .optString(
                                "previousStateHash",
                            )
                            .takeIf {
                                it.isNotBlank() &&
                                    it !=
                                    "null"
                            },
                    rulesetHash =
                        value
                            .optString(
                                "rulesetHash",
                            )
                            .takeIf {
                                it.isNotBlank() &&
                                    it !=
                                    "null"
                            },
                    configDigest =
                        value.getString(
                            "configDigest",
                        ),
                    contextDigest =
                        value.getString(
                            "contextDigest",
                        ),
                    eventBindingDigest =
                        value.getString(
                            "eventBindingDigest",
                        ),
                    transcriptDigest =
                        value.getString(
                            "transcriptDigest",
                        ),
                    outcomeIndex =
                        value.getInt(
                            "outcomeIndex",
                        ),
                    outcome =
                        value.getInt(
                            "outcome",
                        ),
                    proofDigest =
                        value.getString(
                            "proofDigest",
                        ),
                    worldDigest =
                        value.getString(
                            "worldDigest",
                        ),
                    fieldDigest =
                        value.getString(
                            "fieldDigest",
                        ),
                    width =
                        value.getInt(
                            "width",
                        ),
                    height =
                        value.getInt(
                            "height",
                        ),
                    sampleIndex =
                        value.getInt(
                            "sampleIndex",
                        ),
                    sampleTick =
                        value.getInt(
                            "sampleTick",
                        ),
                    layoutEpoch =
                        value.getInt(
                            "layoutEpoch",
                        ),
                    motionProfile =
                        value.getString(
                            "motionProfile",
                        ),
                    selectedProbe =
                        value.getInt(
                            "selectedProbe",
                        ),
                    witnessSwapped =
                        value.getBoolean(
                            "witnessSwapped",
                        ),
                    witnessSourceIndex =
                        value.getInt(
                            "witnessSourceIndex",
                        ),
                    witnessTargetIndex =
                        value.getInt(
                            "witnessTargetIndex",
                        ),
                    cellsPerOutcome =
                        value.getInt(
                            "cellsPerOutcome",
                        ),
                    timelineTicks =
                        value.getInt(
                            "timelineTicks",
                        ),
                    epochCount =
                        value.getInt(
                            "epochCount",
                        ),
                    probeCount =
                        value.getInt(
                            "probeCount",
                        ),
                    field =
                        field,
                    sessionId =
                        value.getString(
                            "sessionId",
                        ),
                )
            }.getOrNull()
        }
    }
}
