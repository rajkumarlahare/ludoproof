import {
  deterministicMatchId,
  httpError,
  issueLeaderboardProfileAssertion,
  normalizeDisplayName,
  requireClientRequestId,
} from "./crypto.js";

const STATE_KEY =
  "matchmaking-state";
const QUEUE_TTL_MS =
  2 * 60 * 1000;
const ASSIGNMENT_TTL_MS =
  10 * 60 * 1000;
const PROFILE_ASSERTION_TTL_MS =
  15 * 60 * 1000;
const ALLOWED_PLAYER_COUNTS =
  new Set([2, 4]);

export class MatchmakerQueue {
  constructor(ctx, env) {
    this.ctx = ctx;
    this.env = env;
    this.mutationTail =
      Promise.resolve();
  }

  async fetch(request) {
    try {
      const url =
        new URL(
          request.url,
        );
      const body =
        request.method ===
        "POST"
          ? await readJson(
              request,
            )
          : null;

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/search"
      ) {
        return await this.#mutate(
          () =>
            this.#search(
              request,
              body,
            ),
        );
      }

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/status"
      ) {
        return await this.#mutate(
          () =>
            this.#status(
              request,
              body,
            ),
        );
      }

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/cancel"
      ) {
        return await this.#mutate(
          () =>
            this.#cancel(
              request,
              body,
            ),
        );
      }

      return json(
        404,
        {
          error:
            "NOT_FOUND",
          message:
            "route not found",
        },
      );
    } catch (error) {
      return errorResponse(
        error,
      );
    }
  }

  async alarm() {
    const state =
      await this.#load();
    const cleaned =
      this.#cleanup(
        state,
        Date.now(),
      );
    await this.#persist(
      cleaned,
    );
  }

  #mutate(work) {
    const run =
      this.mutationTail.then(
        work,
        work,
      );
    this.mutationTail =
      run.catch(
        () => {},
      );
    return run;
  }

  async #search(
    request,
    body,
  ) {
    const profile =
      await this.#profileIdentity(
        request,
      );
    const participant =
      normalizeParticipant(
        body,
        profile.profileId,
      );
    const now =
      Date.now();
    let state =
      this.#cleanup(
        await this.#load(),
        now,
      );

    const assigned =
      state.assignments[
        participant
          .clientRequestId
      ];
    if (assigned) {
      return await this.#matchedResponse(
        participant,
        assigned,
      );
    }

    const existingIndex =
      state.queue
        .findIndex(
          (candidate) =>
            candidate
              .clientRequestId ===
            participant
              .clientRequestId,
        );
    if (
      existingIndex >=
      0
    ) {
      if (
        state.queue[
          existingIndex
        ].profileId !==
        participant.profileId
      ) {
        throw httpError(
          403,
          "MATCHMAKING_PROFILE_MISMATCH",
          "matchmaking request belongs to another authenticated profile",
        );
      }

      state.queue[
        existingIndex
      ] = {
        ...state.queue[
          existingIndex
        ],
        displayName:
          participant
            .displayName,
        profileId:
          participant
            .profileId,
        lastSeenAt:
          now,
      };
      await this.#persist(
        state,
      );
      return json(
        200,
        searchingPayload(
          state,
          participant
            .playerCount,
          now,
        ),
      );
    }

    if (
      participant
        .profileId !=
        null &&
      state.queue.some(
        (candidate) =>
          candidate
            .profileId ===
          participant
            .profileId,
      )
    ) {
      throw httpError(
        409,
        "PROFILE_ALREADY_QUEUED",
        "this profile is already searching",
      );
    }

    state.queue.push({
      ...participant,
      joinedAt:
        now,
      lastSeenAt:
        now,
    });

    const eligible =
      state.queue
        .filter(
          (candidate) =>
            candidate
              .playerCount ===
            participant
              .playerCount,
        )
        .sort(
          (left, right) =>
            left.joinedAt -
            right.joinedAt,
        );

    if (
      eligible.length >=
      participant
        .playerCount
    ) {
      const group =
        eligible.slice(
          0,
          participant
            .playerCount,
        );

      // Persist the full candidate set before crossing into MatchRoom.
      // If this object restarts during create/join/start, the same
      // deterministic request IDs can replay the operation safely.
      await this.#persist(
        state,
      );

      const assignment =
        await this.#materialize(
          group,
          participant
            .playerCount,
        );

      const requestIds =
        new Set(
          group.map(
            (candidate) =>
              candidate
                .clientRequestId,
          ),
        );

      state.queue =
        state.queue.filter(
          (candidate) =>
            !requestIds.has(
              candidate
                .clientRequestId,
            ),
        );

      group.forEach(
        (
          candidate,
          index,
        ) => {
          state.assignments[
            candidate
              .clientRequestId
          ] = {
            matchId:
              assignment
                .matchId,
            targetPlayerCount:
              participant
                .playerCount,
            role:
              index ===
              0
                ? "HOST"
                : "GUEST",
            assignedAt:
              now,
            expiresAt:
              now +
              ASSIGNMENT_TTL_MS,
            displayName:
              candidate
                .displayName,
            profileId:
              candidate
                .profileId,
          };
        },
      );

      await this.#persist(
        state,
      );

      const ownAssignment =
        state.assignments[
          participant
            .clientRequestId
        ];
      return await this.#matchedResponse(
        participant,
        ownAssignment,
      );
    }

    await this.#persist(
      state,
    );
    return json(
      202,
      searchingPayload(
        state,
        participant
          .playerCount,
        now,
      ),
    );
  }

  async #status(
    request,
    body,
  ) {
    const profile =
      await this.#profileIdentity(
        request,
      );
    const clientRequestId =
      requireClientRequestId(
        body
          ?.clientRequestId,
      );
    const playerCount =
      requirePlayerCount(
        body
          ?.playerCount,
      );
    const now =
      Date.now();
    const state =
      this.#cleanup(
        await this.#load(),
        now,
      );

    const assignment =
      state.assignments[
        clientRequestId
      ];
    if (assignment) {
      if (
        assignment.profileId !==
        profile.profileId
      ) {
        throw httpError(
          403,
          "MATCHMAKING_PROFILE_MISMATCH",
          "matchmaking request belongs to another authenticated profile",
        );
      }

      const participant = {
        clientRequestId,
        playerCount,
        displayName:
          assignment
            .displayName,
        profileId:
          assignment
            .profileId,
      };
      await this.#persist(
        state,
      );
      return await this.#matchedResponse(
        participant,
        assignment,
      );
    }

    const queued =
      state.queue.find(
        (candidate) =>
          candidate
            .clientRequestId ===
          clientRequestId,
      );
    if (!queued) {
      await this.#persist(
        state,
      );
      return json(
        200,
        {
          status:
            "IDLE",
          playerCount,
          queuedPlayers:
            0,
          targetPlayerCount:
            playerCount,
        },
      );
    }

    if (
      queued.profileId !==
      profile.profileId
    ) {
      throw httpError(
        403,
        "MATCHMAKING_PROFILE_MISMATCH",
        "matchmaking request belongs to another authenticated profile",
      );
    }

    queued.lastSeenAt =
      now;
    await this.#persist(
      state,
    );
    return json(
      200,
      searchingPayload(
        state,
        playerCount,
        now,
      ),
    );
  }

  async #cancel(
    request,
    body,
  ) {
    const profile =
      await this.#profileIdentity(
        request,
      );
    const clientRequestId =
      requireClientRequestId(
        body
          ?.clientRequestId,
      );
    const playerCount =
      requirePlayerCount(
        body
          ?.playerCount,
      );
    const now =
      Date.now();
    let state =
      this.#cleanup(
        await this.#load(),
        now,
      );

    const assignment =
      state.assignments[
        clientRequestId
      ];
    if (assignment) {
      if (
        assignment.profileId !==
        profile.profileId
      ) {
        throw httpError(
          403,
          "MATCHMAKING_PROFILE_MISMATCH",
          "matchmaking request belongs to another authenticated profile",
        );
      }
      await this.#persist(
        state,
      );
      return json(
        200,
        {
          status:
            "MATCHED",
          cancelled:
            false,
          playerCount,
          matchId:
            assignment
              .matchId,
        },
      );
    }

    const queued =
      state.queue.find(
        (candidate) =>
          candidate
            .clientRequestId ===
          clientRequestId,
      );
    if (
      queued &&
      queued.profileId !==
        profile.profileId
    ) {
      throw httpError(
        403,
        "MATCHMAKING_PROFILE_MISMATCH",
        "matchmaking request belongs to another authenticated profile",
      );
    }

    const before =
      state.queue.length;
    state.queue =
      state.queue.filter(
        (candidate) =>
          candidate
            .clientRequestId !==
          clientRequestId,
      );
    await this.#persist(
      state,
    );

    return json(
      200,
      {
        status:
          "CANCELLED",
        cancelled:
          state.queue
            .length <
          before,
        playerCount,
      },
    );
  }

  async #materialize(
    participants,
    targetPlayerCount,
  ) {
    const host =
      participants[0];

    for (
      let attempt = 0;
      attempt < 4;
      attempt += 1
    ) {
      const matchId =
        await deterministicMatchId(
          host
            .clientRequestId,
          attempt,
        );
      const target =
        room(
          this.env,
          matchId,
        );

      const createResponse =
        await target.fetch(
          new Request(
            "https://room/create",
            {
              method:
                "POST",
              headers: {
                "content-type":
                  "application/json",
                "x-ludoproof-profile-assertion":
                  await issueLeaderboardProfileAssertion(
                    this.env,
                    {
                      matchId,
                      profileId:
                        host.profileId,
                      clientRequestId:
                        host.clientRequestId,
                      expiresAt:
                        Date.now() +
                        PROFILE_ASSERTION_TTL_MS,
                    },
                  ),
              },
              body:
                JSON.stringify({
                  matchId,
                  displayName:
                    host
                      .displayName,
                  clientRequestId:
                    host
                      .clientRequestId,
                  matchmaking:
                    true,
                  targetPlayerCount,
                }),
            },
          ),
        );

      if (
        createResponse
          .status ===
        409
      ) {
        continue;
      }

      const created =
        await requireJsonResponse(
          createResponse,
        );

      for (
        let index = 1;
        index <
        participants.length;
        index += 1
      ) {
        const participant =
          participants[
            index
          ];
        const joinResponse =
          await target.fetch(
            new Request(
              "https://room/join",
              {
                method:
                  "POST",
                headers: {
                  "content-type":
                    "application/json",
                  "x-ludoproof-profile-assertion":
                    await issueLeaderboardProfileAssertion(
                      this.env,
                      {
                        matchId,
                        profileId:
                          participant.profileId,
                        clientRequestId:
                          participant.clientRequestId,
                        expiresAt:
                          Date.now() +
                          PROFILE_ASSERTION_TTL_MS,
                      },
                    ),
                },
                body:
                  JSON.stringify({
                    displayName:
                      participant
                        .displayName,
                    clientRequestId:
                      participant
                        .clientRequestId,
                  }),
              },
            ),
          );
        await requireJsonResponse(
          joinResponse,
        );
      }

      const startResponse =
        await target.fetch(
          new Request(
            "https://room/start",
            {
              method:
                "POST",
              headers: {
                "content-type":
                  "application/json",
                authorization:
                  "Bearer " +
                  created
                    .playerToken,
              },
              body:
                JSON.stringify(
                  {},
                ),
            },
          ),
        );
      await requireJsonResponse(
        startResponse,
      );

      return {
        matchId,
      };
    }

    throw httpError(
      503,
      "MATCH_ID_EXHAUSTED",
      "could not allocate a public match",
    );
  }

  async #matchedResponse(
    participant,
    assignment,
  ) {
    if (
      assignment.profileId !==
      participant.profileId
    ) {
      throw httpError(
        403,
        "MATCHMAKING_PROFILE_MISMATCH",
        "matchmaking request belongs to another authenticated profile",
      );
    }

    if (
      assignment
        .targetPlayerCount !==
      participant
        .playerCount
    ) {
      throw httpError(
        409,
        "MATCHMAKING_COUNT_MISMATCH",
        "assigned match uses a different player count",
      );
    }

    const target =
      room(
        this.env,
        assignment
          .matchId,
      );

    const path =
      assignment.role ===
      "HOST"
        ? "/create"
        : "/join";

    const body = {
      displayName:
        participant
          .displayName,
      clientRequestId:
        participant
          .clientRequestId,
    };

    if (
      assignment.role ===
      "HOST"
    ) {
      body.matchId =
        assignment
          .matchId;
      body.matchmaking =
        true;
      body.targetPlayerCount =
        assignment
          .targetPlayerCount;
    }

    const response =
      await target.fetch(
        new Request(
          "https://room" +
            path,
          {
            method:
              "POST",
            headers: {
              "content-type":
                "application/json",
              "x-ludoproof-profile-assertion":
                await issueLeaderboardProfileAssertion(
                  this.env,
                  {
                    matchId:
                      assignment.matchId,
                    profileId:
                      participant.profileId,
                    clientRequestId:
                      participant.clientRequestId,
                    expiresAt:
                      Date.now() +
                      PROFILE_ASSERTION_TTL_MS,
                  },
                ),
            },
            body:
              JSON.stringify(
                body,
              ),
          },
        ),
      );
    const session =
      await requireJsonResponse(
        response,
      );

    return json(
      200,
      {
        status:
          "MATCHED",
        targetPlayerCount:
          assignment
            .targetPlayerCount,
        matchId:
          assignment
            .matchId,
        playerId:
          session
            .playerId,
        playerToken:
          session
            .playerToken,
        state:
          session
            .state,
      },
    );
  }

  async #profileIdentity(
    request,
  ) {
    const authorization =
      request.headers.get(
        "authorization",
      );
    if (
      typeof authorization !==
        "string" ||
      !/^Bearer\s+lpp_[A-Za-z0-9_-]{32,}$/i
        .test(
          authorization,
        )
    ) {
      throw httpError(
        401,
        "PROFILE_AUTH_REQUIRED",
        "authenticated leaderboard profile is required for public matchmaking",
      );
    }
    if (
      !this.env
        .LUDOPROOF_LEADERBOARD
    ) {
      throw httpError(
        503,
        "LEADERBOARD_NOT_CONFIGURED",
        "leaderboard storage is not configured",
      );
    }

    const id =
      this.env
        .LUDOPROOF_LEADERBOARD
        .idFromName(
          "global",
        );
    const target =
      this.env
        .LUDOPROOF_LEADERBOARD
        .get(id);
    const response =
      await target.fetch(
        new Request(
          "https://leaderboard/profile/identity",
          {
            method:
              "GET",
            headers: {
              authorization,
            },
          },
        ),
      );

    let value;
    try {
      value =
        await response.json();
    } catch {
      throw httpError(
        503,
        "PROFILE_AUTH_BAD_RESPONSE",
        "leaderboard profile authorization returned invalid data",
      );
    }

    if (
      !response.ok ||
      value?.ok !== true ||
      typeof value?.profileId !==
        "string" ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
        .test(
          value.profileId,
        )
    ) {
      throw httpError(
        response.status ===
          401
          ? 401
          : 403,
        value?.error ??
          "PROFILE_AUTH_INVALID",
        value?.message ??
          "leaderboard profile credential is invalid",
      );
    }

    return {
      profileId:
        value.profileId,
    };
  }

  async #load() {
    const stored =
      await this.ctx.storage
        .get(
          STATE_KEY,
        );
    return stored ?? {
      schemaVersion:
        1,
      queue: [],
      assignments: {},
    };
  }

  #cleanup(
    state,
    now,
  ) {
    const next =
      structuredClone(
        state,
      );
    next.queue =
      (
        next.queue ??
        []
      ).filter(
        (participant) =>
          Number(
            participant
              .lastSeenAt ??
              participant
                .joinedAt ??
              0,
          ) +
            QUEUE_TTL_MS >
          now,
      );

    const assignments =
      next.assignments ??
      {};
    for (
      const [
        requestId,
        assignment,
      ] of Object.entries(
        assignments,
      )
    ) {
      if (
        Number(
          assignment
            .expiresAt ??
            0,
        ) <=
        now
      ) {
        delete assignments[
          requestId
        ];
      }
    }
    next.assignments =
      assignments;
    return next;
  }

  async #persist(
    state,
  ) {
    await this.ctx.storage.put(
      STATE_KEY,
      state,
    );

    if (
      typeof this.ctx
        .storage
        .setAlarm !==
      "function"
    ) {
      return;
    }

    const deadlines = [
      ...state.queue.map(
        (participant) =>
          Number(
            participant
              .lastSeenAt ??
              participant
                .joinedAt ??
              Date.now(),
          ) +
          QUEUE_TTL_MS,
      ),
      ...Object.values(
        state.assignments,
      ).map(
        (assignment) =>
          Number(
            assignment
              .expiresAt ??
              Date.now() +
                ASSIGNMENT_TTL_MS,
          ),
      ),
    ].filter(
      Number.isFinite,
    );

    if (
      deadlines.length >
      0
    ) {
      await this.ctx.storage
        .setAlarm(
          Math.min(
            ...deadlines,
          ),
        );
    }
  }
}

