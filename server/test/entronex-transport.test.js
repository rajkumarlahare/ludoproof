import test from "node:test";
import assert from "node:assert/strict";

import { entronexRequest } from "../src/match-room.js";

const BASE_ENV = Object.freeze({
  ENTRONEX_BASE_URL:
    "https://entronex.example.test",
  ENTRONEX_API_TOKEN:
    "lp_test_entronex_token_1234567890",
});

test(
  "EntroNex transport rejects oversized streaming responses",
  { concurrency: false },
  async (t) => {
    const originalFetch =
      globalThis.fetch;
    globalThis.fetch =
      async () =>
        new Response(
          "x".repeat(
            256 * 1024 + 1,
          ),
          {
            status: 200,
            headers: {
              "content-type":
                "application/json",
            },
          },
        );
    t.after(() => {
      globalThis.fetch =
        originalFetch;
    });

    await assert.rejects(
      () =>
        entronexRequest(
          BASE_ENV,
          "/v4/rounds",
          {
            method: "POST",
            body: {},
          },
        ),
      (error) =>
        error.code ===
        "ENTRONEX_RESPONSE_TOO_LARGE",
    );
  },
);

test(
  "EntroNex transport aborts stalled upstream requests",
  { concurrency: false },
  async (t) => {
    const originalFetch =
      globalThis.fetch;
    globalThis.fetch =
      async (_input, init) =>
        new Promise(
          (_resolve, reject) => {
            const keepAlive =
              setTimeout(
                () => {},
                1_000,
              );
            init.signal.addEventListener(
              "abort",
              () => {
                clearTimeout(
                  keepAlive,
                );
                const error =
                  new Error(
                    "aborted",
                  );
                error.name =
                  "AbortError";
                reject(error);
              },
              { once: true },
            );
          },
        );
    t.after(() => {
      globalThis.fetch =
        originalFetch;
    });

    await assert.rejects(
      () =>
        entronexRequest(
          {
            ...BASE_ENV,
            ENTRONEX_REQUEST_TIMEOUT_MS:
              "100",
          },
          "/v4/rounds",
          {
            method: "POST",
            body: {},
          },
        ),
      (error) =>
        error.code ===
        "ENTRONEX_TIMEOUT",
    );
  },
);
