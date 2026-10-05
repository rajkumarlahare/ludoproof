# LudoProof Android 1.0.0-rc2

## Purpose

This release candidate prepares the Android client for the Ruleset V2 rollout without activating Ruleset V2 on production matches.

- `versionCode`: `2`
- `versionName`: `1.0.0-rc2`
- production backend default: Ruleset V1
- Android authoritative snapshot parser: V1 + V2 compatible
- billing default: disabled unless explicitly enabled by build property

## Compatibility contract

RC2 must continue to accept all four authoritative ruleset IDs:

- `ludoproof-standard-v1`
- `ludoproof-standard-v2`
- `ludoproof-team-v1`
- `ludoproof-team-v2`

The backend must remain pinned to V1 for newly-created production matches until a V2-capable Android build is distributed to users. Existing V1 proof hashes remain immutable.

## Release signing

Do not commit a keystore or passwords to the repository. Release signing is read only from environment variables:

- `LUDOPROOF_RELEASE_STORE_FILE`
- `LUDOPROOF_RELEASE_STORE_PASSWORD`
- `LUDOPROOF_RELEASE_KEY_ALIAS`
- `LUDOPROOF_RELEASE_KEY_PASSWORD`

The manual `LudoProof Android Signed Release` GitHub Actions workflow expects these repository secrets:

- `LUDOPROOF_ANDROID_KEYSTORE_BASE64`
- `LUDOPROOF_ANDROID_STORE_PASSWORD`
- `LUDOPROOF_ANDROID_KEY_ALIAS`
- `LUDOPROOF_ANDROID_KEY_PASSWORD`

The workflow decodes the keystore only into the ephemeral runner directory, builds the release APK/AAB, verifies their signatures, hashes release evidence, and uploads the signed artifacts. Missing signing secrets cause the workflow to fail instead of falling back to a debug key.

## Required gates before distribution

1. Server tests and repository security gate pass.
2. Android RC2 release-readiness gate passes.
3. Android unit tests pass.
4. Release lint passes.
5. Debug/release APK and release AAB build successfully.
6. Android 16 target verification passes.
7. Emulator instrumentation tests pass.
8. CodeQL JavaScript and Android Kotlin analyses pass.
9. Signed workflow verifies APK and AAB signatures.

## Ruleset V2 rollout sequence

1. Merge RC2 client compatibility and release-signing preparation.
2. Produce and distribute a signed V2-capable Android build.
3. Confirm the deployed client population is compatible.
4. Use a separate, minimal backend rollout PR to make new matches opt into `CLASSIC_V2` / `TEAM_UP_V2`.
5. Deploy the backend through the production workflow and run live `/health`, `/ready`, and real-game smoke checks.
6. Keep V1 verification support permanently for historical match receipts.
