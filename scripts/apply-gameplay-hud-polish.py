from pathlib import Path
import re


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


# 1) Back button: use a proper left arrow and remove font padding so the glyph sits
# exactly in the circular action's center instead of riding low/off-center.
common = "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/common/OfflineCommonUi.kt"
replace_once(
    common,
    '''            Button(this@backHeader).apply {
                LudoProofTheme.homeCircularAction(this, "‹")
                contentDescription = "Back"''',
    '''            Button(this@backHeader).apply {
                LudoProofTheme.homeCircularAction(this, "←")
                includeFontPadding = false
                textSize = 28f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)
                contentDescription = "Back"''',
)

# 2) Player rails must reach the device edges. The board stays completely untouched.
screen = "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineGameplayScreenUi.kt"
text = read(screen)
for target in ("topPlayerRail", "bottomPlayerRail"):
    pattern = rf'''        requireNotNull\({target}\),\n        gameplaySectionParams\(\n            if \(isCompactSetup\(\)\) 0 else 2,\n        \)\.apply \{{\n            leftMargin =\n                sectionSideMargin\n            rightMargin =\n                sectionSideMargin\n        \}},'''
    replacement = f'''        requireNotNull({target}),
        gameplaySectionParams(
            if (isCompactSetup()) 0 else 2,
        ),'''
    text, count = re.subn(pattern, replacement, text, count=1)
    if count != 1:
        raise SystemExit(f"{screen}: could not remove side margins for {target}")
write(screen, text)

# 3) Mirror right-edge player cards so the portrait is on the physical device edge
# and text remains inward. Works automatically for both 2-player and 4-player slots.
card = "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsPlayerCardView.kt"
text = read(card)
text = text.replace(
    '''    private val progressText =
        TextView(context)
''',
    '''    private val progressText =
        TextView(context)
    private val copy =
        LinearLayout(context)
''',
    1,
)
text = text.replace(
    '''        val copy =
            LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_VERTICAL
            }
''',
    '''        copy.apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
''',
    1,
)
text = text.replace(
    '''        localPlayer: Boolean = true,
    ) {
        val character =''',
    '''        localPlayer: Boolean = true,
        portraitOnEnd: Boolean = false,
    ) {
        layoutDirection =
            if (portraitOnEnd) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }
        copy.layoutDirection =
            View.LAYOUT_DIRECTION_LTR
        nameText.textDirection =
            View.TEXT_DIRECTION_LTR
        stateText.textDirection =
            View.TEXT_DIRECTION_LTR
        progressText.textDirection =
            View.TEXT_DIRECTION_LTR
        setPadding(
            dp(if (portraitOnEnd) 8 else 2),
            dp(6),
            dp(if (portraitOnEnd) 2 else 8),
            dp(6),
        )

        val character =''',
    1,
)
if "portraitOnEnd" not in text:
    raise SystemExit(f"{card}: portraitOnEnd patch failed")
write(card, text)

# 4) Rail layout: edge-safe cards, vertically centered dice, no clipping shell around dice.
rail = "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflinePlayerRailUi.kt"
text = read(rail)
text = text.replace(
    '''        gravity =
            Gravity.CENTER_VERTICAL
        minimumHeight =''',
    '''        gravity =
            Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        minimumHeight =''',
    1,
)
text = text.replace(
    '''            gravity =
                (
                    if (alignEnd) {
                        Gravity.END
                    } else {
                        Gravity.START
                    }
                    ) or
                    Gravity.CENTER_VERTICAL
        }''',
    '''            gravity =
                (
                    if (alignEnd) {
                        Gravity.END
                    } else {
                        Gravity.START
                    }
                    ) or
                    Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
        }''',
    1,
)
text = text.replace(
    '''                    compact = isCompactSetup(),
                )''',
    '''                    compact = isCompactSetup(),
                    portraitOnEnd = alignEnd,
                )''',
    1,
)
old_control = '''            background =
                LudoProofTheme
                    .rounded(
                        0xECF8FAFF.toInt(),
                        12f,
                        0xFF5FE1FF.toInt(),
                        2f,
                        this@activeDiceControl,
                    )
            elevation =
                dp(7).toFloat()
            setOnClickListener {'''
new_control = '''            // The dice itself is the visual control. Keep this host transparent so
            // no extra "safe" frame surrounds or clips the face/shadow.
            background = null
            elevation = 0f
            clipChildren = false
            clipToPadding = false
            setOnClickListener {'''
if old_control not in text:
    raise SystemExit(f"{rail}: dice shell marker missing")
