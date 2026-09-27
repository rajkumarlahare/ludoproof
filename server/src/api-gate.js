export class ApiGate {
  constructor(ctx) {
    this.ctx = ctx;
    this.tail = Promise.resolve();
  }

  fetch(request) {
    const run =
      this.tail.then(
        () => this.#handle(request),
        () => this.#handle(request),
      );
    this.tail =
      run.catch(() => {});
    return run;
  }

  async alarm() {
    if (
      typeof this.ctx.storage.deleteAll ===
      "function"
    ) {
      await this.ctx.storage.deleteAll();
    }
  }

  async #handle(request) {
    if (request.method !== "POST") {
      return Response.json(
        {
          error: "METHOD_NOT_ALLOWED",
        },
        {
          status: 405,
          headers: {
            allow: "POST",
          },
        },
      );
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return Response.json(
        {
          error: "INVALID_JSON",
        },
        {
          status: 400,
        },
      );
    }

    const limit =
      Number(body?.limit);
    const periodMs =
      Number(body?.periodMs);

    if (
      !Number.isInteger(limit) ||
      limit < 1 ||
      limit > 10_000 ||
      !Number.isInteger(periodMs) ||
      periodMs < 1_000 ||
      periodMs > 60 * 60 * 1000
    ) {
      return Response.json(
        {
          error: "INVALID_RATE_POLICY",
        },
        {
          status: 400,
        },
      );
    }

    const now = Date.now();
    const windowId =
      Math.floor(now / periodMs);
    const current =
      await this.ctx.storage.get(
        "bucket",
      );

    const count =
      current?.windowId === windowId
        ? Number(current.count ?? 0) + 1
        : 1;

    await this.ctx.storage.put(
      "bucket",
      {
        windowId,
        count,
      },
    );

    const resetAt =
      (windowId + 1) * periodMs;
    if (
      typeof this.ctx.storage.setAlarm ===
      "function"
    ) {
      await this.ctx.storage.setAlarm(
        resetAt + 60_000,
      );
    }

    const allowed =
      count <= limit;

    return Response.json(
      {
        allowed,
        limit,
        remaining:
          Math.max(
            0,
            limit - count,
          ),
        resetAt,
      },
      {
        status:
          allowed
            ? 200
            : 429,
        headers: {
          "cache-control": "no-store",
          "retry-after":
            allowed
              ? "0"
              : String(
                  Math.max(
                    1,
                    Math.ceil(
                      (resetAt - now) /
                        1000,
                    ),
                  ),
                ),
        },
      },
    );
  }
}
