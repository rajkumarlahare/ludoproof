# Ludo Paws authored audio

This folder is the **source-of-truth area for manually added production audio**.
Do not put generated build output here.

## Current first asset

Place the first pawn-jump sound here:

`audio/sfx/gameplay/movement/jump/lp_sfx_jump_01.wav`

Optional future variants:

- `lp_sfx_jump_02.wav`
- `lp_sfx_jump_03.wav`

The app tries these authored jump assets first. When none exists, it keeps using the existing movement SFX/procedural fallback, so the game remains playable before the first audio file is pushed.

## Planned structure

```
audio/
└── sfx/
    ├── ui/
    │   └── click/
    ├── gameplay/
    │   ├── movement/
    │   │   └── jump/
    │   ├── dice/
    │   ├── yard_exit/
    │   ├── capture/
    │   ├── safe/
    │   ├── home_lane/
    │   ├── home/
    │   ├── fail/
    │   ├── third_six/
    │   ├── victory/
    │   └── defeat/
    └── voices/
        ├── dog/
        ├── goat/
        ├── duck/
        └── cat/
```

Keep each behavior in its own folder. Use stable semantic filenames such as
`lp_sfx_jump_01.wav`, never generic names such as `sound1.wav`.

## Jump audio format

Preferred first upload:

- WAV
- PCM 16-bit
- mono
- 44.1 kHz or 48 kHz
- about 90–130 ms
- no clipping
- clean, short transient
- no music or ambience

Do not replace Kotlin code just to change the sound. Replace the file in this
folder and rebuild the Android app.
