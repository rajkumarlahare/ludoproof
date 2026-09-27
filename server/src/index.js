import {
  httpError,
  randomMatchId,
} from "./crypto.js";

export { MatchRoom } from "./match-room.js";

export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);

      if (request.method === "GET" && url.pathname === "/") {
        return json(200, {
          ok: true,
          service: "ludoproof-game-api",
          production: false,
          game: "LudoProof",
          ruleset: "ludoproof-standard-v1",
          entronex: "v4-evaluation",
        });
      }

      if (request.method === "GET" && url.pathname === "/health") {
        return json(200, {
          ok: true,
          service: "ludoproof-game-api",
          entronexConfigured:
            typeof env.ENTRONEX_API_TOKEN === "string" &&
            env.ENTRONEX_API_TOKEN.length >= 20,
        });
      }

      if (request.method === "POST" && url.pathname === "/api/matches") {
        const body = await request.json();
        for (let attempt = 0; attempt < 4; attempt += 1) {
          const matchId = randomMatchId();
          const target = room(env, matchId);
          const response = await target.fetch(
            new Request("https://room/create", {
              method: "POST",
              headers: {
                "content-type": "application/json",
              },
              body: JSON.stringify({
                ...body,
                matchId,
              }),
            }),
          );
          if (response.status !== 409) return response;
        }
        throw httpError(503, "MATCH_ID_EXHAUSTED", "could not allocate a match ID");
      }

      const matchRoute = url.pathname.match(
        /^\/api\/matches\/(LP[A-Z2-9]{8})(?:\/(.+))?$/,
      );
      if (!matchRoute) {
        return json(404, { error: "NOT_FOUND", message: "route not found" });
      }

      const matchId = matchRoute[1];
      const action = matchRoute[2] ?? "state";
      const routeMap = {
        join: "/join",
        state: "/state",
        start: "/start",
        "roll/commit": "/roll/commit",
        "roll/reveal": "/roll/reveal",
        move: "/move",
      };
      const targetPath = routeMap[action];
      if (!targetPath) {
        return json(404, { error: "NOT_FOUND", message: "route not found" });
      }

      const target = room(env, matchId);
      const headers = new Headers();
      const authorization = request.headers.get("authorization");
      if (authorization) headers.set("authorization", authorization);
      headers.set("content-type", "application/json");

      return await target.fetch(
        new Request("https://room" + targetPath, {
          method: request.method,
          headers,
          body:
            request.method === "GET"
              ? undefined
              : await request.text(),
        }),
      );
    } catch (error) {
      return errorResponse(error);
    }
  },
};

function room(env, matchId) {
  const id = env.LUDOPROOF_MATCHES.idFromName(matchId);
  return env.LUDOPROOF_MATCHES.get(id);
}

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
    },
  });
}

function errorResponse(error) {
  const status =
    Number.isInteger(error?.status) &&
    error.status >= 400 &&
    error.status <= 599
      ? error.status
      : 500;

  return json(status, {
    error: error?.code ?? "INTERNAL_ERROR",
    message:
      status === 500 && !error?.code
        ? "internal server error"
        : String(error?.message ?? "internal server error"),
  });
}
