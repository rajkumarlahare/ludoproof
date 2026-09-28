const required = [
  "CLOUDFLARE_API_TOKEN",
  "CLOUDFLARE_ACCOUNT_ID",
  "ENTRONEX_API_TOKEN",
  "LUDOPROOF_SESSION_HMAC_KEY",
  "ENTRONEX_SIGNING_KEY_FINGERPRINT",
  "ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64",
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
  String(
    process.env.LUDOPROOF_SESSION_HMAC_KEY,
  ).length < 32
) {
  console.error(
    "LUDOPROOF_SESSION_HMAC_KEY must contain at least 32 characters.",
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

if (
  !/^[0-9a-f]{64}$/i.test(
    String(
      process.env
        .ENTRONEX_SIGNING_KEY_FINGERPRINT,
    ),
  )
) {
  console.error(
    "ENTRONEX_SIGNING_KEY_FINGERPRINT must be a 64-character SHA-256 hex digest.",
  );
  process.exit(2);
}

let pem;
try {
  pem =
    Buffer.from(
      String(
        process.env
          .ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64,
      ),
      "base64",
    ).toString("utf8");
} catch {
  pem = "";
}

if (
  !pem.includes(
    "-----BEGIN PUBLIC KEY-----",
  ) ||
  !pem.includes(
    "-----END PUBLIC KEY-----",
  )
) {
  console.error(
    "ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64 must decode to an SPKI public PEM.",
  );
  process.exit(2);
}

console.log(
  "Cloudflare production deploy preflight passed.",
);
