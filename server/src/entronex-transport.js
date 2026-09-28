export function hasEntroNexServiceBinding(env) {
  return Boolean(
    env?.ENTRONEX_SERVICE &&
      typeof env.ENTRONEX_SERVICE.fetch === "function",
  );
}

export async function entronexFetch(
  env,
  input,
  init,
) {
  if (hasEntroNexServiceBinding(env)) {
    const request =
      input instanceof Request &&
      init == null
        ? input
        : new Request(
            input,
            init,
          );
    return env.ENTRONEX_SERVICE.fetch(
      request,
    );
  }

  return fetch(
    input,
    init,
  );
}
