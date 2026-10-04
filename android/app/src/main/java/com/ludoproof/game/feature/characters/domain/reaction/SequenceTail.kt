package com.ludoproof.game.feature.characters.domain.reaction

/**
 * Small bounded tail helper for finite presentation-history sequences.
 *
 * Kotlin's Sequence API intentionally has no standard takeLast operation.
 * This keeps the poor-roll detector allocation bounded without materializing
 * the complete history and is never used by authoritative gameplay logic.
 */
internal fun <T> Sequence<T>.takeLast(
    count: Int,
): List<T> {
    require(count >= 0) {
        "count must be non-negative"
    }
    if (count == 0) {
        return emptyList()
    }

    val tail =
        ArrayDeque<T>(count)
    for (item in this) {
        if (tail.size == count) {
            tail.removeFirst()
        }
        tail.addLast(item)
    }
    return tail.toList()
}
