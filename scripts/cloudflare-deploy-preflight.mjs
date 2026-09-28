import fs from "node:fs";
import {
  createHash,
  createPublicKey,
} from "node:crypto";

const required = [
  "CLOUDFLARE_API_TOKEN",
  "CLOUDFLARE_ACCOUNT_ID",
  "ENTRONEX_API_TOKEN",
];

const missing =
  required.filter(
    (name) =>
      !String(
        process.env[name] ?? "",
      ).trim(),
  );

if (missing.length > 0) {
  console.error(
    "Cloudflare production deploy is missing required configuration: " +
      missing.join(", "),
  );
  process.exit(2);
}

if (
  !/^[0-9a-f]{32}$/i.test(
    String(
      process.env.CLOUDFLARE_ACCOUNT_ID,
    ),
  )
) {
  console.error(
    "CLOUDFLARE_ACCOUNT_ID must be a 32-character Cloudflare account ID.",
  );
  process.exit(2);
}

if (
  String(
    process.env.ENTRONEX_API_TOKEN,
  ).length < 20
) {
  console.error(
    "ENTRONEX_API_TOKEN is too short to be a valid server credential.",
  );
  process.exit(2);
}

const optionalHmac =
  String(
    process.env
      .LUDOPROOF_SESSION_HMAC_KEY ??
      "",
  );
if (
  optionalHmac.length > 0 &&
  optionalHmac.length < 32
) {
  console.error(
    "LUDOPROOF_SESSION_HMAC_KEY must contain at least 32 characters when supplied.",
  );
  process.exit(2);
}

const wrangler =
  JSON.parse(
    fs.readFileSync(
      "server/wrangler.json",
      "utf8",
    ),
  );
const vars =
  wrangler?.vars ?? {};
const fingerprint =
  String(
    vars.ENTRONEX_SIGNING_KEY_FINGERPRINT ??
      "",
  ).toLowerCase();
const publicKeyPemB64 =
  String(
    vars.ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64 ??
      "",
  );

if (
  vars.ENTRONEX_SIGNING_KEY_ID !==
    "cf-v4-eval-sign-1" ||
  vars.ENTRONEX_TENANT_ID !==
    "cloudflare_v4_eval" ||
  !/^[0-9a-f]{64}$/.test(
    fingerprint,
  ) ||
  !publicKeyPemB64
) {
  console.error(
    "Pinned EntroNex public trust material is incomplete or unexpected.",
  );
  process.exit(2);
}

let publicKey;
try {
  const pem =
    Buffer.from(
      publicKeyPemB64,
      "base64",
    ).toString("utf8");
  publicKey =
    createPublicKey(
      pem,
    );
} catch {
  console.error(
    "Pinned EntroNex public key is not a valid SPKI PEM.",
  );
  process.exit(2);
}

const recomputed =
  createHash("sha256")
    .update(
      publicKey.export({
        type: "spki",
        format: "der",
      }),
    )
    .digest("hex");

if (recomputed !== fingerprint) {
  console.error(
    "Pinned EntroNex public key does not match its configured fingerprint.",
  );
  process.exit(2);
}

console.log(
  "Cloudflare production deploy preflight passed.",
);
