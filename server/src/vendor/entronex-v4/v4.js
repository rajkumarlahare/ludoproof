// Vendored from rajkumarlahare/entronex@d219148338b43e961689a7a7ab6f8ce3eb88e8de
// Source: src/core/v4.js. Keep byte-for-byte logic aligned with the pinned EntroNex v4 candidate.
import { hkdfSync } from 'node:crypto';
import { assertSeed, secureEqualHex, sha256Hex } from './crypto.js';
import { HmacStream } from './stream.js';
import { buildBalancedField, validateFieldComplexity } from './field.js';
import { jcsCanonicalize } from './jcs.js';

export const ALGORITHM_V4 =
  'entronex-v4-dual-commit-hkdf-sha256-context-bound';
export const NATURAL_WORLD_V1 = 'entronex-natural-world-v1';
export const MAX_V4_WORLD_CELLS = 65_536;

const DEFAULT_WORLD = Object.freeze({
  cellsPerOutcome: 256,
  timelineTicks: 4_096,
  epochCount: 32,
  probeCount: 3,
});

const MOTION_PROFILES = Object.freeze([
  'crosswind',
  'orbital-drift',
  'pulse-reverse',
  'toroidal-flow',
  'scatter-wave',
]);

export function clientCommitmentForSeedV4(seedHex) {
  assertSeed(seedHex);
  return sha256Hex(
    'entronex:v4:client-commit:' + seedHex.toLowerCase(),
  );
}

export function serverCommitmentForSeedV4(seedHex) {
  assertSeed(seedHex);
  return sha256Hex(
    'entronex:v4:server-commit:' + seedHex.toLowerCase(),
  );
}

export function normalizeV4Context(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) {
    throw new TypeError('context must be an object');
  }

  const context = {
    applicationId: boundedText(input.applicationId, 'applicationId', 120),
    sessionId: boundedText(input.sessionId, 'sessionId', 200),
    eventId: boundedText(input.eventId, 'eventId', 200),
    eventType: boundedText(input.eventType, 'eventType', 80),
    eventIndex: boundedNonNegativeInt(input.eventIndex, 'eventIndex'),
    subjectHash: optionalDigest(input.subjectHash, 'subjectHash'),
    previousStateHash: optionalDigest(
      input.previousStateHash,
      'previousStateHash',
    ),
    metadataDigest: optionalDigest(input.metadataDigest, 'metadataDigest'),
  };

  jcsCanonicalize(context);
  return context;
}

export function v4ContextDigest(contextInput) {
  const context = normalizeV4Context(contextInput);
  return sha256Hex(
    'entronex:v4:context:' + jcsCanonicalize(context),
  );
}

export function v4EventBindingDigest(contextInput) {
  const context = normalizeV4Context(contextInput);
  const binding = {
    applicationId: context.applicationId,
    sessionId: context.sessionId,
    eventType: context.eventType,
    eventIndex: context.eventIndex,
  };
  return sha256Hex(
    'entronex:v4:event-binding:' + jcsCanonicalize(binding),
  );
}

export function normalizeV4Config({
  outcomes,
  context,
  world = {},
}) {
  const cellsPerOutcome =
    world.cellsPerOutcome ?? DEFAULT_WORLD.cellsPerOutcome;
  const fieldSize = validateFieldComplexity(
    outcomes,
    cellsPerOutcome,
  );
  if (fieldSize > MAX_V4_WORLD_CELLS) {
    throw new RangeError(
      'v4 natural world exceeds maximum of ' +
        MAX_V4_WORLD_CELLS +
        ' cells',
    );
  }

  const timelineTicks = boundedInt(
    world.timelineTicks,
    DEFAULT_WORLD.timelineTicks,
    256,
    16_384,
    'timelineTicks',
  );
  const epochCount = boundedInt(
    world.epochCount,
    DEFAULT_WORLD.epochCount,
    2,
    128,
    'epochCount',
  );
  const probeCount = boundedInt(
    world.probeCount,
    DEFAULT_WORLD.probeCount,
    1,
    8,
    'probeCount',
  );

  if (epochCount > timelineTicks) {
    throw new RangeError('epochCount cannot exceed timelineTicks');
  }

  return {
    outcomes: structuredClone(outcomes),
    context: normalizeV4Context(context),
    world: {
      cellsPerOutcome,
      timelineTicks,
      epochCount,
      probeCount,
    },
  };
}

