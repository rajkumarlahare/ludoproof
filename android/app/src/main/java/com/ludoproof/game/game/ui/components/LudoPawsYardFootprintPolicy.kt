package com.ludoproof.game

/**
 * A presentation-only footprint left in a token's fixed starting slot.
 *
 * The footprint is derived from the authoritative snapshot instead of a transient
 * animation callback. That makes it deterministic after redraws, reconnects and
 * process restoration, while a token currently back in its yard naturally covers
 * its own footprint.
 */
internal data class LudoPawsYardFootprint(
    val playerId: String,
    val tokenIndex: Int,
    val teamColor: String,
    val species: LudoPaws3DSpecies,
)

internal object LudoPawsYardFootprintPolicy {
    /**
     * Return prints only for tokens that are no longer in their starting yard slot.
     * Character species is resolved by seat assignment first, matching the 3D pawn
     * renderer; team color is used only as its stable compatibility fallback.
     */
    fun footprints(
        snapshot: MatchSnapshot?,
        characterIdsBySeat: List<String>,
    ): List<LudoPawsYardFootprint> {
        if (snapshot == null) return emptyList()

        return buildList {
            snapshot.players.forEach { player ->
                val team =
                    LudoPawsTeamIdentityPolicy.resolve(player.color)
                        ?: return@forEach
                val canonicalColor = team.wireName
                val species =
                    LudoPaws3DCharacterPolicy.speciesForSeat(
                        characterIdsBySeat = characterIdsBySeat,
                        seat = player.seat,
                        fallbackColor = canonicalColor,
                    ) ?: return@forEach

                player.tokens.forEachIndexed { tokenIndex, rawPosition ->
                    val position = LudoPathEncoding.normalizeLegacyEntry(rawPosition)
                    if (position == -1) return@forEachIndexed

                    add(
                        LudoPawsYardFootprint(
                            playerId = player.playerId,
                            tokenIndex = tokenIndex,
                            teamColor = canonicalColor,
                            species = species,
                        ),
                    )
                }
            }
        }
    }
}
