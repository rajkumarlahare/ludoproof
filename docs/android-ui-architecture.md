# LudoProof Android UI Architecture

## Goal

Keep Activities thin, keep each visible surface easy to find, and keep game/network/proof behavior out of layout-building files.

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
│  │  ├─ OnlineStatusUi.kt
│  │  ├─ OnlineActionUi.kt
│  │  └─ OnlinePrimitives.kt
│  ├─ offline/
│  │  ├─ common/OfflineCommonUi.kt
│  │  ├─ setup/
│  │  │  ├─ OfflineSetupScreenUi.kt
│  │  │  └─ OfflineSetupSelectionUi.kt
│  │  └─ gameplay/
│  │     ├─ OfflineGameplayScreenUi.kt
│  │     └─ OfflineGameplayControlsUi.kt
│  └─ dialogs/
│     ├─ SettingsDialogUi.kt
│     ├─ NaturalWorldAuditDialogUi.kt
│     ├─ ProofHistoryDialogUi.kt
│     ├─ DialogEvidenceComponents.kt
│     ├─ DialogMetricComponents.kt
│     └─ DialogShellComponents.kt
└─ feature/
   ├─ online/
   │  ├─ OnlineMatchActions.kt
   │  ├─ OnlineResponseRenderer.kt
   │  ├─ OnlineVerificationRenderer.kt
   │  └─ OnlineSessionActions.kt
   └─ offline/
      └─ OfflineGameActions.kt
```

## Editing map

- Home profile/connectivity -> `ui/home/HomeHeaderUi.kt`
- Home hero/quick actions -> `ui/home/HomeHeroUi.kt`
- Home modes/footer -> `ui/home/HomeModesUi.kt`
- Online lobby -> `ui/online/OnlineLobbyUi.kt`
- Online match status/result -> `ui/online/OnlineStatusUi.kt`
- Online buttons/action controls -> `ui/online/OnlineActionUi.kt`
- Online create/join/roll/move -> `feature/online/OnlineMatchActions.kt`
- Online response application -> `feature/online/OnlineResponseRenderer.kt`
- Online verification/proof rendering -> `feature/online/OnlineVerificationRenderer.kt`
- Offline setup screen -> `ui/offline/setup/OfflineSetupScreenUi.kt`
- Offline player/color choices -> `ui/offline/setup/OfflineSetupSelectionUi.kt`
- Offline gameplay HUD/result -> `ui/offline/gameplay/OfflineGameplayScreenUi.kt`
- Offline gameplay controls/render -> `ui/offline/gameplay/OfflineGameplayControlsUi.kt`
- Offline roll/history/status -> `feature/offline/OfflineGameActions.kt`
- Dialogs -> one file per dialog plus small shared component files

## Guardrails

1. Activities own lifecycle and screen-level state only.
2. New major panels/screens get a dedicated file.
3. Aim for UI/feature files below ~500 lines; split by visible responsibility before they become hard to scan.
4. UI files do not own cryptographic/proof algorithms.
5. Proof/engine refactors require conformance tests and stay separate from visual refactors.
6. Runtime Activity names stay stable during this refactor.
