# Ludo Paws audio replacement guide

This is the quick designer-facing map for changing game audio without editing gameplay code.

## Resolution order

Animal vocals resolve in this order:

1. Situation-specific clip, for example `lp_vocal_dog_capture_01.wav`.
2. Packaged species primitive, for example `lp_vocal_dog_yip.wav`.
3. Built-in generated non-verbal fallback.

Physical SFX resolve in this order:

1. Preferred named clip under `android/app/src/main/res/raw/`.
2. Built-in generated situation-specific fallback.

There are no legacy generic SFX aliases in the runtime path. This prevents unrelated events from accidentally sharing the same historical placeholder WAV.

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

Examples:

- Dog capture: `lp_vocal_dog_capture_01.wav`
- Goat captured: `lp_vocal_goat_captured_01.wav`
- Duck victory: `lp_vocal_duck_victory_01.wav`
- Cat safe: `lp_vocal_cat_safe_01.wav`

Optional second variants use `_02`. If `_02` is absent, the resolver tries the other named clip and then the character fallback.

## Built-in character primitive names

These names can also be supplied as WAV/OGG files to replace the generated default for a whole vocal family:

- `lp_vocal_dog_yip`
- `lp_vocal_dog_whine`
- `lp_vocal_dog_ruff`
- `lp_vocal_goat_bleat`
- `lp_vocal_goat_soft_bleat`
- `lp_vocal_duck_quack`
- `lp_vocal_duck_soft_quack`
- `lp_vocal_cat_chirp`
- `lp_vocal_cat_mew`
- `lp_vocal_cat_purr`
- `lp_vocal_cat_huff`

Situation-specific files always win over these primitive names.

## Physical SFX names

Drop any of these into `android/app/src/main/res/raw/`:

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

## Rules for replacement clips

- Keep animal vocals non-verbal. No spoken English/Hindi/game commentary.
- Prefer short mono clips: roughly 0.1–0.8 seconds.
- Avoid clipping and very loud normalization; character vocals and game SFX can overlap on capture/home events.
- Normal movement should remain subtle. Major events may be brighter/louder.
- Rebuild the Android app after replacing a `res/raw` file.
- Do not put dice outcomes, legal-move decisions, turn logic, or fairness decisions inside audio code.
