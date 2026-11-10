const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const getAuth = () => JSON.parse(localStorage.getItem('auth') || 'null');
export const setAuth = (a) => (a ? localStorage.setItem('auth', JSON.stringify(a)) : localStorage.removeItem('auth'));

export async function api(path, { method = 'GET', body, headers = {} } = {}) {
  const auth = getAuth();
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(auth ? { Authorization: `Bearer ${auth.token}` } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401 && auth) { setAuth(null); window.location.href = '/login'; }
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const fieldErrors = data?.errors?.map((e) => e.defaultMessage).join(', ');
    throw new Error(fieldErrors || data?.message || data?.error || `Request failed (${res.status})`);
  }
  return data;
}

export const inr = (n) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(n ?? 0);
export const when = (t) => (t ? new Date(t).toLocaleString() : '');
