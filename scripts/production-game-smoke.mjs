import {
  createHash,
  randomBytes,
  randomUUID,
} from "node:crypto";

const baseUrl =
  String(
    process.env.LUDOPROOF_WORKER_URL ??
      "https://ludoproof-game-api.ai-8f3.workers.dev",
  ).replace(/\/+$/, "");

const ALGORITHM =
  "entronex-v4-dual-commit-hkdf-sha256-context-bound";

const host =
  await api(
    "POST",
    "/api/matches",
    {
      body: {
        displayName:
          "Production Smoke Host",
        clientRequestId:
          randomUUID(),
      },
    },
  );

const matchId =
  requireText(
    host.matchId,
    "matchId",
  );
const hostPlayer =
  {
    playerId:
      requireText(
        host.playerId,
        "host.playerId",
      ),
    token:
      requireText(
        host.playerToken,
        "host.playerToken",
      ),
  };

const guest =
  await api(
    "POST",
    "/api/matches/" +
      encodeURIComponent(
        matchId,
      ) +
      "/join",
    {
      body: {
        displayName:
          "Production Smoke Guest",
        clientRequestId:
          randomUUID(),
      },
    },
  );

const guestPlayer =
  {
    playerId:
      requireText(
        guest.playerId,
        "guest.playerId",
      ),
    token:
      requireText(
        guest.playerToken,
        "guest.playerToken",
      ),
  };

let started =
  await api(
    "POST",
    "/api/matches/" +
      encodeURIComponent(
        matchId,
      ) +
      "/start",
    {
      token:
        hostPlayer.token,
      body: {},
    },
  );

let state =
  started.state;

if (
  state?.status !==
    "ACTIVE" ||
  !Array.isArray(
    state?.players,
  ) ||
  state.players.length !== 2
) {
  throw new Error(
    "Production smoke match did not enter ACTIVE state with two players.",
  );
}

const players =
  new Map([
    [
      hostPlayer.playerId,
      hostPlayer,
    ],
    [
      guestPlayer.playerId,
      guestPlayer,
    ],
  ]);

let verifiedRolls = 0;
let moveExercised = false;

for (
  let attempt = 0;
  attempt < 12;
  attempt += 1
) {
  const turnPlayer =
    state.players[
      state.turnSeat
    ];
  const identity =
    players.get(
      turnPlayer?.playerId,
    );

  if (!identity) {
    throw new Error(
      "Production smoke could not map the authoritative turn to a player credential.",
    );
  }

  const clientSeed =
    randomBytes(32)
      .toString("hex");
  const clientCommitment =
    createHash("sha256")
      .update(
        "entronex:v4:client-commit:" +
          clientSeed,
        "utf8",
      )
      .digest("hex");

  const committed =
    await api(
      "POST",
      "/api/matches/" +
        encodeURIComponent(
          matchId,
        ) +
        "/roll/commit",
      {
        token:
          identity.token,
        body: {
          clientCommitment,
        },
      },
    );

  const eventIndex =
    committed?.round
      ?.eventIndex;
  if (
    !Number.isSafeInteger(
      eventIndex,
    )
  ) {
    throw new Error(
      "Production smoke commitment did not return a safe eventIndex.",
    );
  }

  const revealed =
    await api(
      "POST",
      "/api/matches/" +
        encodeURIComponent(
          matchId,
        ) +
        "/roll/reveal",
      {
        token:
          identity.token,
        body: {
          clientSeed,
        },
      },
    );

  if (
    !Number.isInteger(
      revealed?.outcome,
    ) ||
    revealed.outcome < 1 ||
    revealed.outcome > 6
  ) {
    throw new Error(
      "Production smoke reveal returned an invalid dice outcome.",
    );
  }

  if (
    revealed?.proof
      ?.algorithm !==
      ALGORITHM ||
    !/^[0-9a-f]{64}$/i.test(
      String(
        revealed?.proofDigest ??
          "",
      ),
    )
  ) {
    throw new Error(
      "Production smoke reveal did not return the pinned EntroNex v4 proof contract.",
    );
  }

  verifiedRolls += 1;
  state =
    revealed.state;

  const legal =
    Array.isArray(
      revealed
        ?.legalTokenIndexes,
    )
      ? revealed
          .legalTokenIndexes
      : [];

  if (
    revealed
      ?.awaitingMove ===
        true &&
    legal.length > 0
  ) {
    const tokenIndex =
      legal[0];

    const moved =
      await api(
        "POST",
        "/api/matches/" +
          encodeURIComponent(
            matchId,
          ) +
          "/move",
        {
          token:
            identity.token,
          body: {
            tokenIndex,
            eventIndex,
          },
        },
      );

    if (
      moved?.state
        ?.pendingRoll !==
      null
    ) {
      throw new Error(
        "Production smoke move did not consume the verified roll.",
      );
    }

    state =
      moved.state;
    moveExercised = true;
    break;
  }

  if (
    state?.status !==
    "ACTIVE"
  ) {
    throw new Error(
      "Production smoke match left ACTIVE state unexpectedly.",
    );
  }
}

if (
  verifiedRolls < 1
) {
  throw new Error(
    "Production smoke did not complete a verified EntroNex roll.",
  );
}

console.log(
  JSON.stringify(
    {
      ok: true,
      matchId,
      players: 2,
      verifiedRolls,
      moveExercised,
      finalRevision:
        state?.revision ?? null,
      ruleset:
        state?.rulesetId ??
        null,
    },
  ),
);

async function api(
  method,
  path,
  {
    token = null,
    body = null,
  } = {},
) {
  const headers = {
    accept:
      "application/json",
    "user-agent":
      "ludoproof-production-smoke/1",
  };

  if (token) {
    headers.authorization =
      "Bearer " + token;
  }

  let payload;
  if (body !== null) {
    headers["content-type"] =
      "application/json";
    payload =
      JSON.stringify(body);
  }

  const response =
    await fetch(
      baseUrl + path,
      {
        method,
        headers,
        body: payload,
        redirect: "error",
        signal:
          AbortSignal.timeout(
            20_000,
          ),
      },
    );

  const text =
    await readLimited(
      response,
      1024 * 1024,
    );

  let value;
  try {
    value =
      text
        ? JSON.parse(text)
        : {};
  } catch {
    throw new Error(
      method +
        " " +
        path +
        " returned non-JSON data.",
    );
  }

  if (!response.ok) {
    throw new Error(
      method +
        " " +
        path +
        " failed with HTTP " +
        response.status +
        " (" +
        String(
          value?.error ??
            "UNKNOWN",
        ) +
        ").",
    );
  }

  return value;
}

async function readLimited(
  response,
  maxBytes,
) {
  if (!response.body) {
    return "";
  }

  const reader =
    response.body
      .getReader();
  const decoder =
    new TextDecoder();
  let total = 0;
  let text = "";

  try {
    for (;;) {
      const {
        done,
        value,
      } =
        await reader.read();
      if (done) break;

      total +=
        value.byteLength;
      if (
        total >
        maxBytes
      ) {
        await reader.cancel();
        throw new Error(
          "Production smoke response exceeded the safety limit.",
        );
      }

      text +=
        decoder.decode(
          value,
          {
            stream: true,
          },
        );
    }

    text +=
      decoder.decode();
    return text;
  } finally {
    reader.releaseLock();
  }
}

function requireText(
  value,
  label,
) {
  if (
    typeof value !==
      "string" ||
    value.length < 1
  ) {
    throw new Error(
      "Production smoke response is missing " +
        label +
        ".",
    );
  }
  return value;
}
