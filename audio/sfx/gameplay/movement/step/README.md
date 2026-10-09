# Movement step SFX

Put short cell-landing / footfall sounds in this folder:

- lp_sfx_step_01.wav
- lp_sfx_step_02.wav
- lp_sfx_step_03.wav

Preferred source: WAV PCM 16-bit mono at 44.1 or 48 kHz. Aim for a soft, clean 40–75 ms contact with little tail. This is a landing/footfall, not a jump whoosh.

Optional character-specific overrides live in sibling folders dog/, goat/, duck/ and cat/. The resolver tries the selected character's actual files, then this shared family, then legacy raw/procedural fallback. Existing numbered variants rotate to add variety.

Jump take-off remains in audio/sfx/gameplay/movement/jump/. The renderer triggers jump audio at hop take-off and this family on landing; do not put the same jump sound in both folders.
