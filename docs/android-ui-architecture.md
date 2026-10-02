# LudoProof Android UI Architecture

## Goal

Keep Activities thin. Keep each visible UI surface in its own file. Keep user actions/state transitions outside layout-building code.

## Structure

```text
com/ludoproof/game/
├─ HomeActivity.kt
├─ MainActivity.kt
├─ OfflineGameActivity.kt
├─ ArcadeDialogs.kt
├─ ui/
│  ├─ home/
│  │  ├─ HomeHeaderUi.kt
│  │  ├─ HomeHeroUi.kt
│  │  ├─ HomeModesUi.kt
│  │  └─ HomeLayoutUi.kt
│  ├─ online/
│  │  ├─ OnlineHeaderUi.kt
│  │  ├─ OnlineLobbyUi.kt
│  │  ├─ OnlineMatchUi.kt
│  │  └─ OnlinePrimitives.kt
│  ├─ offline/
│  │  ├─ common/OfflineCommonUi.kt
│  │  ├─ setup/OfflineSetupUi.kt
│  │  └─ gameplay/OfflineGameplayUi.kt
│  └─ dialogs/
│     ├─ SettingsDialogUi.kt
│     ├─ NaturalWorldAuditDialogUi.kt
│     ├─ ProofHistoryDialogUi.kt
│     └─ DialogComponents.kt
└─ feature/
   ├─ online/
   │  ├─ OnlineMatchActions.kt
   │  ├─ OnlineStateRenderer.kt
   │  └─ OnlineSessionActions.kt
   └─ offline/
      └─ OfflineGameActions.kt
```

## Editing map

- Home profile/connectivity -> `ui/home/HomeHeaderUi.kt`
- Home hero/quick actions -> `ui/home/HomeHeroUi.kt`
- Home modes/footer -> `ui/home/HomeModesUi.kt`
- Online lobby -> `ui/online/OnlineLobbyUi.kt`
- Online match/result/action panels -> `ui/online/OnlineMatchUi.kt`
- Online create/join/roll/move -> `feature/online/OnlineMatchActions.kt`
- Online proof/state rendering -> `feature/online/OnlineStateRenderer.kt`
- Offline setup/player/color selection -> `ui/offline/setup/OfflineSetupUi.kt`
- Offline gameplay HUD/board/actions -> `ui/offline/gameplay/OfflineGameplayUi.kt`
- Offline roll/history/status -> `feature/offline/OfflineGameActions.kt`
- Each dialog -> its own file under `ui/dialogs/`

## Guardrails

1. Activities own lifecycle and screen-level state, not long UI builders.
2. New major panels/screens get a dedicated file.
3. Prefer a split before a file grows beyond roughly 500 lines.
4. UI files must not own cryptographic/proof algorithms.
5. Proof/engine refactors require conformance tests and should remain separate from visual refactors.
6. Runtime Activity class names stay stable during this refactor.
