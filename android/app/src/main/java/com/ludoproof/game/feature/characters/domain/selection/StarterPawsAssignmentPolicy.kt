package com.ludoproof.game.feature.characters.domain.selection

import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog

/**
 * Presentation-only assignment policy for the Phase 4 Starter Paws pack.
 * It never participates in dice, legal-move, turn, capture, CPU, or winner logic.
 */
object StarterPawsAssignmentPolicy {
    private const val MIN_PLAYERS = 2
    private const val MAX_PLAYERS = 4

    val starterCharacterIds: List<String>
        get() =
            LudoPawsCharacterCatalog
                .charactersForPack(
                    LudoPawsCharacterCatalog.STARTER_PACK_ID,
                )
                .map { character -> character.id }

    fun normalize(
        playerCount: Int,
        requestedCharacterIds: List<String>,
        preferredCharacterId: String,
        computerMode: Boolean,
    ): List<String> {
        requirePlayerCount(playerCount)

        val available = starterCharacterIds
        require(available.size >= playerCount) {
            "Starter Paws must contain at least $playerCount characters"
        }

        val safePreferred =
            preferredCharacterId
                .takeIf(available::contains)
                ?: LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
                    .takeIf(available::contains)
                ?: available.first()

        if (computerMode) {
            return buildList(playerCount) {
                add(safePreferred)
                available
                    .asSequence()
                    .filterNot { characterId -> characterId == safePreferred }
                    .take(playerCount - 1)
                    .forEach(::add)
            }
        }

        val normalized = MutableList<String?>(playerCount) { null }
        val used = linkedSetOf<String>()

        for (slot in 0 until playerCount) {
            val requested = requestedCharacterIds.getOrNull(slot)
            val candidate =
                when {
                    requested != null &&
                        requested in available &&
                        requested !in used -> requested

                    slot == 0 && safePreferred !in used -> safePreferred
                    else -> null
                }

            if (candidate != null) {
                normalized[slot] = candidate
                used += candidate
            }
        }

        for (slot in 0 until playerCount) {
            if (normalized[slot] != null) continue
            val fallback =
                available.firstOrNull { characterId -> characterId !in used }
                    ?: available[slot % available.size]
            normalized[slot] = fallback
            used += fallback
        }

        return normalized.map { characterId ->
            requireNotNull(characterId)
        }
    }

    fun select(
        playerCount: Int,
        currentCharacterIds: List<String>,
        selectedSlot: Int,
        requestedCharacterId: String,
        computerMode: Boolean,
    ): List<String> {
        requirePlayerCount(playerCount)
        require(selectedSlot in 0 until playerCount) {
            "Character slot $selectedSlot is outside the active player range"
        }

        val available = starterCharacterIds
        if (requestedCharacterId !in available) {
            return normalize(
                playerCount = playerCount,
                requestedCharacterIds = currentCharacterIds,
                preferredCharacterId = currentCharacterIds.firstOrNull()
                    ?: LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID,
                computerMode = computerMode,
            )
        }

        if (computerMode) {
            // Only the human slot is directly selectable in Computer mode.
            val humanCharacterId =
                if (selectedSlot == 0) {
                    requestedCharacterId
                } else {
                    currentCharacterIds.firstOrNull()
                        ?: LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
                }
            return normalize(
                playerCount = playerCount,
                requestedCharacterIds = listOf(humanCharacterId),
                preferredCharacterId = humanCharacterId,
                computerMode = true,
            )
        }

        val current =
            normalize(
                playerCount = playerCount,
                requestedCharacterIds = currentCharacterIds,
                preferredCharacterId = currentCharacterIds.firstOrNull()
                    ?: LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID,
                computerMode = false,
            )
                .toMutableList()

        val previousCharacterId = current[selectedSlot]
        if (previousCharacterId == requestedCharacterId) {
            return current
        }

        val ownerSlot = current.indexOf(requestedCharacterId)
        current[selectedSlot] = requestedCharacterId

        if (ownerSlot >= 0 && ownerSlot != selectedSlot) {
            // Swap instead of silently creating duplicates. This keeps every
            // active local player visually distinct while preserving intent.
            current[ownerSlot] = previousCharacterId
        }

        return current
    }

    private fun requirePlayerCount(
        playerCount: Int,
    ) {
        require(playerCount in MIN_PLAYERS..MAX_PLAYERS) {
            "Ludo Paws local character assignment supports 2 to 4 players"
        }
    }
}
