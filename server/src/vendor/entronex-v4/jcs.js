// Vendored from rajkumarlahare/entronex@d219148338b43e961689a7a7ab6f8ce3eb88e8de
// Source: src/core/jcs.js. Keep byte-for-byte logic aligned with the pinned EntroNex v4 candidate.
export function jcsCanonicalize(value) {
  return serialize(value);
}

function serialize(value) {
  if (value === null) return 'null';

  if (typeof value === 'string') {
    assertValidUnicode(value, 'string');
    return JSON.stringify(value);
  }

  if (typeof value === 'boolean') return value ? 'true' : 'false';

  if (typeof value === 'number') {
    if (!Number.isSafeInteger(value)) {
      throw new TypeError('EntroNex JCS profile accepts safe integers only');
    }
    return Object.is(value, -0) ? '0' : String(value);
  }

  if (Array.isArray(value)) {
    return '[' + value.map(serialize).join(',') + ']';
  }

  if (value && typeof value === 'object') {
    const prototype = Object.getPrototypeOf(value);
    if (prototype !== Object.prototype && prototype !== null) {
      throw new TypeError('EntroNex JCS profile accepts plain objects only');
    }

    const keys = Object.keys(value).sort();
    const parts = [];
    for (const key of keys) {
      assertValidUnicode(key, 'object key');
      const child = value[key];
      if (
        child === undefined ||
        typeof child === 'function' ||
        typeof child === 'symbol' ||
        typeof child === 'bigint'
      ) {
        throw new TypeError('unsupported EntroNex JCS value at key ' + key);
      }
      parts.push(JSON.stringify(key) + ':' + serialize(child));
    }
    return '{' + parts.join(',') + '}';
  }

  throw new TypeError('unsupported EntroNex JCS value');
}

function assertValidUnicode(value, name) {
  for (let index = 0; index < value.length; index += 1) {
    const code = value.charCodeAt(index);
    if (code >= 0xd800 && code <= 0xdbff) {
      const next = value.charCodeAt(index + 1);
      if (!(next >= 0xdc00 && next <= 0xdfff)) {
        throw new TypeError(name + ' contains an unpaired high surrogate');
      }
      index += 1;
      continue;
    }
    if (code >= 0xdc00 && code <= 0xdfff) {
      throw new TypeError(name + ' contains an unpaired low surrogate');
    }
  }
}
