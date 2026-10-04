from pathlib import Path


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected 1 match, found {count}")
    write(path, text.replace(old, new, 1))


board = "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsBoardView.kt"
text = read(board)

# Add the math used by the per-cell frog-hop arc.
replace_once(
    board,
    '''import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
''',
    '''import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
''',
)

# Make each animated step visually lift and grow, while keeping reduced-motion respected.
replace_once(
    board,
    '''                val radius =
                    cell *
                        LudoPawsPawnLayout
                            .radiusScale(
                                position = position,
                                occupancy = occupants,
                            )
''',
    '''                val radius =
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
''',
)

replace_once(
    board,
    '''        val speed =
            GameSettingsStore(context)
                .snapshot()
                .gameSpeed
''',
    '''        val settings =
            GameSettingsStore(context)
                .snapshot()
        val speed =
            settings.gameSpeed
''',
)

replace_once(
    board,
    '''        moveAnimation =
            movement
''',
    '''        moveAnimation =
            movement.copy(
                hopEnabled =
                    !settings.reducedMotionEnabled,
            )
''',
)

old_return = '''        return (
            from.first +
                (
                    to.first -
                        from.first
                    ) *
                fraction
            ) to
            (
                from.second +
                    (
                        to.second -
                            from.second
                        ) *
                    fraction
                )
'''
new_return = '''        // Move one board cell at a time with a visible take-off and landing.
        // Smoothstep removes the old rail-like constant-speed glide; the sine lift
        // gives every visual step a small frog-hop arc before it lands in the next cell.
        val easedFraction =
            fraction *
                fraction *
                (3f - 2f * fraction)
        val hopFraction =
            if (animation.hopEnabled) {
                sin(
                    PI *
                        fraction.toDouble(),
                ).toFloat()
            } else {
                0f
            }
        val hopHeight =
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
if old_return not in text:
    raise SystemExit(f"{board}: animated interpolation marker missing")
text = text.replace(old_return, new_return, 1)

helper_anchor = '''    // TOKEN POSITION MIRROR LOCK: mirror the corrected shared-track exit exactly.
    private fun tokenCenter(
'''
helper = '''    private fun movementHopScale(
        playerId: String,
        tokenIndex: Int,
    ): Float {
        val animation =
            moveAnimation
                ?.takeIf {
                    it.playerId == playerId &&
                        it.tokenIndex == tokenIndex &&
                        it.hopEnabled
                }
                ?: return 1f
        val whole =
            floor(animation.progress)
        val fraction =
            (animation.progress - whole)
                .coerceIn(0f, 1f)
        val hop =
            sin(
                PI *
                    fraction.toDouble(),
            ).toFloat()
                .coerceAtLeast(0f)
        return 1f + hop * 0.12f
    }

    // TOKEN POSITION MIRROR LOCK: mirror the corrected shared-track exit exactly.
    private fun tokenCenter(
'''
if helper_anchor not in text:
    raise SystemExit(f"{board}: tokenCenter anchor missing")
text = text.replace(helper_anchor, helper, 1)

replace_once(
    board,
    '''        val toPosition: Int,
        val progress: Float,
    )
''',
    '''        val toPosition: Int,
        val progress: Float,
        val hopEnabled: Boolean = true,
    )
''',
)
write(board, text)

# Keep the dice bright and readable against the dark gameplay HUD in every cosmetic theme.
dice = "android/app/src/main/java/com/ludoproof/game/game/ui/components/DiceView.kt"
text = read(dice)
text = text.replace("0x55000000", "0x38000000", 1)
text = text.replace("0x52000000", "0x3D000000", 1)

palette_updates = {
    '''                        0xFFF6A24D.toInt(),
                        0xFFD16D25.toInt(),
                        0xFFA84819.toInt(),''': '''                        0xFFFFD59A.toInt(),
                        0xFFFFAD55.toInt(),
                        0xFFF47A2A.toInt(),''',
    '''                        0xFFE2463D.toInt(),
                        0xFFB51E39.toInt(),
                        0xFF7C1930.toInt(),''': '''                        0xFFFF9A90.toInt(),
                        0xFFF45361.toInt(),
                        0xFFC92D48.toInt(),''',
    '''                        0xFFE6F5DA.toInt(),
                        0xFF73B86A.toInt(),
                        0xFF2F6F47.toInt(),''': '''                        0xFFF5FFE9.toInt(),
                        0xFFA8D99A.toInt(),
                        0xFF62A978.toInt(),''',
    '''                        0xFFFFD83D.toInt(),
                        0xFFEF4F9A.toInt(),
                        0xFF45C7D8.toInt(),''': '''                        0xFFFFEA75.toInt(),
                        0xFFFF7BB5.toInt(),
                        0xFF6ADCE8.toInt(),''',
    '''                        Color.WHITE,
                        0xFFF3F6FA.toInt(),
                        0xFFD5DDE8.toInt(),''': '''                        Color.WHITE,
                        0xFFF9FBFF.toInt(),
                        0xFFEAF0F7.toInt(),''',
}
for old, new in palette_updates.items():
    if old not in text:
        raise SystemExit(f"{dice}: palette marker missing: {old[:30]}")
    text = text.replace(old, new, 1)

# Add a soft upper-face sheen so even themed dice stay legible on the dark blue HUD.
face_anchor = '''        canvas.drawRoundRect(
            rect,
            size * .17f,
            size * .17f,
            facePaint,
        )
        facePaint.shader = null

        borderPaint.color =
'''
face_replacement = '''        canvas.drawRoundRect(
            rect,
            size * .17f,
            size * .17f,
            facePaint,
        )
        facePaint.shader = null
        facePaint.color =
            0x2EFFFFFF
        canvas.drawRoundRect(
            RectF(
                rect.left + size * .035f,
                rect.top + size * .035f,
                rect.right - size * .035f,
                rect.top + rect.height() * .43f,
            ),
            size * .13f,
            size * .13f,
            facePaint,
        )

        borderPaint.color =
'''
if face_anchor not in text:
    raise SystemExit(f"{dice}: face highlight anchor missing")
text = text.replace(face_anchor, face_replacement, 1)
write(dice, text)

# CPU dice should still look disabled, but never become too dim to read.
rail = "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflinePlayerRailUi.kt"
replace_once(
    rail,
    '''        if (cpuTurn) {
            .58f
        } else {
            1f
        }
''',
    '''        if (cpuTurn) {
            .82f
        } else {
            1f
        }
''',
)

# Regression gate for the requested movement behavior.
gate = "scripts/check-ludo-paws-pawn-rendering.mjs"
text = read(gate)
anchor = '''requireText(
  board,
  "startMoveAnimationIfNeeded",
  "Phase 5 animal overlay must follow token movement animation.",
);
'''
addition = anchor + '''requireText(
  board,
  "movementHopScale",
  "Gameplay pawn movement must keep the per-cell frog-hop scale pulse.",
);
requireText(
  board,
  "hopHeight",
  "Gameplay pawn movement must use a visible take-off/landing arc instead of rail-like gliding.",
);
requireText(
  board,
  "reducedMotionEnabled",
  "Gameplay pawn hop must respect the reduced-motion accessibility setting.",
);
'''
if anchor not in text:
    raise SystemExit(f"{gate}: movement gate anchor missing")
text = text.replace(anchor, addition, 1)
write(gate, text)

print("Applied per-cell pawn hop animation and brighter dice contrast without changing board geometry or game rules.")
