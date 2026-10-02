import {
  LEADERBOARD_FINISH_POINTS,
  LEADERBOARD_WIN_BONUS,
  leaderboardPointsForResult,
  normalizeLeaderboardProfileId,
} from "./leaderboard-core.js";

const MAX_LEADERBOARD_LIMIT = 50;
const DEFAULT_LEADERBOARD_LIMIT = 25;

export class LeaderboardRoom {
  constructor(ctx, env) {
    this.ctx = ctx;
    this.env = env;

    ctx.blockConcurrencyWhile(async () => {
      const sql = ctx.storage.sql;
      sql.exec(`
        CREATE TABLE IF NOT EXISTS leaderboard_entries (
          profile_id TEXT PRIMARY KEY,
          display_name TEXT NOT NULL,
          games INTEGER NOT NULL DEFAULT 0,
          wins INTEGER NOT NULL DEFAULT 0,
          points INTEGER NOT NULL DEFAULT 0,
          updated_at INTEGER NOT NULL
        )
      `);
      sql.exec(`
        CREATE TABLE IF NOT EXISTS leaderboard_matches (
          match_id TEXT PRIMARY KEY,
          recorded_at INTEGER NOT NULL
        )
      `);
      sql.exec(`
        CREATE INDEX IF NOT EXISTS idx_leaderboard_rank
        ON leaderboard_entries (
          points DESC,
          wins DESC,
          games ASC,
          updated_at ASC,
          profile_id ASC
        )
      `);
    });
  }

  async fetch(request) {
    try {
      const url = new URL(request.url);

      if (
        request.method === "POST" &&
        url.pathname === "/record"
      ) {
        return await this.#record(request);
      }

      if (
        request.method === "GET" &&
        url.pathname === "/list"
      ) {
        return this.#list(url);
      }

      return json(404, {
        error: "NOT_FOUND",
        message: "route not found",
      });
    } catch (error) {
      return json(
        Number.isInteger(error?.status)
          ? error.status
          : 500,
        {
          error:
            error?.code ??
            "INTERNAL_ERROR",
          message:
            error?.message ??
            "internal server error",
        },
      );
    }
  }

  async #record(request) {
    const body = await readJson(request);
    const matchId = String(
      body?.matchId ?? "",
    ).trim().toUpperCase();

    if (!/^LP[A-Z2-9]{8}$/.test(matchId)) {
      throw badRequest(
        "INVALID_MATCH_ID",
        "invalid match ID",
      );
    }

    if (
      !Array.isArray(body?.players) ||
      body.players.length < 1 ||
      body.players.length > 4
    ) {
      throw badRequest(
        "INVALID_PLAYERS",
        "players must contain one to four entries",
      );
    }

    const players = [];
    const seen = new Set();

    for (const raw of body.players) {
      const profileId =
        normalizeLeaderboardProfileId(
          raw?.profileId,
        );
      if (!profileId) {
        throw badRequest(
          "INVALID_PROFILE_ID",
          "leaderboard profile ID is invalid",
        );
      }
      if (seen.has(profileId)) {
        throw badRequest(
          "DUPLICATE_PROFILE",
          "a leaderboard profile may only appear once per match",
        );
      }
      seen.add(profileId);

      const displayName =
        normalizeDisplayName(
          raw?.displayName,
        );
      const won =
        raw?.won === true;

      players.push({
        profileId,
        displayName,
        won,
      });
    }

    const now = Date.now();
    let replayed = false;

    this.ctx.storage.transactionSync(() => {
      const inserted =
        this.ctx.storage.sql.exec(
          `
            INSERT OR IGNORE INTO leaderboard_matches (
              match_id,
              recorded_at
            ) VALUES (?, ?)
          `,
          matchId,
          now,
        );

      if (
        Number(inserted.rowsWritten ?? 0) !== 1
      ) {
        replayed = true;
        return;
      }

      for (const player of players) {
        const points =
          leaderboardPointsForResult(
            player.won,
          );
        const wins =
          player.won ? 1 : 0;

        this.ctx.storage.sql.exec(
          `
            INSERT INTO leaderboard_entries (
              profile_id,
              display_name,
              games,
              wins,
              points,
              updated_at
            ) VALUES (?, ?, 1, ?, ?, ?)
            ON CONFLICT(profile_id) DO UPDATE SET
              display_name = excluded.display_name,
              games = leaderboard_entries.games + 1,
              wins = leaderboard_entries.wins + excluded.wins,
              points = leaderboard_entries.points + excluded.points,
              updated_at = excluded.updated_at
          `,
          player.profileId,
          player.displayName,
          wins,
          points,
          now,
        );
      }
    });

