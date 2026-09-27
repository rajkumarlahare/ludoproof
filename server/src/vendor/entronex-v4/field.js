// Vendored from rajkumarlahare/entronex@d219148338b43e961689a7a7ab6f8ce3eb88e8de
// Source: src/core/field.js. Keep byte-for-byte logic aligned with the pinned EntroNex v4 candidate.
import { HmacStream } from './stream.js';

const DEFAULT_CELLS_PER_OUTCOME = 256;
const DEFAULT_LAYER_COUNT = 12;
export const MAX_FIELD_CELLS = 262_144;

export function validateOutcomes(outcomes) {
  if (!Array.isArray(outcomes) || outcomes.length < 2 || outcomes.length > 1024) {
    throw new RangeError('outcomes must contain between 2 and 1024 items');
  }

  const keys = outcomes.map((value) => {
    if (typeof value === 'number') {
      if (!Number.isSafeInteger(value)) {
        throw new TypeError('numeric outcomes must be safe integers');
      }
      return `number:${value}`;
    }

    if (typeof value === 'string') {
      if (value.length < 1 || value.length > 256) {
        throw new TypeError('string outcomes must contain 1 to 256 characters');
      }
      return `string:${value}`;
    }

    throw new TypeError('each outcome must be a string or safe integer');
  });

  if (new Set(keys).size !== keys.length) {
    throw new TypeError('outcomes must be unique');
  }
}

export function validateFieldComplexity(outcomes, cellsPerOutcome) {
  validateOutcomes(outcomes);
  if (!Number.isSafeInteger(cellsPerOutcome) || cellsPerOutcome < 1 || cellsPerOutcome > 4096) {
    throw new RangeError('cellsPerOutcome must be between 1 and 4096');
  }
  const fieldCells = outcomes.length * cellsPerOutcome;
  if (!Number.isSafeInteger(fieldCells) || fieldCells > MAX_FIELD_CELLS) {
    throw new RangeError(`field exceeds maximum of ${MAX_FIELD_CELLS} cells`);
  }
  return fieldCells;
}

export function buildBalancedField(outcomes, rng, cellsPerOutcome = DEFAULT_CELLS_PER_OUTCOME) {
  validateFieldComplexity(outcomes, cellsPerOutcome);
  if (!(rng instanceof HmacStream)) throw new TypeError('rng must be an HmacStream');
  const field = [];
  for (const outcome of outcomes) {
    for (let i = 0; i < cellsPerOutcome; i += 1) field.push(outcome);
  }

  for (let i = field.length - 1; i > 0; i -= 1) {
    const j = rng.uniformInt(i + 1);
    [field[i], field[j]] = [field[j], field[i]];
  }

  return field;
}

export function sampleMovingField({ outcomes, key, cellsPerOutcome = DEFAULT_CELLS_PER_OUTCOME, layerCount = DEFAULT_LAYER_COUNT }) {
  validateFieldComplexity(outcomes, cellsPerOutcome);
  if (!Number.isSafeInteger(layerCount) || layerCount < 1 || layerCount > 64) {
    throw new RangeError('layerCount must be between 1 and 64');
  }

  const fieldRng = new HmacStream(key, 'field-layout');
  const field = buildBalancedField(outcomes, fieldRng, cellsPerOutcome);
  const fieldSize = field.length;
  const laserIndex = Math.floor(fieldSize / 2);
  let worldOffset = 0;
  const layers = [];

  for (let layer = 0; layer < layerCount; layer += 1) {
    const rng = new HmacStream(key, `motion-layer:${layer}`);
    const shift = rng.uniformInt(fieldSize);
    const direction = rng.uniformInt(2) === 0 ? -1 : 1;
    worldOffset = mod(worldOffset + direction * shift, fieldSize);
    layers.push({ layer, direction, shift });
  }

  const sampleRng = new HmacStream(key, 'sample-instant');
  const samplingTick = 1 + sampleRng.uniformInt(1_000_000_000);
  const samplingDrift = sampleRng.uniformInt(fieldSize);
  worldOffset = mod(worldOffset + samplingDrift, fieldSize);

  const sampleIndex = mod(laserIndex - worldOffset, fieldSize);
  return {
    outcome: field[sampleIndex],
    fieldSize,
    cellsPerOutcome,
    layerCount,
    laserIndex,
    sampleIndex,
    samplingTick,
    samplingDrift,
    layers,
  };
}

function mod(value, divisor) {
  return ((value % divisor) + divisor) % divisor;
}
