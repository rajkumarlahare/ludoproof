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

Server-only Cloudflare secrets:

```text
ENTRONEX_API_TOKEN
LUDOPROOF_SESSION_HMAC_KEY
```

`ENTRONEX_API_TOKEN` is the EntroNex evaluation customer bearer token. `LUDOPROOF_SESSION_HMAC_KEY` must be an independently generated random value of at least 32 characters and is used only by the game Worker to reconstruct retry-safe player sessions.

**Never put either secret in Android source, BuildConfig, the repository, or chat.**

Pinned public EntroNex trust material must also be configured on the Worker:

```text
ENTRONEX_SIGNING_KEY_ID
ENTRONEX_TENANT_ID
ENTRONEX_SIGNING_KEY_FINGERPRINT
ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64
```

Obtain the signing fingerprint/public key from an independently verified EntroNex export or release channel. Do not create the trust root by copying only from the same unauthenticated proof response.

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