text = text.replace(old_control, new_control, 1)
old_layout = '''    control.layoutParams =
        LinearLayout.LayoutParams(
            dp(
                if (isCompactSetup()) {
                    52
                } else {
                    58
                },
            ),
            dp(
                if (isCompactSetup()) {
                    52
                } else {
                    58
                },
            ),
        ).apply {
            setMargins(
                dp(4),
                0,
                dp(4),
                dp(12),
            )
        }'''
new_layout = '''    control.layoutParams =
        LinearLayout.LayoutParams(
            dp(
                if (isCompactSetup()) {
                    58
                } else {
                    64
                },
            ),
            dp(
                if (isCompactSetup()) {
                    58
                } else {
                    64
                },
            ),
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
            setMargins(
                dp(2),
                0,
                dp(2),
                0,
            )
        }'''
if old_layout not in text:
    raise SystemExit(f"{rail}: dice layout marker missing")
text = text.replace(old_layout, new_layout, 1)
write(rail, text)

# 5) Dice renderer: keep all shadow/highlight inside the measured view and use a
# lighter modern depth treatment. This prevents the top/bottom from looking cropped.
dice = "android/app/src/main/java/com/ludoproof/game/game/ui/components/DiceView.kt"
text = read(dice)
text = text.replace(
    '''        val shadow =
            RectF(
                size * .11f,
                size * .14f,
                size * .92f,
                size * .94f,
            )''',
    '''        val shadow =
            RectF(
                size * .09f,
                size * .11f,
                size * .91f,
                size * .92f,
            )''',
    1,
)
text = text.replace(
    '''        facePaint.setShadowLayer(
            dp(8f),
            0f,
            dp(4f),
            0x77000000,
        )''',
    '''        facePaint.setShadowLayer(
            dp(3.5f),
            0f,
            dp(2f),
            0x52000000,
        )''',
    1,
)
text = text.replace(
    '''        val inset =
            size * .10f''',
    '''        val inset =
            size * .07f''',
    1,
)
write(dice, text)

# 6) Animal pawns: remove the always-visible colored outer safety ring and white
# backing disk. Keep the legal-move halo because it communicates an actionable move.
paws = "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsBoardView.kt"
text = read(paws)
text, count = re.subn(
    r'''    private val ringPaint =\n        Paint\(Paint\.ANTI_ALIAS_FLAG\)\.apply \{\n            style =\n                Paint\.Style\.FILL\n        \}\n    private val innerPaint =\n        Paint\(Paint\.ANTI_ALIAS_FLAG\)\.apply \{\n            style =\n                Paint\.Style\.FILL\n            color =\n                Color\.argb\(\n                    248,\n                    255,\n                    255,\n                    255,\n                \)\n        \}\n''',
    '',
    text,
    count=1,
)
if count != 1:
    raise SystemExit(f"{paws}: ring paint block replacement failed")
old_ring_draw = '''        ringPaint.color =
            playerColor
        canvas.drawCircle(
            x,
            y,
            radius,
            ringPaint,
        )
        canvas.drawCircle(
            x,
            y,
            radius * 0.84f,
            innerPaint,
        )

        val artRadius =
            radius *
                0.80f'''
new_ring_draw = '''        // Draw only the character art at rest. The old permanent colored/white
        // outer disks made pawns look bulky and dated; legal moves still get a halo.
        val artRadius =
            radius *
                0.96f'''
if old_ring_draw not in text:
    raise SystemExit(f"{paws}: pawn ring draw marker missing")
text = text.replace(old_ring_draw, new_ring_draw, 1)
# playerColor is no longer needed by the rendering function.
text = text.replace(
    '''                    playerColor =
                        playerColor(
                            player.color,
                        ),
                    legal = isLegal,''',
    '''                    legal = isLegal,''',
    1,
)
text = text.replace(
    '''        radius: Float,
        playerColor: Int,
        legal: Boolean,''',
    '''        radius: Float,
        legal: Boolean,''',
    1,
)
write(paws, text)

# 7) Keep a lightweight source gate so these exact UI regressions do not silently return.
gate = "scripts/check-ludo-paws-pawn-rendering.mjs"
text = read(gate)
anchor = '''requireText(
  board,
  "legalHaloPaint",
  "Phase 5 board must keep legal-move highlighting visible around animal pawns.",
);
'''
addition = anchor + '''if (board.includes("ringPaint") || board.includes("innerPaint")) {
  throw new Error(
    "Gameplay pawns must render clean character art without the retired permanent outer safety rings.",
  );
}
'''
if anchor not in text:
    raise SystemExit(f"{gate}: legal halo gate anchor missing")
text = text.replace(anchor, addition, 1)
write(gate, text)

# Sanity guards: geometry files/markers are deliberately not touched by this task.
assert "axisBoundary" not in read(common)
print("Gameplay HUD polish patch applied without changing locked board geometry.")