function normalizeParticipant(
  body,
  profileId,
) {
  return {
    clientRequestId:
      requireClientRequestId(
        body
          ?.clientRequestId,
      ),
    displayName:
      normalizeDisplayName(
        body
          ?.displayName,
      ),
    profileId,
    playerCount:
      requirePlayerCount(
        body
          ?.playerCount,
      ),
  };
}

function requirePlayerCount(
  value,
) {
  const count =
    Number(value);
  if (
    !Number.isInteger(
      count,
    ) ||
    !ALLOWED_PLAYER_COUNTS
      .has(
        count,
      )
  ) {
    throw httpError(
      400,
      "INVALID_PLAYER_COUNT",
      "public matchmaking supports 2 or 4 players",
    );
  }
  return count;
}

function searchingPayload(
  state,
  playerCount,
  now,
) {
  const queued =
    state.queue
      .filter(
        (participant) =>
          participant
            .playerCount ===
          playerCount,
      )
      .sort(
        (left, right) =>
          left.joinedAt -
          right.joinedAt,
      );

  return {
    status:
      "SEARCHING",
    playerCount,
    queuedPlayers:
      Math.min(
        queued.length,
        playerCount,
      ),
    targetPlayerCount:
      playerCount,
    searchStartedAt:
      queued[0]
        ?.joinedAt ??
      now,
  };
}