    return json(200, {
      ok: true,
      replayed,
      matchId,
    });
  }

  #list(url) {
    const requestedLimit =
      Number(
        url.searchParams.get("limit"),
      );
    const limit =
      Number.isSafeInteger(requestedLimit)
        ? Math.min(
            MAX_LEADERBOARD_LIMIT,
            Math.max(1, requestedLimit),
          )
        : DEFAULT_LEADERBOARD_LIMIT;

    const viewerId =
      normalizeLeaderboardProfileId(
        url.searchParams.get(
          "profileId",
        ),
      );

    const rows = [
      ...this.ctx.storage.sql.exec(
        `
          SELECT
            profile_id,
            display_name,
            games,
            wins,
            points
          FROM leaderboard_entries
          ORDER BY
            points DESC,
            wins DESC,
            games ASC,
            updated_at ASC,
            profile_id ASC
          LIMIT ?
        `,
        limit,
      ),
    ];

    const entries =
      rows.map((row, index) => ({
        rank: index + 1,
        displayName:
          String(row.display_name),
        games:
          Number(row.games),
        wins:
          Number(row.wins),
        points:
          Number(row.points),
        isViewer:
          viewerId != null &&
          String(row.profile_id) ===
            viewerId,
      }));

    let viewer = null;

    if (viewerId) {
      const viewerRows = [
        ...this.ctx.storage.sql.exec(
          `
            SELECT
              ranked.display_name,
              ranked.games,
              ranked.wins,
              ranked.points,
              ranked.rank
            FROM (
              SELECT
                profile_id,
                display_name,
                games,
                wins,
                points,
                ROW_NUMBER() OVER (
                  ORDER BY
                    points DESC,
                    wins DESC,
                    games ASC,
                    updated_at ASC,
                    profile_id ASC
                ) AS rank
              FROM leaderboard_entries
            ) AS ranked
            WHERE ranked.profile_id = ?
            LIMIT 1
          `,
          viewerId,
        ),
      ];

      if (viewerRows.length === 1) {
        const row = viewerRows[0];
        viewer = {
          rank:
            Number(row.rank),
          displayName:
            String(row.display_name),
          games:
            Number(row.games),
          wins:
            Number(row.wins),
          points:
            Number(row.points),
        };
      }
    }

    return json(200, {
      season: "all-time-v1",
      scope: "verified-online-classic",
      scoring: {
        finish:
          LEADERBOARD_FINISH_POINTS,
        winBonus:
          LEADERBOARD_WIN_BONUS,
      },
      entries,
      viewer,
    });
  }
}

function normalizeDisplayName(value) {
  const normalized =
    String(value ?? "")
      .trim()
      .replace(/\s+/g, " ")
      .slice(0, 24);

  if (normalized.length < 2) {
    throw badRequest(
      "INVALID_DISPLAY_NAME",
      "display name must contain 2 to 24 characters",
    );
  }
  return normalized;
}

async function readJson(request) {
  try {
    const body = await request.json();
    if (
      !body ||
      typeof body !== "object" ||
      Array.isArray(body)
    ) {
      throw new Error(
        "object required",
      );
    }
    return body;
  } catch {
    throw badRequest(
      "INVALID_JSON",
      "request body must be a JSON object",
    );
  }
}

function badRequest(code, message) {
  const error =
    new Error(message);
  error.status = 400;
  error.code = code;
  return error;
}

function json(status, body) {
  return new Response(
    JSON.stringify(body),
    {
      status,
      headers: {
        "content-type":
          "application/json; charset=utf-8",
        "cache-control":
          "no-store",
        "x-content-type-options":
          "nosniff",
      },
    },
  );
}
