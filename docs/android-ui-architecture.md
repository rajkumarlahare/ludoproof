# LudoProof Android Project Structure

## Goal

Make every UI and responsibility easy to find in VS Code as the project grows. Activities stay thin; UI, actions, storage, network, proof, and engine code have separate homes.

## Explorer map

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
│  ├─ dialogs/
│  │  ├─ SettingsDialogUi.kt
│  │  ├─ NaturalWorldAuditDialogUi.kt
│  │  ├─ ProofHistoryDialogUi.kt
│  │  ├─ DialogEvidenceComponents.kt
│  │  ├─ DialogMetricComponents.kt
│  │  └─ DialogShellComponents.kt
│  ├─ art/
│  │  ├─ ArcadeBackdropView.kt
│  │  ├─ GameResultArtView.kt
│  │  ├─ HomeHeroArtView.kt
│  │  ├─ ModeArtView.kt
│  │  ├─ NaturalWorldMapView.kt
│  │  ├─ OfflineSetupArtView.kt
│  │  └─ OnlineLobbyArtView.kt
│  ├─ components/
│  │  ├─ DiceView.kt
│  │  └─ LudoBoardView.kt
│  └─ theme/LudoProofTheme.kt
├─ feature/
│  ├─ online/
│  │  ├─ OnlineMatchActions.kt
│  │  ├─ OnlineResponseRenderer.kt
│  │  ├─ OnlineVerificationRenderer.kt
│  │  └─ OnlineSessionActions.kt
│  └─ offline/OfflineGameActions.kt
├─ data/
│  ├─ network/
│  │  ├─ ConnectivityMonitor.kt
│  │  └─ GameApi.kt
│  └─ storage/
│     ├─ CachedMatchStore.kt
│     ├─ PendingOperationStore.kt
│     ├─ PendingRollStore.kt
│     └─ SecureSessionStore.kt
├─ domain/model/GameModels.kt
├─ engine/offline/OfflineGameEngine.kt
└─ proof/
   ├─ core/
   │  ├─ EntroNexV4Local.kt
   │  ├─ LocalSecretBinding.kt
   │  └─ SeedCommitment.kt
   └─ offline/
      ├─ OfflineLudoV4Binding.kt
      └─ OfflineRandomnessAudit.kt
```

## Editing map

- Home profile/connectivity -> `ui/home/HomeHeaderUi.kt`
- Home hero/quick actions -> `ui/home/HomeHeroUi.kt`
- Home modes/footer -> `ui/home/HomeModesUi.kt`
- Online lobby -> `ui/online/OnlineLobbyUi.kt`
- Online match status/result -> `ui/online/OnlineStatusUi.kt`
- Online controls -> `ui/online/OnlineActionUi.kt`
- Online create/join/roll/move -> `feature/online/OnlineMatchActions.kt`
- Online proof rendering -> `feature/online/OnlineVerificationRenderer.kt`
- Offline setup -> `ui/offline/setup/`
- Offline gameplay -> `ui/offline/gameplay/`
- Dialog UI -> `ui/dialogs/`
- Reusable board/dice -> `ui/components/`
- Decorative/custom art -> `ui/art/`
- API/connectivity -> `data/network/`
- Saved sessions/pending operations -> `data/storage/`
- Game models -> `domain/model/`
- Offline game engine -> `engine/offline/`
- Proof/randomness implementation -> `proof/`

## Guardrails

1. Activities own lifecycle and screen-level state only.
2. New major UI panels/screens get their own file.
3. Aim for UI/feature files below roughly 500 lines.
4. Network/storage code never goes into a UI file.
5. Proof/cryptographic algorithms never go into a UI file.
6. Proof/engine algorithm refactors require conformance tests; they are kept cohesive rather than split only to satisfy a line-count target.
7. Existing runtime package/class names stay stable during folder organization, so this is behavior-neutral.
