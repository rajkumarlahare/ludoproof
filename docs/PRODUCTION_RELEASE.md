# LudoProof Production Release Checklist

LudoProof is a production-oriented release candidate. The game backend is authoritative and the Android client cannot select the dice outcome, event index, legal move, capture result, turn advancement, or winner.

## Release gates

A release must not be promoted unless all of these are true:

1. GitHub Actions is green for the exact release commit.
2. Server unit/integration tests pass.
3. Wrangler production dry-run passes.
4. Android JVM unit tests and `lintRelease` pass.
5. Android debug/release APK variants and the release AAB assemble successfully.
6. SHA-256 release evidence is generated for the exact release artifacts.
7. Cloudflare `GET /ready` returns HTTP 200.
8. `ENTRONEX_API_TOKEN` and `LUDOPROOF_SESSION_HMAC_KEY` exist only as Cloudflare Secrets.
9. The pinned EntroNex Ed25519 key ID, tenant ID, fingerprint, and public key match the independently verified trust material.
10. The Android APK/AAB contains no EntroNex customer credential or signing secret.
11. The game Worker remains isolated from Rekixo/AR3D routes.
12. A real-device multiplayer smoke test covers create, join, start, commit, reveal, move, reconnect, timeout recovery, win, and proof/history display.
13. The final store/upload-signed Android artifact is signed outside the repository with the controlled release key.

## Cloudflare

Worker:

```text
ludoproof-game-api
```

Durable Objects:

```text
LUDOPROOF_MATCHES -> MatchRoom
LUDOPROOF_API_GATE -> ApiGate
```

The rate gate is sharded by a one-way hash of the Cloudflare client IP plus request scope. Raw client IP addresses are not stored in match state or limiter state.

Required non-secret variables/trust material:

```text
ENTRONEX_BASE_URL=https://entronex-v4-eval.ai-8f3.workers.dev
ENTRONEX_SIGNING_KEY_ID=<independently verified key id>
ENTRONEX_TENANT_ID=<expected tenant>
ENTRONEX_SIGNING_KEY_FINGERPRINT=<64-char SHA-256>
ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64=<base64 public SPKI PEM>
```

Required server-only secrets:

```text
ENTRONEX_API_TOKEN
LUDOPROOF_SESSION_HMAC_KEY
```

Do not commit, log, paste, or embed either secret in the Android app.

Readiness endpoint:

```text
GET /ready
```

The endpoint fails closed with HTTP 503 until the match-storage and rate-gate bindings, retry-safe session HMAC key, EntroNex server credential, pinned EntroNex signing identity, and upstream EntroNex health check are all ready.

## Android release security

The release candidate:

- blocks cleartext network traffic;
- disables Android backup for local game state;
- encrypts the player bearer token with an Android Keystore AES-GCM key and binds its match/player metadata with GCM AAD;
- encrypts the pending unrevealed client seed with Android Keystore and binds its match/commitment metadata with GCM AAD;
- limits API response size;
- enables release shrinking and resource shrinking;
- runs JVM unit tests and release lint in CI;
- builds release APK/AAB candidates and records SHA-256 evidence in CI;
- refreshes multiplayer state automatically only while the Activity is in the foreground.

The release signing key must be managed outside this repository. Never commit a keystore or signing password. CI artifacts are release candidates, not store-signed production packages; final upload/store signing is an external controlled step.

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


## Automated Cloudflare production deployment

The repository contains a gated production workflow:

```text
.github/workflows/deploy-cloudflare.yml
```

Automatic deployment on a `main` push is disabled unless the GitHub repository/environment variable below is explicitly enabled:

```text
CLOUDFLARE_DEPLOY_ENABLED=true
```

The production GitHub environment must contain these secrets:

```text
CLOUDFLARE_API_TOKEN
CLOUDFLARE_ACCOUNT_ID
ENTRONEX_API_TOKEN
LUDOPROOF_SESSION_HMAC_KEY
ENTRONEX_SIGNING_KEY_FINGERPRINT
ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64
```

The last two values are public trust material rather than confidential credentials, but they are stored in the deployment secret channel so the pinned trust root cannot silently drift through a source-code commit.

The workflow validates configuration without printing secret values, runs the repository security gate and server tests, performs a Wrangler production dry-run, deploys the Worker, synchronizes runtime secrets, and only completes successfully after both `/health` and `/ready` pass.

A manual `workflow_dispatch` run is also available. Do not enable continuous deployment until the production environment contains all required values and the pinned EntroNex signing identity has been verified independently.
