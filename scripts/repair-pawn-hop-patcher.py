from pathlib import Path

path = Path("android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsBoardView.kt")
text = path.read_text(encoding="utf-8")

old_imports = '''import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
'''
new_imports = '''import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
'''
if old_imports not in text:
    raise SystemExit("board: math import marker missing")
text = text.replace(old_imports, new_imports, 1)

old_speed = '''        val speed =
            GameSettingsStore(context)
                .snapshot()
                .gameSpeed
'''
new_speed = '''        val settings =
            GameSettingsStore(context)
                .snapshot()
        val speed =
            settings.gameSpeed
'''
if old_speed not in text:
    raise SystemExit("board: speed settings marker missing")
text = text.replace(old_speed, new_speed, 1)

old_move = '''        moveAnimation =
            movement
'''
new_move = '''        moveAnimation =
            movement.copy(
                hopEnabled =
                    !settings.reducedMotionEnabled,
            )
'''
if old_move not in text:
    raise SystemExit("board: move animation marker missing")
text = text.replace(old_move, new_move, 1)

path.write_text(text, encoding="utf-8")
print("Repaired pawn hop patch ordering and reduced-motion wiring.")