function room(
  env,
  matchId,
) {
  if (
    !env
      .LUDOPROOF_MATCHES
  ) {
    throw httpError(
      503,
      "MATCH_STORE_NOT_CONFIGURED",
      "match storage is not configured",
    );
  }
  const id =
    env
      .LUDOPROOF_MATCHES
      .idFromName(
        matchId,
      );
  return env
    .LUDOPROOF_MATCHES
    .get(
      id,
    );
}

async function requireJsonResponse(
  response,
) {
  let body;
  try {
    body =
      await response.json();
  } catch {
    throw httpError(
      503,
      "MATCHMAKER_BAD_RESPONSE",
      "match storage returned invalid data",
    );
  }

  if (
    !response.ok
  ) {
    throw httpError(
      response.status,
      body
        ?.error ??
        "MATCHMAKER_MATCH_ERROR",
      body
        ?.message ??
        "match storage rejected matchmaking",
    );
  }
  return body;
}

async function readJson(
  request,
) {
  const type =
    request.headers.get(
      "content-type",
    ) ?? "";
  if (
    !type
      .toLowerCase()
      .startsWith(
        "application/json",
      )
  ) {
    throw httpError(
      415,
      "UNSUPPORTED_MEDIA_TYPE",
      "content-type must be application/json",
    );
  }

  const text =
    await request.text();
  if (
    new TextEncoder()
      .encode(
        text,
      )
      .byteLength >
    8 * 1024
  ) {
    throw httpError(
      413,
      "REQUEST_TOO_LARGE",
      "request body is too large",
    );
  }

  try {
    const value =
      text.length ===
      0
        ? {}
        : JSON.parse(
            text,
          );
    if (
      !value ||
      typeof value !==
        "object" ||
      Array.isArray(
        value,
      )
    ) {
      throw new Error(
        "not object",
      );
    }
    return value;
  } catch {
    throw httpError(
      400,
      "INVALID_JSON",
      "request body must be valid JSON",
    );
  }
}

function json(
  status,
  body,
) {
  return new Response(
    JSON.stringify(
      body,
    ),
    {
      status,
      headers: {
        "content-type":
          "application/json; charset=utf-8",
        "cache-control":
          "no-store",
      },
    },
  );
}

function errorResponse(
  error,
) {
  const status =
    Number.isInteger(
      error
        ?.status,
    )
      ? error.status
      : 500;
  return json(
    status,
    {
      error:
        error
          ?.code ??
        "INTERNAL_ERROR",
      message:
        status ===
          500 &&
        !error
          ?.code
          ? "internal server error"
          : String(
              error
                ?.message ??
                "internal server error",
            ),
    },
  );
}
