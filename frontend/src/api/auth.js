const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");
  const payload = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const message = isJson
      ? payload?.message || payload?.error || "요청 처리에 실패했습니다."
      : payload || "요청 처리에 실패했습니다.";
    throw new Error(message);
  }

  return payload;
}

export async function signup({ id, password, name }) {
  const response = await fetch(`${API_BASE}/api/signup`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ id, password, name })
  });

  return parseResponse(response);
}

export async function login({ id, password }) {
  const response = await fetch(`${API_BASE}/api/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ id, password })
  });

  return parseResponse(response);
}

export async function me() {
  const response = await fetch(`${API_BASE}/api/auth/me`, {
    credentials: "include"
  });

  return parseResponse(response);
}

export async function logout() {
  const response = await fetch(`${API_BASE}/api/logout`, {
    method: "POST",
    credentials: "include"
  });

  if (!response.ok && response.status !== 204) {
    await parseResponse(response);
  }
}

export async function withdrawAccount() {
  const response = await fetch(`${API_BASE}/api/users/me`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ confirmed: true })
  });

  return parseResponse(response);
}
