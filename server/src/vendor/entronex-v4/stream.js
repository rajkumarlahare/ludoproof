// Vendored from rajkumarlahare/entronex@d219148338b43e961689a7a7ab6f8ce3eb88e8de
// Source: src/core/stream.js. Keep byte-for-byte logic aligned with the pinned EntroNex v4 candidate.
import { createHmac } from 'node:crypto';

const TWO_POW_48 = 2 ** 48;

export class HmacStream {
  #key;
  #namespace;
  #counter = 0;
  #buffer = Buffer.alloc(0);

  constructor(key, namespace) {
    if (!Buffer.isBuffer(key) || key.length < 16) {
      throw new TypeError('key must be a Buffer of at least 16 bytes');
    }
    this.#key = Buffer.from(key);
    this.#namespace = String(namespace);
  }

  bytes(size) {
    if (!Number.isSafeInteger(size) || size < 0) {
      throw new RangeError('size must be a non-negative safe integer');
    }

    while (this.#buffer.length < size) {
      const block = createHmac('sha256', this.#key)
        .update(`entronex:v1:stream:${this.#namespace}:${this.#counter}`, 'utf8')
        .digest();
      this.#counter += 1;
      this.#buffer = Buffer.concat([this.#buffer, block]);
    }

    const output = this.#buffer.subarray(0, size);
    this.#buffer = this.#buffer.subarray(size);
    return output;
  }

  uniformInt(maxExclusive) {
    if (!Number.isSafeInteger(maxExclusive) || maxExclusive <= 0 || maxExclusive > 0xffffffff) {
      throw new RangeError('maxExclusive must be an integer between 1 and 2^32-1');
    }

    const limit = Math.floor(TWO_POW_48 / maxExclusive) * maxExclusive;
    while (true) {
      const value = this.bytes(6).readUIntBE(0, 6);
      if (value < limit) return value % maxExclusive;
    }
  }
}
