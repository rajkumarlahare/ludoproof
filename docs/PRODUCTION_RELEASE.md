# LudoProof Production Release Checklist

LudoProof is a production-oriented release candidate. The game backend is authoritative and the Android client cannot select the dice outcome, event index, legal move, capture result, turn advancement, or winner.

## Release gates

A release must not be promoted unless all of these are true:

1. GitHub Actions is green for the exact release commit.
2. Server unit/integration tests pass.
3. Wrangler production dry-run passes.
4. Android `lintRelease` passes.
5. Android debug and release variants assemble successfully.
6. Cloudflare `GET /ready` returns HTTP 200.
7. `ENTRONEX_API_TOKEN` exists only as a Cloudflare Secret.
8. The Android APK/AAB contains no EntroNex customer credential.
9. The game Worker remains isolated from Rekixo/AR3D routes.
10. A real-device multiplayer smoke test covers create, join, start, commit, reveal, move, reconnect, win, and proof/history display.

## Cloudflare

Worker:

```text
ludoproof-game-api
```

Durable Object:

```text
LUDOPROOF_MATCHES -> MatchRoom
```

Required non-secret variable:

```text
ENTRONEX_BASE_URL=https://entronex-v4-eval.ai-8f3.workers.dev
```

Required secret:

```text
ENTRONEX_API_TOKEN
```

Do not commit, log, paste, or embed the secret in the Android app.

Readiness endpoint:

```text
GET /ready
```

The endpoint fails closed with HTTP 503 until the match storage binding and EntroNex server credential are configured.

## Android release security

The release candidate:

- blocks cleartext network traffic;
- disables Android backup for local game state;
- encrypts the player bearer token with an Android Keystore AES-GCM key;
- encrypts the pending unrevealed client seed with Android Keystore;
- limits API response size;
- enables release shrinking and resource shrinking;
- runs release lint in CI.

The release signing key must be managed outside this repository. Never commit a keystore or signing password.

## Match lifecycle

Waiting and active matches expire after seven days without a game mutation.

Finished matches expire after 24 hours.

Reads do not extend the lifetime. Real game mutations schedule a new Durable Object alarm.

## Randomness invariants

Do not add any of the following:

- anti-repeat logic;
- anti-streak logic;
- recent-result weighting;
- reroll-until-different behavior;
- replacement rolls after network failure;
- client-selected event indexes.

Repeated dice results are valid IID outcomes.

The same logical event is identified by:

```text
applicationId + matchId + eventType + eventIndex
```

A retry must resume the same pending round.

## Ruleset

Current ruleset:

```text
ludoproof-standard-v1
```

Do not silently add blockade/stack behavior or otherwise change gameplay semantics under this ID. A rules change requires a reviewed new ruleset identifier and corresponding tests.

## Cryptographic claim boundary

EntroNex v4 remains an evaluation candidate pending independent cryptographic review.

Therefore the game may describe a roll as mathematically verified against the current EntroNex v4 proof implementation, but must not claim that the system is independently audited, certified, gambling-certified, or production-certified by a third party.

Independent review remains a release requirement for high-assurance or regulated use.