export function v4ConfigDigest(configInput) {
  const config = normalizeV4Config(configInput);
  return sha256Hex(
    'entronex:v4:config:' + jcsCanonicalize(config),
  );
}

export function resolveOutcomeV4({
  roundId,
  serverSeed,
  serverCommitment,
  clientSeed,
  clientCommitment,
  contextDigest,
  eventBindingDigest,
  config,
  configDigest,
}) {
  boundedText(roundId, 'roundId', 200);
  assertSeed(serverSeed);
  assertSeed(clientSeed);

  const expectedServerCommitment =
    serverCommitmentForSeedV4(serverSeed);
  if (!secureEqualHex(expectedServerCommitment, serverCommitment)) {
    throw new Error('server seed commitment verification failed');
  }

  const expectedClientCommitment =
    clientCommitmentForSeedV4(clientSeed);
  if (!secureEqualHex(expectedClientCommitment, clientCommitment)) {
    throw new Error('client seed commitment verification failed');
  }

  const normalizedConfig = normalizeV4Config(config);
  const expectedConfigDigest = v4ConfigDigest(normalizedConfig);
  if (!secureEqualHex(expectedConfigDigest, configDigest)) {
    throw new Error('round configuration digest verification failed');
  }

  const expectedContextDigest =
    v4ContextDigest(normalizedConfig.context);
  if (!secureEqualHex(expectedContextDigest, contextDigest)) {
    throw new Error('round context digest verification failed');
  }

  const expectedEventBindingDigest =
    v4EventBindingDigest(normalizedConfig.context);
  if (
    !secureEqualHex(
      expectedEventBindingDigest,
      eventBindingDigest,
    )
  ) {
    throw new Error('event binding digest verification failed');
  }

  const transcript = {
    algorithm: ALGORITHM_V4,
    roundId,
    serverCommitment: serverCommitment.toLowerCase(),
    clientCommitment: clientCommitment.toLowerCase(),
    configDigest: configDigest.toLowerCase(),
    contextDigest: contextDigest.toLowerCase(),
    eventBindingDigest: eventBindingDigest.toLowerCase(),
  };
  const transcriptDigest = sha256Hex(
    'entronex:v4:transcript:' + jcsCanonicalize(transcript),
  );

  const outcomeKey = deriveV4Key({
    serverSeed,
    clientSeed,
    transcriptDigest,
    label: 'outcome',
  });
  const outcomeRng = new HmacStream(
    outcomeKey,
    'direct-outcome-selection',
  );
  const outcomeIndex = outcomeRng.uniformInt(
    normalizedConfig.outcomes.length,
  );
  const outcome = normalizedConfig.outcomes[outcomeIndex];

  const worldKey = deriveV4Key({
    serverSeed,
    clientSeed,
    transcriptDigest,
    label: 'natural-world',
  });
  const world = sampleNaturalWorld({
    outcomes: normalizedConfig.outcomes,
    outcome,
    key: worldKey,
    ...normalizedConfig.world,
  });

  const core = {
    algorithm: ALGORITHM_V4,
    roundId,
    serverCommitment: serverCommitment.toLowerCase(),
    serverSeed: serverSeed.toLowerCase(),
    clientCommitment: clientCommitment.toLowerCase(),
    clientSeed: clientSeed.toLowerCase(),
    configDigest: configDigest.toLowerCase(),
    contextDigest: contextDigest.toLowerCase(),
    eventBindingDigest: eventBindingDigest.toLowerCase(),
    transcriptDigest,
    config: normalizedConfig,
    outcomeIndex,
    outcome,
    world,
  };

  return {
    ...core,
    proofDigest: digestV4Core(core),
  };
}

