import {
  bearerToken,
  deriveFriendIdentity,
  httpError,
  normalizeDisplayName,
  requireClientRequestId,
  sha256Hex,
} from "./crypto.js";

const FRIEND_ID =
  /^LPF-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$/;
const MATCH_ID =
  /^LP[A-Z2-9]{8}$/;
const FRIEND_TOKEN =
  /^lf_[A-Za-z0-9_-]{32,}$/;
const PRESENCE_TTL_MS =
  75 * 1000;
const INVITE_TTL_MS =
  24 * 60 * 60 * 1000;

export class FriendDirectory {
  constructor(ctx, env) {
    this.ctx =
      ctx;
    this.env =
      env;

    ctx.blockConcurrencyWhile(
      async () => {
        const sql =
          ctx.storage.sql;

        sql.exec(`
          CREATE TABLE IF NOT EXISTS friend_profiles (
            friend_id TEXT PRIMARY KEY,
            token_hash TEXT NOT NULL UNIQUE,
            registration_request_id TEXT NOT NULL UNIQUE,
            display_name TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            last_seen_at INTEGER NOT NULL
          )
        `);

        sql.exec(`
          CREATE TABLE IF NOT EXISTS friend_requests (
            request_id TEXT PRIMARY KEY,
            sender_id TEXT NOT NULL,
            receiver_id TEXT NOT NULL,
            status TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
          )
        `);

        sql.exec(`
          CREATE INDEX IF NOT EXISTS idx_friend_requests_receiver
          ON friend_requests (
            receiver_id,
            status,
            updated_at DESC
          )
        `);

        sql.exec(`
          CREATE INDEX IF NOT EXISTS idx_friend_requests_sender
          ON friend_requests (
            sender_id,
            status,
            updated_at DESC
          )
        `);

        sql.exec(`
          CREATE TABLE IF NOT EXISTS friendships (
            friend_a TEXT NOT NULL,
            friend_b TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            PRIMARY KEY (
              friend_a,
              friend_b
            )
          )
        `);

        sql.exec(`
          CREATE INDEX IF NOT EXISTS idx_friendships_b
          ON friendships (
            friend_b,
            friend_a
          )
        `);

        sql.exec(`
          CREATE TABLE IF NOT EXISTS friend_invites (
            invite_id TEXT PRIMARY KEY,
            sender_id TEXT NOT NULL,
            receiver_id TEXT NOT NULL,
            match_id TEXT NOT NULL,
            status TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            expires_at INTEGER NOT NULL
          )
        `);

        sql.exec(`
          CREATE INDEX IF NOT EXISTS idx_friend_invites_receiver
          ON friend_invites (
            receiver_id,
            status,
            created_at DESC
          )
        `);
      },
    );
  }

  async fetch(request) {
    try {
      const url =
        new URL(
          request.url,
        );

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/register"
      ) {
        return await this.#register(
          request,
        );
      }

