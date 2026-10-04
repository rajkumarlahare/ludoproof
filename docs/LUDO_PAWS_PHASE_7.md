# Ludo Paws Phase 7 — Game moments + player-panel gap closure

Phase 7 completes the shared game-moment detection layer while also closing one roadmap gap deliberately discovered during review.

## Roadmap reconciliation

The original migration plan assigns Phase 6 to player character panels and Phase 7 to game-moment detection. PR #91 delivered the first reaction/voice scaffold earlier than that plan, but the existing offline rails were still generic P/CPU badges. To keep the promise that no planned item is skipped, this phase also finishes the missing character-first player rail work before moving to Phase 8.

## Delivered in this phase

- Pure `LudoPawsGameMomentDetector` shared-contract layer over `MatchSnapshot` diffs.
- Moment types for turn start, roll start, six, low roll, yard exit, normal move, capture made, captured victim, safe, home, no legal move, third-six forfeit, meaningful lead change, idle, solo win/loss, and team win/loss.
- Explicit idle API driven by caller-supplied timestamps so the detector remains deterministic and testable.
- Existing Phase 6 reaction engine now consumes the shared moment detector instead of maintaining a second copy of snapshot-diff rules.
- Character-first offline player cards showing selected animal art, player name, turn/team state, and HOME/RACING/YARD progress.
- Phone-safe card/dice sizing for 2/3/4-player rails.
- Unit regression coverage and a dedicated CI gate.

## Safety boundary

This phase does not modify `OfflineGameEngine`, EntroNex v4, legal moves, dice derivation, capture rules, safe cells, exact-home rules, winner logic, proof/fairness history, server protocol, Durable Objects, or persistence schema.

Phase 8 remains responsible for stable reaction keys, reconnect/revision deduplication, cooldowns, queueing, interruption, and deterministic priority/order. This phase intentionally does not pre-empt those responsibilities.