export function verifyProofV4(proof) {
  try {
    if (!proof || typeof proof !== 'object') {
      throw new TypeError('proof must be an object');
    }
    if (proof.algorithm !== ALGORITHM_V4) {
      throw new Error('unsupported v4 proof algorithm');
    }
    if (
      typeof proof.proofDigest !== 'string' ||
      !/^[0-9a-f]{64}$/i.test(proof.proofDigest)
    ) {
      throw new Error('invalid proof digest');
    }

    const recomputed = resolveOutcomeV4(proof);
    const suppliedCore = extractV4Core(proof);
    const recomputedCore = extractV4Core(recomputed);
    const coreMatches =
      jcsCanonicalize(suppliedCore) ===
      jcsCanonicalize(recomputedCore);
    const digestMatches =
      secureEqualHex(proof.proofDigest, recomputed.proofDigest) &&
      secureEqualHex(
        proof.proofDigest,
        digestV4Core(suppliedCore),
      );

    return {
      valid: coreMatches && digestMatches,
      recomputed,
    };
  } catch (error) {
    return {
      valid: false,
      error: error instanceof Error ? error.message : String(error),
    };
  }
}

export function sampleNaturalWorld({
  outcomes,
  outcome,
  key,
  cellsPerOutcome = DEFAULT_WORLD.cellsPerOutcome,
  timelineTicks = DEFAULT_WORLD.timelineTicks,
  epochCount = DEFAULT_WORLD.epochCount,
  probeCount = DEFAULT_WORLD.probeCount,
}) {
  const fieldSize = validateFieldComplexity(
    outcomes,
    cellsPerOutcome,
  );
  if (fieldSize > MAX_V4_WORLD_CELLS) {
    throw new RangeError('v4 natural world is too large');
  }
  if (!Buffer.isBuffer(key) || key.length !== 32) {
    throw new TypeError('natural-world key must be 32 bytes');
  }
  boundedInt(
    timelineTicks,
    DEFAULT_WORLD.timelineTicks,
    256,
    16_384,
    'timelineTicks',
  );
  boundedInt(epochCount, DEFAULT_WORLD.epochCount, 2, 128, 'epochCount');
  boundedInt(probeCount, DEFAULT_WORLD.probeCount, 1, 8, 'probeCount');
  if (epochCount > timelineTicks) {
    throw new RangeError('epochCount cannot exceed timelineTicks');
  }
  if (!outcomes.some((value) => Object.is(value, outcome))) {
    throw new TypeError('outcome must belong to outcomes');
  }

  const { width, height } = nearSquareDimensions(fieldSize);
  const timelineRng = new HmacStream(key, 'timeline');
  const sampleTick = timelineRng.uniformInt(timelineTicks);
  const layoutEpoch = Math.min(
    epochCount - 1,
    Math.floor((sampleTick * epochCount) / timelineTicks),
  );

  const profileRng = new HmacStream(key, 'motion-profile');
  const motionProfile =
    MOTION_PROFILES[profileRng.uniformInt(MOTION_PROFILES.length)];

  const probes = [];
  for (let probe = 0; probe < probeCount; probe += 1) {
    probes.push(
      createProbe({
        key,
        probe,
        width,
        height,
        timelineTicks,
        sampleTick,
      }),
    );
  }

  const probeSelector = new HmacStream(key, 'probe-selector');
  const selectedProbe = probeSelector.uniformInt(probeCount);
  const selected = probes[selectedProbe];

  const motion = new HmacStream(
    key,
    'logical-motion:' + layoutEpoch + ':' + sampleTick,
  );
  const driftX = motion.uniformInt(width);
  const driftY = motion.uniformInt(height);
  const mirrorX = motion.uniformInt(2) === 1;
  const mirrorY = motion.uniformInt(2) === 1;

  let sourceX = mod(selected.sampleX - driftX, width);
  let sourceY = mod(selected.sampleY - driftY, height);
  if (mirrorX) sourceX = width - 1 - sourceX;
  if (mirrorY) sourceY = height - 1 - sourceY;

  const rowShift = new HmacStream(
    key,
    'row-shift:' + layoutEpoch + ':' + sourceY,
  ).uniformInt(width);
  sourceX = mod(sourceX - rowShift, width);

  const columnShift = new HmacStream(
    key,
    'column-shift:' + layoutEpoch + ':' + sourceX,
  ).uniformInt(height);
  sourceY = mod(sourceY - columnShift, height);

  const rawSampleIndex = sourceY * width + sourceX;

  const field = buildBalancedField(
    outcomes,
    new HmacStream(
      key,
      'field-layout:epoch:' + layoutEpoch,
    ),
    cellsPerOutcome,
  );

  const witness = alignOutcomeWitness({
    field,
    outcome,
    sampleIndex: rawSampleIndex,
    key,
    layoutEpoch,
  });

  const fieldDigest = sha256Hex(
    'entronex:natural-world:v1:field:' +
      jcsCanonicalize(field),
  );

  const regionRng = new HmacStream(
    key,
    'region-grid:' + layoutEpoch,
  );
  const regionColumns = ranged(
    regionRng,
    Math.min(2, width),
    Math.min(6, width),
  );
  const regionRows = ranged(
    regionRng,
    Math.min(2, height),
    Math.min(6, height),
  );
  const regionStates = [];
  for (
    let region = 0;
    region < regionColumns * regionRows;
    region += 1
  ) {
    const rng = new HmacStream(
      key,
      'region-motion:' +
        layoutEpoch +
        ':' +
        sampleTick +
        ':' +
        region,
    );
    regionStates.push({
      region,
      offsetXMilli: rng.uniformInt(901) - 450,
      offsetYMilli: rng.uniformInt(901) - 450,
      rotationMilliDegrees: rng.uniformInt(24_001) - 12_000,
      scalePermille: 900 + rng.uniformInt(201),
      phasePermille: rng.uniformInt(1_000),
    });
  }

  const jitterSeedDigest = sha256Hex(
    'entronex:natural-world:v1:jitter:' + key.toString('hex'),
  );

  const worldCore = {
    version: NATURAL_WORLD_V1,
    role: 'deterministic-presentation-proof',
    outcomeSelection:
      'independent-hkdf-sha256-rejection-sampling',
    topology: 'rectangular-torus',
    fieldSize,
    width,
    height,
    cellsPerOutcome,
    timelineTicks,
    epochCount,
    layoutEpoch,
    sampleTick,
    motionProfile,
    selectedProbe,
    probes,
    logicalTransform: {
      driftX,
      driftY,
      mirrorX,
      mirrorY,
      rowShift,
      columnShift,
    },
    regions: {
      presentationOnly: true,
      columns: regionColumns,
      rows: regionRows,
      states: regionStates,
    },
    jitter: {
      presentationOnly: true,
      amplitudePermille: 420,
      seedDigest: jitterSeedDigest,
    },
    sampleIndex: rawSampleIndex,
    witness,
    fieldDigest,
    sampledOutcome: field[rawSampleIndex],
  };

  return {
    ...worldCore,
    worldDigest: sha256Hex(
      'entronex:natural-world:v1:manifest:' +
        jcsCanonicalize(worldCore),
    ),
  };
}

