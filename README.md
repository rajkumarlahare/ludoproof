# LudoProof

LudoProof is an Android Ludo game being built around **verifiable EntroNex dice outcomes**.

Current status: **release candidate / evaluation**. The app and game backend are being hardened for production operation, while EntroNex v4 remains pre-independent-audit.

The repository contains:

- `android/` — native Kotlin Android client;
- `server/` — authoritative Cloudflare Worker + Durable Object match backend;
- `docs/` — protocol and architecture notes.

## Core rule

The Android app does not decide the dice result.

For every roll:

```text
Android creates fresh client seed
        |
        v
client commitment
        |
        v
LudoProof authoritative match Worker
        |
        v
EntroNex v4 commitment
        |
        v
Android reveals original client seed
        |
        v
EntroNex proof
        |
        v
game Worker verifies exact event binding
+ frozen local v4 mathematics
+ pinned Ed25519 attestation
        |
        v
legal Ludo move
```

A network retry reuses the same logical random event. The game never requests a replacement roll because a result is undesirable. The Android dice animation is visual only and cannot change, reroll, weight, or select the authoritative outcome.

## Playable v1 rules

- 2–4 players
- 4 tokens each
- roll 6 to leave yard
- exact roll required to reach home
- extra turn on 6
- extra turn on capture
- third consecutive 6 forfeits the roll and passes the turn
- safe cells cannot be captured
- server validates every move

Blockade/stack restrictions are not yet part of ruleset v1.

## Android

Package:

```text
com.ludoproof.game
```

The Android MVP now supports:

- create/join/share match and waiting-room player display;
- a native interactive 15×15 Ludo board;
- server-authoritative token positions and legal-token highlighting;
- tap-to-move only for server-returned legal tokens;
- presentation-only dice animation whose final face is the verified server outcome;
- turn/winner banners and EntroNex verified-roll status;
- recent round/proof history;
- persisted match/player session with the bearer token encrypted by Android Keystore and AAD-bound to its match/player metadata;
- encrypted pending client-seed recovery with Android Keystore, AAD-bound to its match/commitment metadata, so a reconnect resumes the same logical roll;
- cleartext network traffic blocked and app backup disabled for sensitive local game state;
- foreground-only automatic state sync so multiplayer turns, moves, verified rolls, and winner state refresh without manual polling.

The current API URL compiled into the app is:

```text
https://ludoproof-game-api.ai-8f3.workers.dev
```

## Backend

The isolated Cloudflare Worker name is:

```text
ludoproof-game-api
```

It uses two isolated SQLite-backed Durable Object classes:

```text
LUDOPROOF_MATCHES -> MatchRoom
LUDOPROOF_API_GATE -> ApiGate
```

`ApiGate` provides per-client, per-scope request throttling without storing raw client IP addresses.

Existing EntroNex, Rekixo, AR3D, domains, DNS, and Workers are not modified by this project.

### Required server configuration

The game Worker needs a server-only EntroNex customer bearer token at runtime:

```text
ENTRONEX_API_TOKEN
```

You do **not** need the project on a local PC to create it. The production GitHub workflow inspects both Workers. If neither side has a customer credential yet, it generates a new `ent_cf_eval_...` token inside GitHub Actions, masks it immediately, writes the same value to `ENTRONEX_EVAL_CUSTOMER_TOKEN` on `entronex-v4-eval` and `ENTRONEX_API_TOKEN` on `ludoproof-game-api`, and never prints the value. If both Workers already have the credential, it preserves them. A one-sided mismatch fails closed and requires an explicit synchronized rotation.

The retry-safe session HMAC is also a Cloudflare secret:

```text
LUDOPROOF_SESSION_HMAC_KEY
```

During automated deployment, an operator-supplied HMAC is used when provided. Otherwise the workflow preserves an existing Cloudflare secret and, on the first deployment only, generates a cryptographically random 48-byte value directly in the deployment channel without printing it.

**Never put either runtime secret in Android source, BuildConfig, the repository, or chat.**

The EntroNex signing identity is now source-pinned from the verified live public-trust export. The current public fingerprint is:

```text
1fe248e8ee9129fcd13c1ee96c1cd7d162a610e9b1df3037a4ec48935f2d43c4
```

The key ID, tenant ID, fingerprint, and base64 SPKI public key are committed as non-secret Worker variables in `server/wrangler.json`. A signing-key rotation requires a new verified EntroNex public-trust export and a reviewed pin update; deployment must not silently learn a new trust root from a proof response.

The non-secret EntroNex base URL remains:

```text
ENTRONEX_BASE_URL=https://entronex-v4-eval.ai-8f3.workers.dev
```

## Cloudflare Git deployment

Create a new Cloudflare Worker from this repository only.

Use:

```text
Worker name: ludoproof-game-api
Production branch: main
Root directory: /server
Build command: (empty)
Deploy command: npx --yes wrangler@4.135.0 deploy
```

Keep the Worker isolated on `workers.dev` during the current evaluation stage. Do not attach existing Rekixo/AR3D domains or routes.

## Local checks

Server:

```bash
cd server
npm test
npx --yes wrangler@4.135.0 deploy --dry-run
```

Android:

```bash
gradle -p android :app:testDebugUnitTest
gradle -p android :app:lintRelease
gradle -p android :app:assembleDebug
gradle -p android :app:assembleRelease
gradle -p android :app:bundleRelease
```

GitHub Actions gates server tests, Wrangler dry-run, Android JVM unit tests, release lint, debug/release APK builds, release AAB bundling, SBOM generation, and SHA-256 release evidence automatically. CI release artifacts are intentionally not store-signed; upload/store signing remains an external release-key operation.

Deployment readiness can be checked with:

```text
GET /ready
```

It returns HTTP 503 until both Durable Object bindings, the retry-safe session HMAC key, pinned EntroNex signing identity, EntroNex bearer credential, and a reachable EntroNex health endpoint are all available.

## Security boundary

Player session tokens are random bearer credentials generated by the game backend and stored only as hashes in the match Durable Object.

The EntroNex bearer credential exists only on the server.

The game backend binds every roll to:

```text
applicationId = ludoproof
matchId
eventType = DICE_ROLL
eventIndex
actorHash
previousStateHash
rulesetHash
```

## Important status

LudoProof is now structured as a production release candidate, but it must not be represented as cryptographically production-certified yet.

EntroNex v4 is still an evaluation candidate pending independent cryptographic review. LudoProof must not be described as independently audited, certified, or gambling-certified at this stage.

See [Architecture](docs/ARCHITECTURE.md) and [Production release checklist](docs/PRODUCTION_RELEASE.md).
