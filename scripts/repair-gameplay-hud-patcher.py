from pathlib import Path

path = Path("scripts/apply-gameplay-hud-polish.py")
text = path.read_text(encoding="utf-8")
start = text.index("# 2) Player rails must reach the device edges.")
end = text.index("# 3) Mirror right-edge player cards", start)
replacement = '''# 2) Player rails must reach the device edges. The board stays completely untouched.
screen = "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineGameplayScreenUi.kt"
text = read(screen)
for target, spacing in (("topPlayerRail", "10 else 12"), ("bottomPlayerRail", "6 else 8")):
    old = f''' + "'''" + '''        requireNotNull(
            {target},
        ),
        gameplaySectionParams(
            if (isCompactSetup()) {spacing},
        ).apply {{
            leftMargin =
                sectionSideMargin
            rightMargin =
                sectionSideMargin
        }},''' + "'''" + '''
    new = f''' + "'''" + '''        requireNotNull(
            {target},
        ),
        gameplaySectionParams(
            if (isCompactSetup()) {spacing},
        ),''' + "'''" + '''
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{screen}: expected one side-margin block for {target}, found {count}")
    text = text.replace(old, new, 1)
write(screen, text)

'''
path.write_text(text[:start] + replacement + text[end:], encoding="utf-8")
