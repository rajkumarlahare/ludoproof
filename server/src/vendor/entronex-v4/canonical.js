// Vendored from rajkumarlahare/entronex@d219148338b43e961689a7a7ab6f8ce3eb88e8de
// Source: src/core/canonical.js. Keep byte-for-byte logic aligned with the pinned EntroNex v4 candidate.
export function canonicalJson(value) {
  return JSON.stringify(normalize(value));
}

function normalize(value) {
  if (value === null || typeof value === 'string' || typeof value === 'boolean') return value;

  if (typeof value === 'number') {
    if (!Number.isFinite(value)) throw new TypeError('canonical JSON does not support non-finite numbers');
    return Object.is(value, -0) ? 0 : value;
  }

  if (Array.isArray(value)) return value.map(normalize);

  if (value && typeof value === 'object') {
    const prototype = Object.getPrototypeOf(value);
    if (prototype !== Object.prototype && prototype !== null) {
      throw new TypeError('canonical JSON only supports plain objects');
    }

    const output = {};
    for (const key of Object.keys(value).sort()) {
      const child = value[key];
      if (child === undefined || typeof child === 'function' || typeof child === 'symbol' || typeof child === 'bigint') {
        throw new TypeError(`unsupported canonical JSON value at key ${key}`);
      }
      output[key] = normalize(child);
    }
    return output;
  }

  throw new TypeError('unsupported canonical JSON value');
}
