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
  const request =
    input instanceof Request && init == null
      ? input
      : new Request(input, init);

  if (hasEntroNexServiceBinding(env)) {
    return env.ENTRONEX_SERVICE.fetch(
      request,
    );
  }

  return fetch(request);
}