      if (
        request.method ===
          "GET" &&
        url.pathname ===
          "/snapshot"
      ) {
        return await this.#snapshot(
          request,
        );
      }

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/heartbeat"
      ) {
        return await this.#heartbeat(
          request,
        );
      }

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/request"
      ) {
        return await this.#sendRequest(
          request,
        );
      }

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/request/respond"
      ) {
        return await this.#respondRequest(
          request,
        );
      }

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/remove"
      ) {
        return await this.#removeFriend(
          request,
        );
      }

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/invite"
      ) {
        return await this.#invite(
          request,
        );
      }

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/invite/respond"
      ) {
        return await this.#respondInvite(
          request,
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

  async #register(
    request,
  ) {
    const body =
      await readJson(
        request,
      );
    const clientRequestId =
      requireClientRequestId(
        body
          ?.clientRequestId,
      );
    const displayName =
      normalizeDisplayName(
        body
          ?.displayName,
      );
    const identity =
      await deriveFriendIdentity(
        this.env,
        clientRequestId,
      );
    const tokenHash =
      await friendTokenHash(
        identity
          .friendToken,
      );
    const now =
      Date.now();

    const existingRows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT
              friend_id,
              registration_request_id
            FROM friend_profiles
            WHERE
              friend_id = ?
              OR registration_request_id = ?
              OR token_hash = ?
            LIMIT 1
          `,
          identity
            .friendId,
          clientRequestId,
          tokenHash,
        ),
    ];

    if (
      existingRows.length >
      0
    ) {
      const existing =
        existingRows[0];
      if (
        String(
          existing
            .friend_id,
        ) !==
          identity
            .friendId ||
        String(
          existing
            .registration_request_id,
        ) !==
          clientRequestId
      ) {
        throw httpError(
          409,
          "FRIEND_ID_CONFLICT",
          "friend identity could not be recovered",
        );
      }

      this.ctx.storage.sql
        .exec(
          `
            UPDATE friend_profiles
            SET
              token_hash = ?,
              display_name = ?,
              last_seen_at = ?
            WHERE friend_id = ?
          `,
          tokenHash,
          displayName,
          now,
          identity
            .friendId,
        );
    } else {
      this.ctx.storage.sql
        .exec(
          `
            INSERT INTO friend_profiles (
              friend_id,
              token_hash,
              registration_request_id,
              display_name,
              created_at,
              last_seen_at
            ) VALUES (?, ?, ?, ?, ?, ?)
          `,
          identity
            .friendId,
          tokenHash,
          clientRequestId,
          displayName,
          now,
          now,
        );
    }

    return json(
      200,
      {
        ok: true,
        friendId:
          identity
            .friendId,
        friendToken:
          identity
            .friendToken,
        displayName,
        online:
          true,
      },
    );
  }

  async #snapshot(
    request,
  ) {
    const me =
      await this.#authorize(
        request,
      );
    const now =
      Date.now();
    this.#expireInvites(
      now,
    );
    this.#touch(
      me.friendId,
      now,
    );

    const friends =
      this.#friendRows(
        me.friendId,
        now,
      );
    const incomingRequests =
      this.#requestRows(
        me.friendId,
        true,
      );
    const outgoingRequests =
      this.#requestRows(
        me.friendId,
        false,
      );
    const invites =
      this.#inviteRows(
        me.friendId,
        now,
      );

    return json(
      200,
      {
        friendId:
          me.friendId,
        displayName:
          me.displayName,
        online:
          true,
        presenceTtlMs:
          PRESENCE_TTL_MS,
        friends,
        incomingRequests,
        outgoingRequests,
        invites,
      },
    );
  }

  async #heartbeat(
    request,
  ) {
    const me =
      await this.#authorize(
        request,
      );
    const body =
      await readJson(
        request,
      );
    const now =
      Date.now();

    let displayName =
      me.displayName;
    if (
      body
        ?.displayName !=
      null
    ) {
      displayName =
        normalizeDisplayName(
          body
            .displayName,
        );
    }

    this.ctx.storage.sql
      .exec(
        `
          UPDATE friend_profiles
          SET
            display_name = ?,
            last_seen_at = ?
          WHERE friend_id = ?
        `,
        displayName,
        now,
        me.friendId,
      );

    return json(
      200,
      {
        ok: true,
        friendId:
          me.friendId,
        displayName,
        onlineUntil:
          now +
          PRESENCE_TTL_MS,
      },
    );
  }

  async #sendRequest(
    request,
  ) {
    const me =
      await this.#authorize(
        request,
      );
    const body =
      await readJson(
        request,
      );
    const targetFriendId =
      normalizeFriendId(
        body
          ?.friendId,
      );

    if (
      targetFriendId ===
      me.friendId
    ) {
      throw httpError(
        409,
        "CANNOT_ADD_SELF",
        "you cannot send a friend request to yourself",
      );
    }

    const target =
      this.#profile(
        targetFriendId,
      );
    if (!target) {
      throw httpError(
        404,
        "FRIEND_NOT_FOUND",
        "friend ID was not found",
      );
    }

    if (
      this.#areFriends(
        me.friendId,
        targetFriendId,
      )
    ) {
      return json(
        200,
        {
          ok: true,
          status:
            "ALREADY_FRIENDS",
          friend:
            publicProfile(
              target,
              Date.now(),
            ),
        },
      );
    }

    const reverseRows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT request_id
            FROM friend_requests
            WHERE
              sender_id = ?
              AND receiver_id = ?
              AND status = 'PENDING'
            LIMIT 1
          `,
          targetFriendId,
          me.friendId,
        ),
    ];

    if (
      reverseRows.length >
      0
    ) {
      return json(
        200,
        {
          ok: true,
          status:
            "INCOMING_PENDING",
          requestId:
            String(
              reverseRows[0]
                .request_id,
            ),
        },
      );
    }

    const requestId =
      await friendshipRequestId(
        me.friendId,
        targetFriendId,
      );
    const now =
      Date.now();

    this.ctx.storage.sql
      .exec(
        `
          INSERT INTO friend_requests (
            request_id,
            sender_id,
            receiver_id,
            status,
            created_at,
            updated_at
          ) VALUES (?, ?, ?, 'PENDING', ?, ?)
          ON CONFLICT(request_id) DO UPDATE SET
            status = 'PENDING',
            updated_at = excluded.updated_at
        `,
        requestId,
        me.friendId,
        targetFriendId,
        now,
        now,
      );

    this.#touch(
      me.friendId,
      now,
    );

    return json(
      200,
      {
        ok: true,
        status:
          "PENDING",
        requestId,
        friend:
          publicProfile(
            target,
            now,
          ),
      },
    );
  }

  async #respondRequest(
    request,
  ) {
    const me =
      await this.#authorize(
        request,
      );
    const body =
      await readJson(
        request,
      );
    const requestId =
      normalizeActionId(
        body
          ?.requestId,
        "FRQ",
      );
    const accept =
      body
        ?.accept ===
      true;

    const rows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT
              request_id,
              sender_id,
              receiver_id,
              status
            FROM friend_requests
            WHERE request_id = ?
            LIMIT 1
          `,
          requestId,
        ),
    ];

    if (
      rows.length !==
      1
    ) {
      throw httpError(
        404,
        "FRIEND_REQUEST_NOT_FOUND",
        "friend request was not found",
      );
    }

    const row =
      rows[0];
    if (
      String(
        row.receiver_id,
      ) !==
      me.friendId
    ) {
      throw httpError(
        403,
        "FRIEND_REQUEST_FORBIDDEN",
        "only the receiver can respond to this friend request",
      );
    }

    const senderId =
      String(
        row.sender_id,
      );
    const now =
      Date.now();

    if (
      String(
        row.status,
      ) ===
      "PENDING"
    ) {
      this.ctx.storage
        .transactionSync(
          () => {
            this.ctx.storage.sql
              .exec(
                `
                  UPDATE friend_requests
                  SET
                    status = ?,
                    updated_at = ?
                  WHERE request_id = ?
                `,
                accept
                  ? "ACCEPTED"
                  : "DECLINED",
                now,
                requestId,
              );

            if (accept) {
              const [
                friendA,
                friendB,
              ] =
                canonicalPair(
                  me.friendId,
                  senderId,
                );
              this.ctx.storage.sql
                .exec(
                  `
                    INSERT OR IGNORE INTO friendships (
                      friend_a,
                      friend_b,
                      created_at
                    ) VALUES (?, ?, ?)
                  `,
                  friendA,
                  friendB,
                  now,
                );
            }
          },
        );
    }

    const sender =
      this.#profile(
        senderId,
      );

    return json(
      200,
      {
        ok: true,
        status:
          accept
            ? "ACCEPTED"
            : "DECLINED",
        friend:
          sender
            ? publicProfile(
                sender,
                now,
              )
            : null,
      },
    );
  }

  async #removeFriend(
    request,
  ) {
    const me =
      await this.#authorize(
        request,
      );
    const body =
      await readJson(
        request,
      );
    const targetFriendId =
      normalizeFriendId(
        body
          ?.friendId,
      );
    const [
      friendA,
      friendB,
    ] =
      canonicalPair(
        me.friendId,
        targetFriendId,
      );

    const now =
      Date.now();
    let removed =
      false;

    this.ctx.storage
      .transactionSync(
        () => {
          const result =
            this.ctx.storage.sql
              .exec(
                `
                  DELETE FROM friendships
                  WHERE
                    friend_a = ?
                    AND friend_b = ?
                `,
                friendA,
                friendB,
              );
          removed =
            Number(
              result.rowsWritten ??
                0,
            ) >
            0;

          this.ctx.storage.sql
            .exec(
              `
                UPDATE friend_invites
                SET
                  status = 'REVOKED',
                  updated_at = ?
                WHERE
                  status = 'PENDING'
                  AND (
                    (
                      sender_id = ?
                      AND receiver_id = ?
                    )
                    OR (
                      sender_id = ?
                      AND receiver_id = ?
                    )
                  )
              `,
              now,
              me.friendId,
              targetFriendId,
              targetFriendId,
              me.friendId,
            );
        },
      );

    return json(
      200,
      {
        ok: true,
        removed,
      },
    );
  }

  async #invite(
    request,
  ) {
    const me =
      await this.#authorize(
        request,
      );
    const body =
      await readJson(
        request,
      );
    const targetFriendId =
      normalizeFriendId(
        body
          ?.friendId,
      );
    const matchId =
      normalizeMatchId(
        body
          ?.matchId,
      );
    const clientRequestId =
      requireClientRequestId(
        body
          ?.clientRequestId,
      );

    await this.#assertFriendRoomHost(
      matchId,
      request.headers.get(
        "x-ludoproof-room-token",
      ),
    );

    if (
      !this.#areFriends(
        me.friendId,
        targetFriendId,
      )
    ) {
      throw httpError(
        403,
        "INVITE_REQUIRES_FRIEND",
        "private room invites can only be sent to friends",
      );
    }

    const now =
      Date.now();
    this.#expireInvites(
      now,
    );

    const pendingRows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT
              invite_id,
              expires_at
            FROM friend_invites
            WHERE
              sender_id = ?
              AND receiver_id = ?
              AND match_id = ?
              AND status = 'PENDING'
              AND expires_at > ?
            ORDER BY
              created_at DESC
            LIMIT 1
          `,
          me.friendId,
          targetFriendId,
          matchId,
          now,
        ),
    ];

    if (
      pendingRows.length >
      0
    ) {
      return json(
        200,
        {
          ok: true,
          inviteId:
            String(
              pendingRows[0]
                .invite_id,
            ),
          status:
            "PENDING",
          matchId,
          expiresAt:
            Number(
              pendingRows[0]
                .expires_at,
            ),
          replayed:
            true,
        },
      );
    }

    const inviteId =
      await friendInviteId(
        clientRequestId,
      );
    const expiresAt =
      now +
      INVITE_TTL_MS;

    const existingRows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT
              sender_id,
              receiver_id,
              match_id
            FROM friend_invites
            WHERE invite_id = ?
            LIMIT 1
          `,
          inviteId,
        ),
    ];

    if (
      existingRows.length >
      0
    ) {
      const existing =
        existingRows[0];
      if (
        String(
          existing.sender_id,
        ) !==
          me.friendId ||
        String(
          existing.receiver_id,
        ) !==
          targetFriendId ||
        String(
          existing.match_id,
        ) !==
          matchId
      ) {
        throw httpError(
          409,
          "INVITE_IDEMPOTENCY_CONFLICT",
          "invite request ID was already used for different data",
        );
      }
    } else {
      this.ctx.storage.sql
        .exec(
          `
            INSERT INTO friend_invites (
              invite_id,
              sender_id,
              receiver_id,
              match_id,
              status,
              created_at,
              updated_at,
              expires_at
            ) VALUES (?, ?, ?, ?, 'PENDING', ?, ?, ?)
          `,
          inviteId,
          me.friendId,
          targetFriendId,
          matchId,
          now,
          now,
          expiresAt,
        );
    }

    this.#touch(
      me.friendId,
      now,
    );

    return json(
      200,
      {
        ok: true,
        inviteId,
        status:
          "PENDING",
        matchId,
        expiresAt,
      },
    );
  }

  async #respondInvite(
    request,
  ) {
    const me =
      await this.#authorize(
        request,
      );
    const body =
      await readJson(
        request,
      );
    const inviteId =
      normalizeActionId(
        body
          ?.inviteId,
        "FIV",
      );
    const accept =
      body
        ?.accept ===
      true;
    const now =
      Date.now();

    this.#expireInvites(
      now,
    );

    const rows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT
              invite_id,
              sender_id,
              receiver_id,
              match_id,
              status,
              expires_at
            FROM friend_invites
            WHERE invite_id = ?
            LIMIT 1
          `,
          inviteId,
        ),
    ];

    if (
      rows.length !==
      1
    ) {
      throw httpError(
        404,
        "INVITE_NOT_FOUND",
        "friend invite was not found",
      );
    }

    const row =
      rows[0];
    if (
      String(
        row.receiver_id,
      ) !==
      me.friendId
    ) {
      throw httpError(
        403,
        "INVITE_FORBIDDEN",
        "only the invited friend can respond",
      );
    }

    const status =
      String(
        row.status,
      );

    if (
      status ===
        "EXPIRED" ||
      status ===
        "REVOKED"
    ) {
      throw httpError(
        410,
        status ===
          "REVOKED"
          ? "INVITE_REVOKED"
          : "INVITE_EXPIRED",
        status ===
          "REVOKED"
          ? "friend invite was revoked"
          : "friend invite expired",
      );
    }

    if (
      status !==
        "PENDING"
    ) {
      const expectedStatus =
        accept
          ? "ACCEPTED"
          : "DECLINED";
      if (
        status !==
        expectedStatus
      ) {
        throw httpError(
          409,
          "INVITE_ALREADY_RESPONDED",
          "friend invite was already answered",
        );
      }

      return json(
        200,
        {
          ok: true,
          status,
          matchId:
            status ===
            "ACCEPTED"
              ? String(
                  row.match_id,
                )
              : null,
          replayed:
            true,
        },
      );
    }

    if (
      status ===
      "PENDING"
    ) {
      this.ctx.storage.sql
        .exec(
          `
            UPDATE friend_invites
            SET
              status = ?,
              updated_at = ?
            WHERE invite_id = ?
          `,
          accept
            ? "ACCEPTED"
            : "DECLINED",
          now,
          inviteId,
        );
    }

    this.#touch(
      me.friendId,
      now,
    );

    return json(
      200,
      {
        ok: true,
        status:
          accept
            ? "ACCEPTED"
            : "DECLINED",
        matchId:
          accept
            ? String(
                row.match_id,
              )
            : null,
      },
    );
  }

  async #assertFriendRoomHost(
    matchId,
    roomToken,
  ) {
    if (
      typeof roomToken !==
        "string" ||
      !/^lp_[A-Za-z0-9_-]{32,}$/
        .test(
          roomToken,
        )
    ) {
      throw httpError(
        401,
        "ROOM_AUTH_REQUIRED",
        "private room host credential is required",
      );
    }

    if (
      !this.env
        .LUDOPROOF_MATCHES
    ) {
      throw httpError(
        503,
        "MATCH_STORE_NOT_CONFIGURED",
        "match storage is not configured",
      );
    }

    const id =
      this.env
        .LUDOPROOF_MATCHES
        .idFromName(
          matchId,
        );
    const target =
      this.env
        .LUDOPROOF_MATCHES
        .get(
          id,
        );
    const response =
      await target.fetch(
        new Request(
          "https://room/state",
          {
            method:
              "GET",
            headers: {
              authorization:
                "Bearer " +
                roomToken,
            },
          },
        ),
      );

    let body;
    try {
      body =
        await response.json();
    } catch {
      throw httpError(
        503,
        "ROOM_AUTH_BAD_RESPONSE",
        "private room authorization returned invalid data",
      );
    }

    if (
      !response.ok
    ) {
      throw httpError(
        response.status ===
          401
          ? 401
          : 403,
        "ROOM_AUTH_INVALID",
        "private room host credential is invalid",
      );
    }

    const state =
      body
        ?.state;
    if (
      state
        ?.matchMode !==
        "FRIENDS"
    ) {
      throw httpError(
        409,
        "NOT_FRIEND_ROOM",
        "room is not a friends private room",
      );
    }
    if (
      state
        ?.status !==
        "WAITING"
    ) {
      throw httpError(
        409,
        "ROOM_NOT_WAITING",
        "room is no longer accepting invites",
      );
    }
    if (
      body
        ?.playerId !==
      state
        ?.hostPlayerId
    ) {
      throw httpError(
        403,
        "HOST_ONLY",
        "only the private room host can invite friends",
      );
    }
  }

  async #authorize(
    request,
  ) {
    const token =
      bearerToken(
        request,
      );
    if (
      !FRIEND_TOKEN.test(
        token,
      )
    ) {
      throw httpError(
        401,
        "FRIEND_AUTH_INVALID",
        "friend credential is invalid",
      );
    }

    const tokenHash =
      await friendTokenHash(
        token,
      );
    const rows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT
              friend_id,
              display_name,
              last_seen_at
            FROM friend_profiles
            WHERE token_hash = ?
            LIMIT 1
          `,
          tokenHash,
        ),
    ];

    if (
      rows.length !==
      1
    ) {
      throw httpError(
        401,
        "FRIEND_AUTH_INVALID",
        "friend credential is invalid",
      );
    }

    return {
      friendId:
        String(
          rows[0]
            .friend_id,
        ),
      displayName:
        String(
          rows[0]
            .display_name,
        ),
      lastSeenAt:
        Number(
          rows[0]
            .last_seen_at,
        ),
    };
  }

  #profile(
    friendId,
  ) {
    const rows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT
              friend_id,
              display_name,
              last_seen_at
            FROM friend_profiles
            WHERE friend_id = ?
            LIMIT 1
          `,
          friendId,
        ),
    ];
    return rows[0] ??
      null;
  }

  #friendRows(
    friendId,
    now,
  ) {
    const rows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT
              p.friend_id,
              p.display_name,
              p.last_seen_at,
              f.created_at
            FROM friendships AS f
            JOIN friend_profiles AS p
              ON p.friend_id = CASE
                WHEN f.friend_a = ? THEN f.friend_b
                ELSE f.friend_a
              END
            WHERE
              f.friend_a = ?
              OR f.friend_b = ?
            ORDER BY
              p.last_seen_at DESC,
              p.display_name COLLATE NOCASE ASC,
              p.friend_id ASC
          `,
          friendId,
          friendId,
          friendId,
        ),
    ];

    return rows.map(
      (row) => ({
        ...publicProfile(
          row,
          now,
        ),
        friendsSince:
          Number(
            row.created_at,
          ),
      }),
    );
  }

  #requestRows(
    friendId,
    incoming,
  ) {
    const ownColumn =
      incoming
        ? "receiver_id"
        : "sender_id";
    const otherColumn =
      incoming
        ? "sender_id"
        : "receiver_id";

    const rows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT
              r.request_id,
              r.created_at,
              r.updated_at,
              p.friend_id,
              p.display_name,
              p.last_seen_at
            FROM friend_requests AS r
            JOIN friend_profiles AS p
              ON p.friend_id = r.${otherColumn}
            WHERE
              r.${ownColumn} = ?
              AND r.status = 'PENDING'
            ORDER BY
              r.updated_at DESC,
              r.request_id ASC
          `,
          friendId,
        ),
    ];
    const now =
      Date.now();

    return rows.map(
      (row) => ({
        requestId:
          String(
            row.request_id,
          ),
        createdAt:
          Number(
            row.created_at,
          ),
        updatedAt:
          Number(
            row.updated_at,
          ),
        friend:
          publicProfile(
            row,
            now,
          ),
      }),
    );
  }

  #inviteRows(
    friendId,
    now,
  ) {
    const rows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT
              i.invite_id,
              i.match_id,
              i.created_at,
              i.expires_at,
              p.friend_id,
              p.display_name,
              p.last_seen_at
            FROM friend_invites AS i
            JOIN friend_profiles AS p
              ON p.friend_id = i.sender_id
            WHERE
              i.receiver_id = ?
              AND i.status = 'PENDING'
              AND i.expires_at > ?
            ORDER BY
              i.created_at DESC,
              i.invite_id ASC
          `,
          friendId,
          now,
        ),
    ];

    return rows.map(
      (row) => ({
        inviteId:
          String(
            row.invite_id,
          ),
        matchId:
          String(
            row.match_id,
          ),
        createdAt:
          Number(
            row.created_at,
          ),
        expiresAt:
          Number(
            row.expires_at,
          ),
        from:
          publicProfile(
            row,
            now,
          ),
      }),
    );
  }

  #areFriends(
    first,
    second,
  ) {
    const [
      friendA,
      friendB,
    ] =
      canonicalPair(
        first,
        second,
      );
    const rows = [
      ...this.ctx.storage.sql
        .exec(
          `
            SELECT 1
            FROM friendships
            WHERE
              friend_a = ?
              AND friend_b = ?
            LIMIT 1
          `,
          friendA,
          friendB,
        ),
    ];
    return rows.length ===
      1;
  }

  #touch(
    friendId,
    now,
  ) {
    this.ctx.storage.sql
      .exec(
        `
          UPDATE friend_profiles
          SET last_seen_at = ?
          WHERE friend_id = ?
        `,
        now,
        friendId,
      );
  }

  #expireInvites(
    now,
  ) {
    this.ctx.storage.sql
      .exec(
        `
          UPDATE friend_invites
          SET
            status = 'EXPIRED',
            updated_at = ?
          WHERE
            status = 'PENDING'
            AND expires_at <= ?
        `,
        now,
        now,
      );
  }
}

function normalizeFriendId(
  value,
) {
  const normalized =
    String(
      value ??
        "",
    )
      .trim()
      .toUpperCase();
  if (
    !FRIEND_ID.test(
      normalized,
    )
  ) {
    throw httpError(
      400,
      "INVALID_FRIEND_ID",
      "Friend ID must look like LPF-ABCD-EFGH-JKLM",
    );
  }
  return normalized;
}

function normalizeMatchId(
  value,
) {
  const normalized =
    String(
      value ??
        "",
    )
      .trim()
      .toUpperCase();
  if (
    !MATCH_ID.test(
      normalized,
    )
  ) {
    throw httpError(
      400,
      "INVALID_MATCH_ID",
      "invalid LudoProof match ID",
    );
  }
  return normalized;
}

function normalizeActionId(
  value,
  prefix,
) {
  const normalized =
    String(
      value ??
        "",
    )
      .trim()
      .toUpperCase();
  const pattern =
    new RegExp(
      "^" +
        prefix +
        "-[A-F0-9]{20}$",
    );
  if (
    !pattern.test(
      normalized,
    )
  ) {
    throw httpError(
      400,
      "INVALID_ACTION_ID",
      "invalid friend action ID",
    );
  }
  return normalized;
}

function canonicalPair(
  first,
  second,
) {
  return first <
    second
    ? [
        first,
        second,
      ]
    : [
        second,
        first,
      ];
}

function publicProfile(
  row,
  now,
) {
  const lastSeenAt =
    Number(
      row.last_seen_at ??
        0,
    );
  return {
    friendId:
      String(
        row.friend_id,
      ),
    displayName:
      String(
        row.display_name,
      ),
    online:
      lastSeenAt +
        PRESENCE_TTL_MS >
      now,
    lastSeenAt,
  };
}

async function friendshipRequestId(
  senderId,
  receiverId,
) {
  const digest =
    await sha256Hex(
      "ludoproof:friend-request:v1:" +
        senderId +
        ":" +
        receiverId,
    );
  return (
    "FRQ-" +
    digest
      .slice(
        0,
        20,
      )
      .toUpperCase()
  );
}

async function friendInviteId(
  clientRequestId,
) {
  const digest =
    await sha256Hex(
      "ludoproof:friend-invite:v1:" +
        clientRequestId,
    );
  return (
    "FIV-" +
    digest
      .slice(
        0,
        20,
      )
      .toUpperCase()
  );
}

async function friendTokenHash(
  token,
) {
  return sha256Hex(
    "ludoproof:friend-token-hash:v1:" +
      token,
  );
}

async function readJson(
  request,
) {
  const type =
    request.headers.get(
      "content-type",
    ) ??
    "";
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
    16 * 1024
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
        "object required",
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
        "x-content-type-options":
          "nosniff",
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
