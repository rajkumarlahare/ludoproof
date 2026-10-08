# Ludo Paws audio replacement guide

This is the quick designer-facing map for changing game audio without editing gameplay code.

## Source-of-truth audio folder

Authored production audio now lives at the repository root under:

`audio/`

The Android build automatically mirrors supported `.wav` and `.ogg` files
from this tree into APK assets. You therefore push the source file once from
your local repository; do **not** manually copy it into `android/app/src/main/res/raw/`.

Runtime resolution for physical SFX is:

1. authored repository audio under `/audio/`,
2. stable `res/raw` resource name,
3. built-in generated situation-specific fallback.

This keeps audio files editable without touching gameplay code while preserving
a safe fallback before a clip is uploaded.

## Pawn jump — first asset

Push this file from the local repo:

`audio/sfx/gameplay/movement/jump/lp_sfx_jump_01.wav`

Optional variants:

- `lp_sfx_jump_02.wav`
- `lp_sfx_jump_03.wav`

No Kotlin change is needed to replace or add these numbered jump clips.

## Planned physical SFX folders

Use one behavior per folder:

- `audio/sfx/ui/click/`
- `audio/sfx/gameplay/movement/jump/`
- `audio/sfx/gameplay/dice/`
- `audio/sfx/gameplay/yard_exit/`
- `audio/sfx/gameplay/capture/`
- `audio/sfx/gameplay/safe/`
- `audio/sfx/gameplay/home_lane/`
- `audio/sfx/gameplay/home/`
- `audio/sfx/gameplay/fail/`
- `audio/sfx/gameplay/third_six/`
- `audio/sfx/gameplay/victory/`
- `audio/sfx/gameplay/defeat/`

Animal voices remain separately organized by character under
`audio/voices/<character>/`.

## Existing stable raw fallbacks

The following names remain supported as fallback/compatibility points:

- `lp_sfx_ui_click`
- `lp_sfx_dice_roll`
- `lp_sfx_move_paw`
- `lp_sfx_move_hoof`
- `lp_sfx_move_web`
- `lp_sfx_six`
- `lp_sfx_yard_exit`
- `lp_sfx_capture_impact`
- `lp_sfx_safe_shimmer`
- `lp_sfx_home_lane`
- `lp_sfx_home_sparkle`
- `lp_sfx_fail_soft`
- `lp_sfx_third_six`
- `lp_sfx_victory_sting`
- `lp_sfx_defeat_sting`

## Character situation overrides

Use `lp_vocal_<character>_<cue>_<variant>.wav` or `.ogg`.

Characters: `dog`, `goat`, `duck`, `cat`.

Cues:

- `six`
- `yard_exit`
- `capture`
- `captured`
- `safe`
- `home_lane`
- `home`
- `frustrated`
- `third_six`
- `idle`
- `nervous`
- `proud`
- `victory`
- `defeat`

## Replacement rules

- Keep animal vocals non-verbal.
- Prefer short mono clips: roughly 0.1–0.8 seconds.
- Avoid clipping and very loud normalization.
- Normal movement should remain subtle.
- Rebuild the Android app after replacing an audio file.
- Do not put dice outcomes, legal-move decisions, turn logic, or fairness decisions inside audio code.
