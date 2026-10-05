import worker from "./index-v1-core.js";
import { RULESET } from "./game.js";

export {
  ApiGate,
  FriendDirectory,
  LeaderboardRoom,
  MatchmakerQueue,
  MatchRoom,
} from "./index-v1-core.js";

export default {
  async fetch(request, env) {
    const response = await worker.fetch(request, env);
    const url = new URL(request.url);
    const advertisesRuleset =
      request.method === "GET" &&
      (url.pathname === "/" || url.pathname === "/ready");

    if (!advertisesRuleset) {
      return response;
    }

    const contentType = response.headers.get("content-type") ?? "";
    if (!contentType.includes("application/json")) {
      return response;
    }

    let body;
    try {
      body = await response.clone().json();
    } catch {
      return response;
    }

    if (body == null || typeof body !== "object" || !("ruleset" in body)) {
      return response;
    }

    const headers = new Headers(response.headers);
    return new Response(
      JSON.stringify({
        ...body,
        ruleset: RULESET.id,
      }),
      {
        status: response.status,
        statusText: response.statusText,
        headers,
      },
    );
  },
};