function createProbe({
  key,
  probe,
  width,
  height,
  timelineTicks,
  sampleTick,
}) {
  const rng = new HmacStream(key, 'laser-probe:' + probe);
  const startX = rng.uniformInt(width);
  const startY = rng.uniformInt(height);
  let velocityX = rng.uniformInt(7) - 3;
  let velocityY = rng.uniformInt(7) - 3;
  if (velocityX === 0 && velocityY === 0) velocityX = 1;

  const accelerationX = rng.uniformInt(3) - 1;
  const accelerationY = rng.uniformInt(3) - 1;
  const bendTick =
    1 + rng.uniformInt(Math.max(1, timelineTicks - 1));
  const impulseX = rng.uniformInt(7) - 3;
  const impulseY = rng.uniformInt(7) - 3;

  const sampleX = mod(
    kinematicPosition(
      startX,
      velocityX,
      accelerationX,
      impulseX,
      bendTick,
      sampleTick,
    ),
    width,
  );
  const sampleY = mod(
    kinematicPosition(
      startY,
      velocityY,
      accelerationY,
      impulseY,
      bendTick,
      sampleTick,
    ),
    height,
  );

  return {
    probe,
    startX,
    startY,
    velocityX,
    velocityY,
    accelerationX,
    accelerationY,
    bendTick,
    impulseX,
    impulseY,
    sampleX,
    sampleY,
  };
}

