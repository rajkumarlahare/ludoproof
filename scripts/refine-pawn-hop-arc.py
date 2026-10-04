from pathlib import Path

board_path = Path("android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsBoardView.kt")
text = board_path.read_text(encoding="utf-8")

old_import = '''import kotlin.math.roundToInt
import kotlin.math.sin
'''
new_import = '''import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
'''
if old_import not in text:
    raise SystemExit("board: sqrt import anchor missing")
text = text.replace(old_import, new_import, 1)

old_radius = '''                val radius =
                    cell *
                        LudoPawsPawnLayout
                            .radiusScale(
                                position = position,
                                occupancy = occupants,
                            )
'''
new_radius = '''                val radius =
                    cell *
                        LudoPawsPawnLayout
                            .radiusScale(
                                position = position,
                                occupancy = occupants,
                            ) *
                        movementHopScale(
                            playerId = player.playerId,
                            tokenIndex = tokenIndex,
                        )
'''
if old_radius not in text:
    raise SystemExit("board: radius marker missing")
text = text.replace(old_radius, new_radius, 1)

old_arc = '''        val hopHeight =
            cell * 0.34f

        return (
            from.first +
                (
                    to.first -
                        from.first
                    ) *
                easedFraction
            ) to
            (
                from.second +
                    (
                        to.second -
                            from.second
                        ) *
                    easedFraction -
                    hopHeight *
                    hopFraction
                )
'''
new_arc = '''        val linearX =
            from.first +
                (to.first - from.first) *
                easedFraction
        val linearY =
            from.second +
                (to.second - from.second) *
                easedFraction

        if (!animation.hopEnabled || hopFraction <= 0f) {
            return linearX to linearY
        }

        // Curve the visual hop slightly toward the board center. Unlike a fixed
        // upward offset, this keeps edge-row pawns inside the clipped board while
        // still giving every cell transition a clear take-off/apex/landing arc.
        val boardCenter =
            cell * 7.5f
        val towardCenterX =
            boardCenter - linearX
        val towardCenterY =
            boardCenter - linearY
        val towardCenterDistance =
            sqrt(
                towardCenterX * towardCenterX +
                    towardCenterY * towardCenterY,
            )
        if (towardCenterDistance <= 0.001f) {
            return linearX to linearY
        }
        val arcOffset =
            cell * 0.22f * hopFraction

        return (
            linearX +
                towardCenterX /
                towardCenterDistance *
                arcOffset
            ) to
            (
                linearY +
                    towardCenterY /
                    towardCenterDistance *
                    arcOffset
                )
'''
if old_arc not in text:
    raise SystemExit("board: old hop arc marker missing")
text = text.replace(old_arc, new_arc, 1)
text = text.replace("return 1f + hop * 0.12f", "return 1f + hop * 0.08f", 1)
board_path.write_text(text, encoding="utf-8")

gate_path = Path("scripts/check-ludo-paws-pawn-rendering.mjs")
gate = gate_path.read_text(encoding="utf-8")
old_gate = '''requireText(
  board,
  "hopHeight",
  "Gameplay pawn movement must use a visible take-off/landing arc instead of rail-like gliding.",
);
'''
new_gate = '''requireText(
  board,
  "towardCenterDistance",
  "Gameplay pawn movement must use a bounded take-off/landing arc instead of rail-like gliding.",
);
'''
if old_gate not in gate:
    raise SystemExit("gate: old hopHeight assertion missing")
gate_path.write_text(gate.replace(old_gate, new_gate, 1), encoding="utf-8")

print("Refined pawn hop to a bounded inward arc with a mild apex scale pulse.")
