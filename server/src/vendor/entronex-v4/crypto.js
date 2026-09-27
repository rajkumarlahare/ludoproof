// Vendored from rajkumarlahare/entronex@d219148338b43e961689a7a7ab6f8ce3eb88e8de
// Source: src/core/crypto.js. Keep byte-for-byte logic aligned with the pinned EntroNex v4 candidate.
import { createHash, createHmac, randomBytes, randomUUID, timingSafeEqual } from 'node:crypto';

const HEX_32_BYTES = /^[0-9a-f]{64}$/i;

export function createServerSeed() {
  return randomBytes(32).toString('hex');
}

export function createRoundId() {
  return randomUUID();
}

export function sha256Hex(value) {
  return createHash('sha256').update(String(value), 'utf8').digest('hex');
}

export function commitmentForSeed(seedHex) {
  assertSeed(seedHex);
  return sha256Hex(`entronex:v1:commit:${seedHex.toLowerCase()}`);
}

export function deriveKey(seedHex, ...parts) {
  assertSeed(seedHex);
  const hmac = createHmac('sha256', Buffer.from(seedHex, 'hex'));
  hmac.update('entronex:v1:derive\0', 'utf8');
  for (const part of parts) {
    hmac.update(String(part), 'utf8');
    hmac.update('\0', 'utf8');
  }
  return hmac.digest();
}

export function secureEqualHex(leftHex, rightHex) {
  if (typeof leftHex !== 'string' || typeof rightHex !== 'string') return false;
  if (leftHex.length !== rightHex.length || leftHex.length % 2 !== 0) return false;
  try {
    return timingSafeEqual(Buffer.from(leftHex, 'hex'), Buffer.from(rightHex, 'hex'));
  } catch {
    return false;
  }
}

export function assertSeed(seedHex) {
  if (!HEX_32_BYTES.test(seedHex ?? '')) {
    throw new TypeError('serverSeed must be a 32-byte hex string');
  }
}
