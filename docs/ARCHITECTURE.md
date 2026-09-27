# LudoProof Architecture

LudoProof is split into three trust domains.

```text
Android client
  |
  | player session token
  | client randomness commitment / reveal
  v
Cloudflare Worker: ludoproof-game-api
  |
  | authoritative match rules/state
  | protected EntroNex bearer credential
  v
EntroNex v4 evaluation Worker
  |
  v
commitment + verifiable outcome proof
```

## Security boundaries

The Android APK never contains the EntroNex customer bearer token.

The game Worker is authoritative for:

- player seats and colors;
- turn order;
- random-event index allocation;
- legal token moves;
- captures;
- safe cells;
- extra turns;
- three-consecutive-six handling;
- exact-roll home entry;
- winner determination;
- retry/reconnect semantics.

EntroNex is responsible for the committed random outcome transcript.

## Random event lifecycle

1. Android generates a fresh 32-byte client seed.
2. Android sends only its SHA-256 v4 client commitment.
3. The authoritative match room freezes the pre-roll game state.
4. The room binds `applicationId + matchId + DICE_ROLL + eventIndex`.
5. The room asks EntroNex to create the v4 round.
6. Android receives the server commitment.
7. Android reveals the original client seed.
8. The room resolves the same EntroNex round.
9. The room checks proof context and asks the EntroNex verifier to recompute the mathematical proof.
10. The verified outcome is attached to the turn.
11. Only a legal token may consume the result.
12. The result/proof digest is preserved in match history.

A timeout or reconnect does not allocate a replacement random event.

## Ruleset v1

The first playable ruleset intentionally stays deterministic and explicit:

- 2-4 players;
- four tokens per player;
- six required to leave yard;
- exact roll required for home;
- extra turn on six;
- extra turn on capture;
- third consecutive six forfeits that roll and passes the turn;
- safe global cells: 0, 8, 13, 21, 26, 34, 39, 47;
- no capture on safe cells.

Blockades/stack restrictions are intentionally not part of v1 yet. They must be added as a new reviewed ruleset revision instead of silently changing existing match semantics.

## Current assurance level

This repository is an MVP/evaluation game integration.

EntroNex v4 is still pre-independent-audit and the current game backend uses the EntroNex evaluation Worker. Do not describe LudoProof as certified, gambling-certified, or production audited.