function kinematicPosition(
  start,
  velocity,
  acceleration,
  impulse,
  bendTick,
  tick,
) {
  const accelerated =
    start +
    velocity * tick +
    (acceleration * tick * (tick - 1)) / 2;
  const afterBend = Math.max(0, tick - bendTick);
  return accelerated + impulse * afterBend;
}

function alignOutcomeWitness({
  field,
  outcome,
  sampleIndex,
  key,
  layoutEpoch,
}) {
  if (Object.is(field[sampleIndex], outcome)) {
    return {
      mode: 'balanced-swap-witness',
      swapped: false,
      sourceIndex: sampleIndex,
      targetIndex: sampleIndex,
    };
  }

  const candidates = [];
  for (let index = 0; index < field.length; index += 1) {
    if (Object.is(field[index], outcome)) candidates.push(index);
  }
  if (candidates.length < 1) {
    throw new Error('natural-world outcome witness is unavailable');
  }

  const rng = new HmacStream(
    key,
    'outcome-witness:' + layoutEpoch + ':' + sampleIndex,
  );
  const sourceIndex = candidates[rng.uniformInt(candidates.length)];
  [field[sourceIndex], field[sampleIndex]] = [
    field[sampleIndex],
    field[sourceIndex],
  ];

  return {
    mode: 'balanced-swap-witness',
    swapped: true,
    sourceIndex,
    targetIndex: sampleIndex,
  };
}

function deriveV4Key({
  serverSeed,
  clientSeed,
  transcriptDigest,
  label,
}) {
  const ikm = Buffer.concat([
    Buffer.from(serverSeed, 'hex'),
    Buffer.from(clientSeed, 'hex'),
  ]);
  return Buffer.from(
    hkdfSync(
      'sha256',
      ikm,
      Buffer.from(transcriptDigest, 'hex'),
      Buffer.from('entronex:v4:' + label, 'utf8'),
      32,
    ),
  );
}

function nearSquareDimensions(size) {
  let height = Math.floor(Math.sqrt(size));
  while (height > 1 && size % height !== 0) height -= 1;
  return {
    width: size / height,
    height,
  };
}

function digestV4Core(core) {
  return sha256Hex(
    'entronex:v4:proof:' + jcsCanonicalize(core),
  );
}

function extractV4Core(proof) {
  return {
    algorithm: proof.algorithm,
    roundId: proof.roundId,
    serverCommitment: proof.serverCommitment,
    serverSeed: proof.serverSeed,
    clientCommitment: proof.clientCommitment,
    clientSeed: proof.clientSeed,
    configDigest: proof.configDigest,
    contextDigest: proof.contextDigest,
    eventBindingDigest: proof.eventBindingDigest,
    transcriptDigest: proof.transcriptDigest,
    config: proof.config,
    outcomeIndex: proof.outcomeIndex,
    outcome: proof.outcome,
    world: proof.world,
  };
}

function optionalDigest(value, name) {
  if (value == null || value === '') return null;
  if (typeof value !== 'string' || !/^[0-9a-f]{64}$/i.test(value)) {
    throw new TypeError(name + ' must be SHA-256 hex or null');
  }
  return value.toLowerCase();
}

function boundedText(value, name, maxLength) {
  if (
    typeof value !== 'string' ||
    value.length < 1 ||
    value.length > maxLength
  ) {
    throw new TypeError(
      name + ' must be a non-empty string up to ' + maxLength + ' characters',
    );
  }
  return value;
}

function boundedNonNegativeInt(value, name) {
  if (!Number.isSafeInteger(value) || value < 0) {
    throw new TypeError(name + ' must be a non-negative safe integer');
  }
  return value;
}

function boundedInt(value, fallback, minimum, maximum, name) {
  const selected = value == null ? fallback : value;
  if (
    !Number.isSafeInteger(selected) ||
    selected < minimum ||
    selected > maximum
  ) {
    throw new RangeError(
      name + ' must be between ' + minimum + ' and ' + maximum,
    );
  }
  return selected;
}

function ranged(rng, minimum, maximum) {
  if (maximum <= minimum) return minimum;
  return minimum + rng.uniformInt(maximum - minimum + 1);
}

function mod(value, divisor) {
  return ((value % divisor) + divisor) % divisor;
}
